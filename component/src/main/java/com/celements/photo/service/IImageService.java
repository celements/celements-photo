package com.celements.photo.service;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.photo.container.ImageDimensions;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.objects.BaseObject;

@ComponentRole
public interface IImageService {

  public ImageDimensions getDimension(String imageFullName) throws XWikiException;

  public ImageDimensions getDimension(AttachmentReference imgRef) throws XWikiException;

  public List<Attachment> getRandomImages(DocumentReference galleryRef, int num);

  public SpaceReference getPhotoAlbumSpaceRef(DocumentReference galleryDocRef
      ) throws NoGalleryDocumentException;

  public boolean checkAddSlideRights(DocumentReference galleryDocRef);

  public boolean addSlideFromTemplate(DocumentReference galleryDocRef,
      String slideBaseName, String attFullName);

  public BaseObject getPhotoAlbumNavObject(DocumentReference galleryDocRef
      ) throws XWikiException, NoGalleryDocumentException;

  public BaseObject getPhotoAlbumObject(DocumentReference galleryDocRef
      ) throws XWikiException;

  public DocumentReference getImageSlideTemplateRef();

}
