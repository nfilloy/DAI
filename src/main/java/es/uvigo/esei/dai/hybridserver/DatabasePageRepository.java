package es.uvigo.esei.dai.hybridserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

class DatabasePageRepository implements PageRepository {
  private static final String TABLE_NAME = "HTML";

  private final String url;
  private final String user;
  private final String password;

  DatabasePageRepository(String url, String user, String password) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Database URL must not be null or blank");
    }

    this.url = url;
    this.user = user;
    this.password = password;
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

    if (path.length == 1 && "html".equals(path[0])) {
      final String uuid = request.getResourceParameters().get("uuid");
      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList());
      }

      final String page = findPage(uuid);
      if (page == null) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
      }

      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, page);
    }

    return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
  }

  private HTTPResponse handlePost(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !"html".equals(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String content = request.getResourceParameters().get("html");
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    final String uuid = insertPage(content);
    final String body = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina almacenada</title></head><body>")
      .append("<p>La pagina se ha almacenado correctamente.</p>")
      .append("<p><a href=\"html?uuid=").append(uuid).append("\">")
      .append(uuid).append("</a></p>")
      .append("</body></html>")
      .toString();

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private HTTPResponse handleDelete(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !"html".equals(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String uuid = request.getResourceParameters().get("uuid");
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    if (!removePage(uuid)) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head><body><p>La pagina se ha eliminado.</p></body></html>";
    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private String findPage(String uuid) {
    final String sql = "SELECT content FROM " + TABLE_NAME + " WHERE uuid = ?";

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

  private String renderPageList() {
    final StringBuilder sb = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>")
      .append("<h1>Paginas disponibles</h1><ul>");

    for (String uuid : listUuids()) {
      sb.append("<li><a href=\"html?uuid=").append(uuid).append("\">").append(uuid).append("</a></li>");
    }

    return sb.append("</ul></body></html>").toString();
  }

  private List<String> listUuids() {
    final String sql = "SELECT uuid FROM " + TABLE_NAME;
    final List<String> uuids = new ArrayList<>();

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        uuids.add(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list pages", e);
    }

    return uuids;
  }

  private String insertPage(String content) {
    final String sql = "INSERT INTO " + TABLE_NAME + " (uuid, content) VALUES (?, ?)";
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

  private boolean removePage(String uuid) {
    final String sql = "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete page", e);
    }
  }

  private Connection openConnection() throws SQLException {
    if (this.user != null && !this.user.isBlank()) {
      return DriverManager.getConnection(this.url, this.user, this.password);
    }

    return DriverManager.getConnection(this.url);
  }

  private static boolean isDuplicateKey(SQLException ex) {
    final String state = ex.getSQLState();
    return state != null && state.startsWith("23");
  }

  private HTTPResponse createHtmlResponse(String version, HTTPResponseStatus status, String content) {
    final HTTPResponse response = new HTTPResponse();
    response.setVersion(version != null ? version : HTTPHeaders.HTTP_1_1.getHeader());
    response.setStatus(status);
    response.setContent(content);
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");
    if (content != null) {
      response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
    }
    return response;
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
