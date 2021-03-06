package com.celements.photo.container;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.photo.exception.IllegalImageUrlException;
import com.xpn.xwiki.web.XWikiURLFactory;

public class ImageUrlTest extends AbstractComponentTest {

  private XWikiURLFactory urlFactory;

  // default set of test values
  private String DEF_DB;
  private final String DEF_DOMAIN = "http://www.celements.ch";
  private final String DEF_ACTION = "download";
  private final String DEF_SPACE = "Test";
  private final String DEF_DOCNAME = "Images";
  private final String DEF_FILENAME = "imag1.png";
  private final String DEF_QUERY = "?";
  private final String DEF_INT_URL = "/" + DEF_ACTION + "/" + DEF_SPACE + "/" + DEF_DOCNAME + "/"
      + DEF_FILENAME + DEF_QUERY;
  private final String DEF_EXT_URL = DEF_DOMAIN + DEF_INT_URL;

  @Before
  public void prepareTest() throws Exception {
    DEF_DB = getContext().getDatabase();
    urlFactory = registerComponentMock(XWikiURLFactory.class);
    getContext().setURLFactory(urlFactory);
  }

  @Test
  public void testImageUrl_externalUrl() {
    String url = "http://www.celements.ch/test/out";
    try {
      new ImageUrl.Builder(url).build();
      fail("Expected IllegalImageUrlException (no external urls)");
    } catch (IllegalImageUrlException e) {
    }
  }

  @Test
  public void testGetUrl() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    URL serverUrl = new URL(DEF_EXT_URL);
    expect(urlFactory.createAttachmentURL(eq(DEF_FILENAME), eq(DEF_SPACE), eq(DEF_DOCNAME), eq(
        DEF_ACTION), eq(""), eq(DEF_DB), eq(getContext()))).andReturn(serverUrl);
    expect(urlFactory.getURL(eq(serverUrl), same(getContext()))).andReturn(DEF_INT_URL);
    replayDefault();
    assertEquals(DEF_INT_URL, imgUrl.getUrl());
    verifyDefault();
  }

  @Test
  public void testGetExternalUrl_urlInit() throws Exception {
    URL serverUrl = new URL(DEF_EXT_URL);
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    expect(urlFactory.createAttachmentURL(eq(DEF_FILENAME), eq(DEF_SPACE), eq(DEF_DOCNAME), eq(
        DEF_ACTION), eq(""), eq(DEF_DB), eq(getContext()))).andReturn(serverUrl);
    replayDefault();
    assertEquals(DEF_EXT_URL, imgUrl.getExternalUrl());
    verifyDefault();
  }

  @Test
  public void testGetExternalUrl_urlInitEncoded() throws Exception {
    final String filename = "2010%2D09%2D24%2DPZ%2DPCT%2D01.jpg";
    final String filenameDecoded = "2010-09-24-PZ-PCT-01.jpg";
    final String internalUrl = "/" + DEF_ACTION + "/" + DEF_SPACE + "/" + DEF_DOCNAME + "/"
        + filename + DEF_QUERY;
    final String externalUrl = DEF_DOMAIN + "/" + DEF_ACTION + "/" + DEF_SPACE + "/" + DEF_DOCNAME
        + "/" + filenameDecoded + DEF_QUERY;
    URL serverUrl = new URL(externalUrl);
    ImageUrl imgUrl = new ImageUrl.Builder(internalUrl).build();
    expect(urlFactory.createAttachmentURL(eq(filenameDecoded), eq(DEF_SPACE), eq(DEF_DOCNAME), eq(
        DEF_ACTION), eq(""), eq(DEF_DB), eq(getContext()))).andReturn(serverUrl);
    replayDefault();
    assertEquals(externalUrl, imgUrl.getExternalUrl());
    verifyDefault();
  }

  @Test
  public void testGetExternalUrl_noUrlInit() throws Exception {
    String queryStr = "celwidth=100&test=true";
    URL serverUrl = new URL(DEF_EXT_URL + queryStr);
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_SPACE, DEF_DOCNAME, DEF_FILENAME).action(
        DEF_ACTION).query(queryStr).build();
    expect(urlFactory.createAttachmentURL(eq(DEF_FILENAME), eq(DEF_SPACE), eq(DEF_DOCNAME), eq(
        DEF_ACTION), eq(queryStr), eq(DEF_DB), eq(getContext()))).andReturn(serverUrl);
    replayDefault();
    assertEquals(DEF_EXT_URL + queryStr, imgUrl.getExternalUrl());
    verifyDefault();
  }

  @Test
  public void testParseUrl_overrideFields() throws Exception {
    String action = "file";
    String space = "OtherSpace";
    String docname = "OtherDoc";
    String filename = "otherfile.png";
    String query = "otherquerystring=1";
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).action(action).space(space).docname(
        docname).filename(filename).query(query).build();
    imgUrl.parseUrl(); // just to be sure - gets also called in the getters
    assertEquals(action, imgUrl.getAction().get());
    assertEquals(space, imgUrl.getSpace());
    assertEquals(docname, imgUrl.getDocname());
    assertEquals(filename, imgUrl.getFilename());
    assertEquals(query, imgUrl.getQuery().get());
  }

  @Test
  public void testGetAction() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertEquals(DEF_ACTION, imgUrl.getAction().get());
    verifyDefault();
  }

  @Test
  public void testGetSpace() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertEquals(DEF_SPACE, imgUrl.getSpace());
    verifyDefault();
  }

  @Test
  public void testGetName() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertEquals(DEF_DOCNAME, imgUrl.getDocname());
    verifyDefault();
  }

  @Test
  public void testGetFilename() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertEquals(DEF_FILENAME, imgUrl.getFilename());
    verifyDefault();
  }

  @Test
  public void testGetQuery_empty() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertEquals("", imgUrl.getQuery().get());
    verifyDefault();
  }

  @Test
  public void testGetQuery_withQuery() throws Exception {
    String queryStr = "celwidth=1";
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL + queryStr).build();
    replayDefault();
    assertEquals(queryStr, imgUrl.getQuery().get());
    verifyDefault();
  }

  @Test
  public void testGetWidth_none() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertFalse(imgUrl.getWidth().isPresent());
    verifyDefault();
  }

  @Test
  public void testGetWidth_invalid() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL + "celwidth=hi").build();
    replayDefault();
    assertFalse(imgUrl.getWidth().isPresent());
    verifyDefault();
  }

  @Test
  public void testGetWidth_valid() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL + "celwidth=1001").build();
    replayDefault();
    assertEquals(1001, (int) imgUrl.getWidth().get());
    verifyDefault();
  }

  @Test
  public void testGetHeight_none() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    replayDefault();
    assertFalse(imgUrl.getHeight().isPresent());
    verifyDefault();
  }

  @Test
  public void testGetHeight_invalid() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL + "celheight=9999999999999").build();
    replayDefault();
    assertFalse(imgUrl.getHeight().isPresent());
    verifyDefault();
  }

  @Test
  public void testGetHeight_valid() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL
        + "celwidth=1001&celheight=999&other=x").build();
    replayDefault();
    assertEquals(999, (int) imgUrl.getHeight().get());
    verifyDefault();
  }

  @Test
  public void testToString() throws Exception {
    ImageUrl imgUrl = new ImageUrl.Builder(DEF_INT_URL).build();
    URL serverUrl = new URL(DEF_EXT_URL);
    expect(urlFactory.createAttachmentURL(eq(DEF_FILENAME), eq(DEF_SPACE), eq(DEF_DOCNAME), eq(
        DEF_ACTION), eq(""), eq(DEF_DB), eq(getContext()))).andReturn(serverUrl);
    expect(urlFactory.getURL(eq(serverUrl), same(getContext()))).andReturn(DEF_INT_URL);
    replayDefault();
    assertEquals(DEF_INT_URL, imgUrl.getUrl());
    verifyDefault();
  }

}
