package software.amazon.devopsguru.resourcecollection;

/**
 * Make ResourceCollectionType until the SDK supports this enum
 */
public enum ResourceCollectionType {
    AWS_CLOUD_FORMATION("AWS_CLOUD_FORMATION");

    private String name;

    ResourceCollectionType(String envUrl) {
        this.name = envUrl;
    }

    public String getName() {
        return name;
    }
}
