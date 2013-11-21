  var loadedMetaTags = new Object();

  Event.observe(window, 'load', function() {
    $(document.body).observe('celimage:imageSelectionChanged', displayMetaSelection);
  });
  
  var loadMeta = function(imageId) {
    if(!loadedMetaTags[imageId]) {
      var galleryId = imageId.replace(/^.*:(.*?):.*$/g, '$1');
      var image = null;
      loadedGalleries.get(galleryId).getImages().each(function(galImg) {
        if(galImg.getId() == imageId) {
          image = galImg;
        }
      });
      new Ajax.Request(getCelHost(), {
          method: 'post',
          parameters: {
            'xpage' : 'celements_ajax',
            'ajax_mode' : 'getMetaTagsForImage',
            'imageDoc' : image.getSrc().replace(/^\/download\/(.*?)\/(.*?)\/.*$/g, '$1.$2')
          },
          onComplete: function(transport) {
            if (transport.responseText.isJSON()) {
              loadedMetaTags[imageId] = transport.responseText.evalJSON();
            } else if ((typeof console != 'undefined')
                && (typeof console.warn != 'undefined')) {
              console.warn('getMetaTagsForImage: noJSON!!! ', transport.responseText);
            }
            displayMetaSelection();
          }
      });
    } else {
      displayMetaSelection();
    }
  };
  
  var displayMetaSelection = function() {
    var selected = $$('.bild.selected');
    selected.each(function(imgDiv) {
      loadMeta(imgDiv.id);
    });
    var tagContainer = $('metaTags');
    if(tagContainer) {
      tagContainer.update();
      var allTagArray = new Array();
      var allTagContent = new Object();
      for(key in loadedMetaTags) {
        var tags = loadedMetaTags[key];
        for(tagKey in tags) {
          if(allTagArray.indexOf(tagKey) < 0) {
            allTagArray.push(tagKey);
            allTagContent[tagKey] = { 
                nr: 1, 
                values: [tags[tagKey]] 
            };
          } else {
            allTagContent[tagKey] = { 
                nr: (allTagContent[tagKey].nr + 1), 
                values: allTagContent[tagKey].values.push(tags[tagKey])] 
            };
          }
        }
      }
      allTagArray.sort();
    	  console.log(allTagArray, allTagContent);
    	  
    	  
    	  
    	  
    	  
//    	  <p id="metaTags"><span class="tag" title="knulf">bli</span><span class="ocurrences">(1)</span></p>
    	  
    	  
    	  
      }
      tagContainer.up().show();
    }
  };