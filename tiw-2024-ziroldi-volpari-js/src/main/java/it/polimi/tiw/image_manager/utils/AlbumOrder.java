package it.polimi.tiw.image_manager.utils;

import java.util.List;

public class AlbumOrder {
	private String albumId;
    private List<String> imageIds;

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds;
    }
    
    public String getAlbumId() {
        return albumId;
    }
    
    public List<String> getImageIds() {
        return imageIds;
    }
}
