package ca.udem.bibudem.calypso;

// Import statements if needed

public class Image {
    private String itemId;
    private String uuid;
    private String itemHandle;
    private String itemName;
    private String collectionId;
    private String urlProperty; // Add other properties as needed
    private String url; // Add other properties as needed

    // Constructors

    public Image(String itemId, String uuid, String itemHandle, String itemName, String collectionId, String url) {
        this.itemId = itemId;
        this.uuid = uuid;
        this.itemHandle = itemHandle;
        this.itemName = itemName;
        this.collectionId = collectionId;
        this.url = url;
    }

    // Getters and setters for each property

    // (Getters and setters for other properties...)


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        // Customize the string representation as needed
        return "Image{" +
                "itemId='" + itemId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", itemHandle='" + itemHandle + '\'' +
                ", itemName='" + itemName + '\'' +
                ", collectionId='" + collectionId + '\'' +
                ", urlProperty='" + urlProperty + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
