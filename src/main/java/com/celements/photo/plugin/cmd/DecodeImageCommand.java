/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.photo.plugin.cmd;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSource;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.sanselan.formats.jpeg.JpegImageParser;
import org.apache.sanselan.formats.jpeg.segments.UnknownSegment;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

public class DecodeImageCommand {
  public static final int COLOR_TYPE_RGB = 1;
  public static final int COLOR_TYPE_CMYK = 2;
  public static final int COLOR_TYPE_YCCK = 3;

  private int colorType = COLOR_TYPE_RGB;
  private String cmykProfile = "ECI_Offset_2009/ISOcoated_v2_300_eci.icc";
  
  public BufferedImage readImage(XWikiAttachment att, XWikiContext context
      ) throws IOException, ImageReadException, XWikiException {
    colorType = COLOR_TYPE_RGB;
    boolean hasAdobeMarker = false;
    ImageInputStream stream = ImageIO.createImageInputStream(att.getContentInputStream(
        context));
    Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
    while (iter.hasNext()) {
      ImageReader reader = iter.next();
      reader.setInput(stream);

      BufferedImage image;
      ICC_Profile profile = null;
      try {
        image = reader.read(0);
      } catch(IIOException e) {
        colorType = COLOR_TYPE_CMYK;
        hasAdobeMarker = checkAdobeMarker(att.getContentInputStream(context), 
            att.getFilename());
        profile = Sanselan.getICCProfile(att.getContentInputStream(context), 
            att.getFilename());
        WritableRaster raster = (WritableRaster) reader.readRaster(0, null);
        if (colorType == COLOR_TYPE_YCCK) {
          convertYcckToCmyk(raster);
        }
        if(hasAdobeMarker) {
          convertInvertedColors(raster);
        }
        image = convertCmykToRgb(raster, profile);
      }
      return image;
    }
    return null;
  }
  
  boolean checkAdobeMarker(InputStream imgIn, String filename
      ) throws IOException, ImageReadException {
    boolean hasAdobeMarker = true;
    JpegImageParser parser = new JpegImageParser();
    ByteSource byteSource = new ByteSourceInputStream(imgIn, filename);//new ByteSourceFile(file);
    @SuppressWarnings("rawtypes")
    ArrayList segments = parser.readSegments(byteSource, new int[] { 0xffee }, true);
    if (segments != null && segments.size() >= 1) {
      UnknownSegment app14Segment = (UnknownSegment) segments.get(0);
      byte[] data = app14Segment.bytes;
      if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' && data[2] == 'o' 
          && data[3] == 'b' && data[4] == 'e'){
        hasAdobeMarker = true;
        int transform = app14Segment.bytes[11] & 0xff;
        if (transform == 2) {
          colorType = COLOR_TYPE_YCCK;
        }
      }
    }
    return hasAdobeMarker;
  }
  
  void convertYcckToCmyk(WritableRaster raster) {
    int height = raster.getHeight();
    int width = raster.getWidth();
    int stride = width * 4;
    int[] pixelRow = new int[stride];
    for (int h = 0; h < height; h++) {
      raster.getPixels(0, h, width, 1, pixelRow);
      for (int x = 0; x < stride; x += 4) {
        int y = pixelRow[x];
        int cb = pixelRow[x + 1];
        int cr = pixelRow[x + 2];
        int c = (int) (y + 1.402 * cr - 178.956);
        int m = (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984);
        y = (int) (y + 1.772 * cb - 226.316);
        if (c < 0) c = 0; else if (c > 255) c = 255;
        if (m < 0) m = 0; else if (m > 255) m = 255;
        if (y < 0) y = 0; else if (y > 255) y = 255;
        pixelRow[x] = 255 - c;
        pixelRow[x + 1] = 255 - m;
        pixelRow[x + 2] = 255 - y;
      }
      raster.setPixels(0, h, width, 1, pixelRow);
    }
  }

  void convertInvertedColors(WritableRaster raster) {
    int height = raster.getHeight();
    int width = raster.getWidth();
    int stride = width * 4;
    int[] pixelRow = new int[stride];
    for (int h = 0; h < height; h++) {
      raster.getPixels(0, h, width, 1, pixelRow);
      for (int x = 0; x < stride; x++) {
        pixelRow[x] = 255 - pixelRow[x];
      }
      raster.setPixels(0, h, width, 1, pixelRow);
    }
  }

  BufferedImage convertCmykToRgb(Raster cmykRaster, ICC_Profile cmykProfile
      ) throws IOException {
    if (cmykProfile == null) {
      cmykProfile = ICC_Profile.getInstance(getClass(
          ).getClassLoader().getResourceAsStream(getCMYKProfile()));
    }
    if (cmykProfile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
      // Need to clone entire profile, due to a JDK 7 bug
      byte[] profileData = cmykProfile.getData();

      if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
        intToBigEndian(ICC_Profile.icSigDisplayClass, profileData, 
            ICC_Profile.icHdrDeviceClass); // Header is first
        cmykProfile = ICC_Profile.getInstance(profileData);
      }
    }
    ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
    BufferedImage rgbImage = new BufferedImage(cmykRaster.getWidth(), 
        cmykRaster.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster rgbRaster = rgbImage.getRaster();
    ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
    ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
    cmykToRgb.filter(cmykRaster, rgbRaster);
    return rgbImage;
  }

  public String getCMYKProfile() {
    return cmykProfile;
  }
  
  public void setCMYKProfile(String cmykProfile) {
    this.cmykProfile = cmykProfile;
  }

  void intToBigEndian(int value, byte[] array, int index) {
    array[index]   = (byte) (value >> 24);
    array[index+1] = (byte) (value >> 16);
    array[index+2] = (byte) (value >>  8);
    array[index+3] = (byte) (value);
  }
}
