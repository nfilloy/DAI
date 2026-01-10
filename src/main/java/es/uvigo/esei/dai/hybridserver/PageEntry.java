package es.uvigo.esei.dai.hybridserver;

/**
 * Represents a page entry with its UUID and the base URL of the server where it
 * resides.
 */
class PageEntry {
    private final String uuid;
    private final String serverUrl;
    private final String serverName;

    PageEntry(String uuid, String serverUrl, String serverName) {
        this.uuid = uuid;
        this.serverUrl = serverUrl;
        this.serverName = serverName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean isLocal() {
        return serverUrl == null || serverUrl.isBlank();
    }
}
