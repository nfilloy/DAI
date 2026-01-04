package es.uvigo.esei.dai.hybridserver;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

public class RemoteServiceClient {

    private final ServerConfiguration serverConfig;

    public RemoteServiceClient(ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
    }

    public HybridServerService getService() {
        try {
            URL wsdlUrl = new URL(serverConfig.getWsdl());
            QName serviceName = new QName(
                serverConfig.getNamespace(),
                serverConfig.getService()
            );

            Service service = Service.create(wsdlUrl, serviceName);
            return service.getPort(HybridServerService.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static String findHtml(List<ServerConfiguration> servers, String uuid) {
        return findContent(servers, uuid, HybridServerService::getHtml);
    }

    public static String findXml(List<ServerConfiguration> servers, String uuid) {
        return findContent(servers, uuid, HybridServerService::getXml);
    }

    public static String findXsd(List<ServerConfiguration> servers, String uuid) {
        return findContent(servers, uuid, HybridServerService::getXsd);
    }

    public static String findXslt(List<ServerConfiguration> servers, String uuid) {
        return findContent(servers, uuid, HybridServerService::getXslt);
    }

    public static String findXsdUuidForXslt(List<ServerConfiguration> servers, String xsltUuid) {
        return findContent(servers, xsltUuid, HybridServerService::getXsdUuidForXslt);
    }

    public static List<String> collectHtmlUuids(List<ServerConfiguration> servers) {
        return collectUuids(servers, HybridServerService::listHtmlUuids);
    }

    public static List<String> collectXmlUuids(List<ServerConfiguration> servers) {
        return collectUuids(servers, HybridServerService::listXmlUuids);
    }

    public static List<String> collectXsdUuids(List<ServerConfiguration> servers) {
        return collectUuids(servers, HybridServerService::listXsdUuids);
    }

    public static List<String> collectXsltUuids(List<ServerConfiguration> servers) {
        return collectUuids(servers, HybridServerService::listXsltUuids);
    }

    private static String findContent(List<ServerConfiguration> servers, String uuid, ContentFetcher fetcher) {
        for (ServerConfiguration server : servers) {
            try {
                RemoteServiceClient client = new RemoteServiceClient(server);
                HybridServerService service = client.getService();
                if (service != null) {
                    String content = fetcher.fetch(service, uuid);
                    if (content != null) {
                        return content;
                    }
                }
            } catch (Exception e) {
                // Server down or error - continue to next
            }
        }
        return null;
    }

    private static List<String> collectUuids(List<ServerConfiguration> servers, UuidLister lister) {
        List<String> allUuids = new ArrayList<>();
        for (ServerConfiguration server : servers) {
            try {
                RemoteServiceClient client = new RemoteServiceClient(server);
                HybridServerService service = client.getService();
                if (service != null) {
                    List<String> uuids = lister.list(service);
                    if (uuids != null) {
                        allUuids.addAll(uuids);
                    }
                }
            } catch (Exception e) {
                // Server down - continue
            }
        }
        return allUuids;
    }

    @FunctionalInterface
    private interface ContentFetcher {
        String fetch(HybridServerService service, String uuid);
    }

    @FunctionalInterface
    private interface UuidLister {
        List<String> list(HybridServerService service);
    }
}
