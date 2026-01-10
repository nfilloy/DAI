package es.uvigo.esei.dai.hybridserver;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

class DatabasePageRepository implements PageRepository {
  private static final Set<String> SUPPORTED_RESOURCES = Set.of("html", "xml", "xsd", "xslt");

  private final String url;
  private final String user;
  private final String password;
  private final List<ServerConfiguration> remoteServers;

  DatabasePageRepository(String url, String user, String password) {
    this(url, user, password, null);
  }

  DatabasePageRepository(String url, String user, String password, List<ServerConfiguration> remoteServers) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Database URL must not be null or blank");
    }

    this.url = url;
    this.user = user;
    this.password = password;
    this.remoteServers = remoteServers != null ? remoteServers : Collections.emptyList();
  }

  @Override
  public HTTPResponse handle(HTTPRequest request, String welcomePage) {
    final HTTPRequestMethod method = request.getMethod();

    switch (method) {
      case GET:
        return handleGet(request, welcomePage);
      case POST:
        return handlePost(request);
      case DELETE:
        return handleDelete(request);
      default:
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Metodo no soportado");
    }
  }

  private HTTPResponse handleGet(HTTPRequest request, String welcomePage) {
    final String[] path = request.getResourcePath();
    if (path.length == 0) {
      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, welcomePage);
    }

    if (path.length == 1 && SUPPORTED_RESOURCES.contains(path[0])) {
      final String resource = path[0];
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList(resource));
      }

      final String page = findPage(resource, uuid);
      if (page == null) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
      }

      if ("xml".equals(resource)) {
        final String xsltUuid = request.getResourceParameters().get("xslt");
        if (xsltUuid != null && !xsltUuid.isBlank()) {
          return handleXmlTransformation(request.getHttpVersion(), page, xsltUuid);
        }
      }

      return createResponse(request.getHttpVersion(), HTTPResponseStatus.S200, page, getMimeType(resource));
    }

    return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
  }

  private HTTPResponse handleXmlTransformation(String version, String xmlContent, String xsltUuid) {
    final String xsltContent = findPage("xslt", xsltUuid);
    if (xsltContent == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSLT no encontrado");
    }

    final String xsdUuid = getXsdUuidForXslt(xsltUuid);
    if (xsdUuid == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSD asociado no encontrado");
    }

    final String xsdContent = findPage("xsd", xsdUuid);
    if (xsdContent == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSD no encontrado");
    }

    if (!validateXmlAgainstXsd(xmlContent, xsdContent)) {
      return createErrorResponse(version, HTTPResponseStatus.S400, "XML no valido para el XSD");
    }

    try {
      final String htmlResult = transformXmlWithXslt(xmlContent, xsltContent);
      return createHtmlResponse(version, HTTPResponseStatus.S200, htmlResult);
    } catch (Exception e) {
      return createErrorResponse(version, HTTPResponseStatus.S500, "Error al transformar XML");
    }
  }

  private HTTPResponse handlePost(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !SUPPORTED_RESOURCES.contains(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String resource = path[0];
    final String content = request.getResourceParameters().get(resource);
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    String uuid;
    if ("xslt".equals(resource)) {
      final String xsdUuid = request.getResourceParameters().get("xsd");
      if (xsdUuid == null || xsdUuid.isBlank()) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Parametro xsd requerido");
      }

      if (!xsdExists(xsdUuid)) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "XSD no encontrado");
      }

      uuid = insertXslt(content, xsdUuid);
    } else {
      uuid = insertPage(resource, content);
    }

    final String body = new StringBuilder()
        .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina almacenada</title></head><body>")
        .append("<p>La pagina se ha almacenado correctamente.</p>")
        .append("<p><a href=\"").append(resource).append("?uuid=").append(uuid).append("\">")
        .append(uuid).append("</a></p>")
        .append("</body></html>")
        .toString();

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private HTTPResponse handleDelete(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !SUPPORTED_RESOURCES.contains(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String resource = path[0];
    final String uuid = request.getResourceParameters().get("uuid");
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    if (!removePage(resource, uuid)) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head><body><p>La pagina se ha eliminado.</p></body></html>";
    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private String findPage(String resource, String uuid) {
    String content = findPageLocal(resource, uuid);
    if (content != null) {
      return content;
    }

    if (!remoteServers.isEmpty()) {
      return findPageRemote(resource, uuid);
    }

    return null;
  }

  private String findPageLocal(String resource, String uuid) {
    final String tableName = getTableName(resource);
    final String sql = "SELECT content FROM " + tableName + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to retrieve page", e);
    }
  }

  private String findPageRemote(String resource, String uuid) {
    switch (resource) {
      case "html":
        return RemoteServiceClient.findHtml(remoteServers, uuid);
      case "xml":
        return RemoteServiceClient.findXml(remoteServers, uuid);
      case "xsd":
        return RemoteServiceClient.findXsd(remoteServers, uuid);
      case "xslt":
        return RemoteServiceClient.findXslt(remoteServers, uuid);
      default:
        return null;
    }
  }

  private String renderPageList(String resource) {
    final StringBuilder sb = new StringBuilder()
        .append(
            "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>");

    // Local pages
    List<PageEntry> localEntries = listEntriesLocal(resource);
    sb.append("<h2>Local Server</h2><ul>");
    for (PageEntry entry : localEntries) {
      sb.append("<li><a href=\"").append(resource).append("?uuid=").append(entry.getUuid()).append("\">")
          .append(entry.getUuid()).append("</a></li>");
    }
    sb.append("</ul>");

    // Remote pages grouped by server
    if (!remoteServers.isEmpty()) {
      List<PageEntry> remoteEntries = listEntriesRemote(resource);
      java.util.Map<String, java.util.List<PageEntry>> byServer = new java.util.LinkedHashMap<>();
      for (PageEntry entry : remoteEntries) {
        String name = entry.getServerName() != null ? entry.getServerName() : "Unknown";
        byServer.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(entry);
      }

      for (java.util.Map.Entry<String, java.util.List<PageEntry>> serverGroup : byServer.entrySet()) {
        sb.append("<h2>").append(serverGroup.getKey()).append("</h2><ul>");
        for (PageEntry entry : serverGroup.getValue()) {
          String serverUrl = entry.getServerUrl();
          if (!serverUrl.endsWith("/")) {
            serverUrl = serverUrl + "/";
          }
          sb.append("<li><a href=\"").append(serverUrl).append(resource).append("?uuid=").append(entry.getUuid())
              .append("\">")
              .append(entry.getUuid()).append("</a></li>");
        }
        sb.append("</ul>");
      }
    }

    return sb.append("</body></html>").toString();
  }

  private List<PageEntry> listEntriesLocal(String resource) {
    final String tableName = getTableName(resource);
    final String sql = "SELECT uuid FROM " + tableName;
    final List<PageEntry> entries = new ArrayList<>();

    try (Connection connection = openConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        entries.add(new PageEntry(rs.getString(1), null, null));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list pages", e);
    }

    return entries;
  }

  private List<PageEntry> listEntriesRemote(String resource) {
    switch (resource) {
      case "html":
        return RemoteServiceClient.collectHtmlEntries(remoteServers);
      case "xml":
        return RemoteServiceClient.collectXmlEntries(remoteServers);
      case "xsd":
        return RemoteServiceClient.collectXsdEntries(remoteServers);
      case "xslt":
        return RemoteServiceClient.collectXsltEntries(remoteServers);
      default:
        return Collections.emptyList();
    }
  }

  private String insertPage(String resource, String content) {
    final String tableName = getTableName(resource);
    final String sql = "INSERT INTO " + tableName + " (uuid, content) VALUES (?, ?)";
    final String uuid = UUID.randomUUID().toString();

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      stmt.setString(2, content);
      stmt.executeUpdate();

      return uuid;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create page", e);
    }
  }

  private String insertXslt(String content, String xsdUuid) {
    final String sql = "INSERT INTO XSLT (uuid, xsd, content) VALUES (?, ?, ?)";
    final String uuid = UUID.randomUUID().toString();

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      stmt.setString(2, xsdUuid);
      stmt.setString(3, content);
      stmt.executeUpdate();

      return uuid;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create XSLT", e);
    }
  }

  private boolean xsdExists(String uuid) {
    final String sql = "SELECT 1 FROM XSD WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check XSD existence", e);
    }
  }

  private String getXsdUuidForXslt(String xsltUuid) {
    String xsdUuid = getXsdUuidForXsltLocal(xsltUuid);
    if (xsdUuid != null) {
      return xsdUuid;
    }

    if (!remoteServers.isEmpty()) {
      return RemoteServiceClient.findXsdUuidForXslt(remoteServers, xsltUuid);
    }

    return null;
  }

  private String getXsdUuidForXsltLocal(String xsltUuid) {
    final String sql = "SELECT xsd FROM XSLT WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, xsltUuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get XSD for XSLT", e);
    }
  }

  private boolean validateXmlAgainstXsd(String xmlContent, String xsdContent) {
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsdContent)));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(new StringReader(xmlContent)));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String transformXmlWithXslt(String xmlContent, String xsltContent) throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer(
        new StreamSource(new StringReader(xsltContent)));

    StringWriter writer = new StringWriter();
    transformer.transform(
        new StreamSource(new StringReader(xmlContent)),
        new StreamResult(writer));

    return writer.toString();
  }

  private boolean removePage(String resource, String uuid) {
    final String tableName = getTableName(resource);
    final String sql = "DELETE FROM " + tableName + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete page", e);
    }
  }

  private String getTableName(String resource) {
    switch (resource) {
      case "html":
        return "HTML";
      case "xml":
        return "XML";
      case "xsd":
        return "XSD";
      case "xslt":
        return "XSLT";
      default:
        throw new IllegalArgumentException("Unknown resource: " + resource);
    }
  }

  private MIME getMimeType(String resource) {
    if ("html".equals(resource)) {
      return MIME.TEXT_HTML;
    }
    return MIME.APPLICATION_XML;
  }

  private Connection openConnection() throws SQLException {
    if (this.user != null && !this.user.isBlank()) {
      return DriverManager.getConnection(this.url, this.user, this.password);
    }

    return DriverManager.getConnection(this.url);
  }

  private HTTPResponse createResponse(String version, HTTPResponseStatus status, String content, MIME mimeType) {
    final HTTPResponse response = new HTTPResponse();
    response.setVersion(version != null ? version : HTTPHeaders.HTTP_1_1.getHeader());
    response.setStatus(status);
    response.setContent(content);
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");
    if (content != null) {
      response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), mimeType.getMime());
    }
    return response;
  }

  private HTTPResponse createHtmlResponse(String version, HTTPResponseStatus status, String content) {
    return createResponse(version, status, content, MIME.TEXT_HTML);
  }

  private HTTPResponse createErrorResponse(String version, HTTPResponseStatus status, String message) {
    final String body = new StringBuilder()
        .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>")
        .append(status.getStatus()).append("</title></head><body><h1>")
        .append(status.getCode()).append(' ').append(status.getStatus()).append("</h1><p>")
        .append(message).append("</p></body></html>")
        .toString();

    return createHtmlResponse(version, status, body);
  }
}
