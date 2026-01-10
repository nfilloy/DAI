/**
 * DatabasePageRepository - Repositorio de Páginas en Base de Datos MySQL
 * 
 * Esta es la clase MÁS IMPORTANTE del proyecto. Se encarga de:
 * 1. Manejar peticiones HTTP (GET, POST, DELETE)
 * 2. Operar con la base de datos (CRUD)
 * 3. Gestionar documentos HTML, XML, XSD, XSLT
 * 4. Validar XML contra XSD y transformar con XSLT
 * 5. Comunicarse con servidores P2P para buscar documentos remotos
 * 
 * IMPORTANTE para el examen:
 * - handle() es el punto de entrada - decide qué método usar
 * - handleGet() maneja GET (incluye listados y transformaciones XML)
 * - handlePost() maneja POST (creación de documentos)
 * - handleDelete() maneja DELETE (eliminación de documentos)
 * - Los métodos findPage* buscan primero local, luego remoto (P2P)
 */
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

  // ==================== CONSTANTES ====================

  // Recursos soportados - EN MINÚSCULAS
  // Si alguien pide /HTML (mayúsculas), devolverá 400 porque no está en esta
  // lista
  private static final Set<String> SUPPORTED_RESOURCES = Set.of("html", "xml", "xsd", "xslt");

  // ==================== CAMPOS ====================

  private final String url; // URL de conexión JDBC (ej: jdbc:mysql://localhost:3306/hstestdb)
  private final String user; // Usuario de BD
  private final String password; // Contraseña de BD
  private final List<ServerConfiguration> remoteServers; // Servidores P2P (segunda entrega)

  // ==================== CONSTRUCTORES ====================

  /**
   * Constructor sin P2P (primera entrega)
   */
  DatabasePageRepository(String url, String user, String password) {
    this(url, user, password, null);
  }

  /**
   * Constructor con P2P (segunda entrega)
   * 
   * @param remoteServers - Lista de servidores remotos para buscar documentos
   */
  DatabasePageRepository(String url, String user, String password, List<ServerConfiguration> remoteServers) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Database URL must not be null or blank");
    }

    this.url = url;
    this.user = user;
    this.password = password;
    this.remoteServers = remoteServers != null ? remoteServers : Collections.emptyList();
  }

  // ==================== MÉTODO PRINCIPAL - ROUTER ====================

  /**
   * Punto de entrada - decide qué handler usar según el método HTTP
   * 
   * Requisito: método no soportado → 400 Bad Request
   * 
   * @param request     - Petición HTTP parseada
   * @param welcomePage - HTML de la página de bienvenida
   * @return HTTPResponse con la respuesta
   */
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
        // Requisito: "método no soportado debería devolver error 400"
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Metodo no soportado");
    }
  }

  // ==================== HANDLER GET ====================

  /**
   * Maneja peticiones GET
   * 
   * Casos:
   * 1. / → Página de bienvenida
   * 2. /html, /xml, /xsd, /xslt (sin uuid) → Listado de páginas
   * 3. /html?uuid=xxx → Contenido del documento
   * 4. /xml?uuid=xxx&xslt=yyy → XML transformado con XSLT
   * 5. /HTML (mayúsculas) → 400 Bad Request
   */
  private HTTPResponse handleGet(HTTPRequest request, String welcomePage) {
    final String[] path = request.getResourcePath();

    // CASO 1: Raíz (/) → Página de bienvenida
    // "Cuando un usuario acceda a la dirección raíz se le servirá una
    // página de bienvenida"
    if (path.length == 0) {
      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, welcomePage);
    }

    // Verificar que el recurso es válido (html, xml, xsd, xslt en minúsculas)
    if (path.length == 1 && SUPPORTED_RESOURCES.contains(path[0])) {
      final String resource = path[0];
      final String uuid = request.getResourceParameters().get("uuid");

      // CASO 2: Sin uuid → Mostrar listado de páginas
      // "Si un cliente hace una petición sobre el recurso html sin
      // ningún parámetro
      // se listarán los enlaces a todas las páginas HTML almacenadas"
      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList(resource));
      }

      // CASO 3: Con uuid → Buscar y devolver documento
      final String page = findPage(resource, uuid);
      if (page == null) {
        // "si el cliente solicita una página que no existe → 404"
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
      }

      // CASO 4: XML con parámetro xslt → Validar y transformar
      // "En las solicitudes GET, el cliente podrá incluir el parámetro
      // xslt"
      if ("xml".equals(resource)) {
        final String xsltUuid = request.getResourceParameters().get("xslt");
        if (xsltUuid != null && !xsltUuid.isBlank()) {
          return handleXmlTransformation(request.getHttpVersion(), page, xsltUuid);
        }
      }

      // Devolver documento con el MIME type correcto
      return createResponse(request.getHttpVersion(), HTTPResponseStatus.S200, page, getMimeType(resource));
    }

    // CASO 5: Recurso no válido (ej: /HTML, /xyz) → 400
    // "si la solicitud es incorrecta → 400 Bad Request"
    return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
  }

  /**
   * Maneja la transformación XML con XSLT
   * 
   * Pasos:
   * 1. Buscar XSLT por uuid
   * 2. Obtener XSD asociado al XSLT
   * 3. Buscar contenido del XSD
   * 4. Validar XML contra XSD
   * 5. Transformar XML con XSLT
   * 
   * "validar el XML con el esquema y devolver el resultado de
   * transformar el documento"
   */
  private HTTPResponse handleXmlTransformation(String version, String xmlContent, String xsltUuid) {
    // 1. Buscar XSLT
    final String xsltContent = findPage("xslt", xsltUuid);
    if (xsltContent == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSLT no encontrado");
    }

    // 2. Obtener UUID del XSD asociado al XSLT
    final String xsdUuid = getXsdUuidForXslt(xsltUuid);
    if (xsdUuid == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSD asociado no encontrado");
    }

    // 3. Buscar contenido del XSD
    final String xsdContent = findPage("xsd", xsdUuid);
    if (xsdContent == null) {
      return createErrorResponse(version, HTTPResponseStatus.S404, "XSD no encontrado");
    }

    // 4. Validar XML contra XSD
    // "validar el XML con el esquema, en caso de que no se supere →
    // 400"
    if (!validateXmlAgainstXsd(xmlContent, xsdContent)) {
      return createErrorResponse(version, HTTPResponseStatus.S400, "XML no valido para el XSD");
    }

    // 5. Transformar XML con XSLT
    try {
      final String htmlResult = transformXmlWithXslt(xmlContent, xsltContent);
      return createHtmlResponse(version, HTTPResponseStatus.S200, htmlResult);
    } catch (Exception e) {
      return createErrorResponse(version, HTTPResponseStatus.S500, "Error al transformar XML");
    }
  }

  // ==================== HANDLER POST ====================

  /**
   * Maneja peticiones POST (creación de documentos)
   * 
   * "El sistema debe ser capaz de añadir nuevas páginas Web a su
   * base de datos,
   * asignándoles un nuevo identificador único"
   * 
   * Parámetros esperados:
   * - html: contenido HTML
   * - xml: contenido XML
   * - xsd: contenido XSD
   * - xslt: contenido XSLT + parámetro "xsd" con UUID del XSD asociado
   */
  private HTTPResponse handlePost(HTTPRequest request) {
    final String[] path = request.getResourcePath();

    // Validar recurso
    if (path.length != 1 || !SUPPORTED_RESOURCES.contains(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String resource = path[0];

    // El contenido viene en el parámetro con el mismo nombre que el recurso
    final String content = request.getResourceParameters().get(resource);
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    String uuid;

    // Caso especial: XSLT requiere parámetro xsd
    // "Cuando se sube un fichero XSLT deberá incluirse el parámetro
    // xsd"
    if ("xslt".equals(resource)) {
      final String xsdUuid = request.getResourceParameters().get("xsd");

      // "si no se proporciona el parámetro xsd → 400"
      if (xsdUuid == null || xsdUuid.isBlank()) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Parametro xsd requerido");
      }

      // "si el XSD no existe → 404"
      if (!xsdExists(xsdUuid)) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "XSD no encontrado");
      }

      uuid = insertXslt(content, xsdUuid);
    } else {
      // Caso normal: HTML, XML, XSD
      uuid = insertPage(resource, content);
    }

    // Respuesta con enlace al documento creado
    // "La respuesta debe ser una página HTML con un hiperenlace"
    final String body = new StringBuilder()
        .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina almacenada</title></head><body>")
        .append("<p>La pagina se ha almacenado correctamente.</p>")
        .append("<p><a href=\"").append(resource).append("?uuid=").append(uuid).append("\">")
        .append(uuid).append("</a></p>")
        .append("</body></html>")
        .toString();

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  // ==================== HANDLER DELETE ====================

  /**
   * Maneja peticiones DELETE (eliminación de documentos)
   * 
   * "El sistema debe ser capaz de eliminar una página Web alojada
   * en su base de datos"
   */
  private HTTPResponse handleDelete(HTTPRequest request) {
    final String[] path = request.getResourcePath();

    // Validar recurso
    if (path.length != 1 || !SUPPORTED_RESOURCES.contains(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String resource = path[0];
    final String uuid = request.getResourceParameters().get("uuid");

    // uuid es obligatorio
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    // Intentar eliminar - devuelve false si no existe
    if (!removePage(resource, uuid)) {
      // "si el cliente solicita una página que no existe → 404"
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    // Respuesta de éxito
    // "La respuesta deberá ser una página web con un mensaje"
    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head><body><p>La pagina se ha eliminado.</p></body></html>";
    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  // ==================== MÉTODOS DE BÚSQUEDA (P2P) ====================

  /**
   * Busca un documento: primero local, luego en servidores remotos
   * 
   * "cuando un cliente solicite un documento, si el servidor no lo
   * contiene
   * lo buscará en otros servidores"
   */
  private String findPage(String resource, String uuid) {
    // Primero buscar en local
    String content = findPageLocal(resource, uuid);
    if (content != null) {
      return content;
    }

    // Si no está local y hay servidores P2P, buscar remoto
    if (!remoteServers.isEmpty()) {
      return findPageRemote(resource, uuid);
    }

    return null;
  }

  /**
   * Busca documento en base de datos local
   */
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

  /**
   * Busca documento en servidores remotos vía Web Service
   * Usa RemoteServiceClient para invocar el WS
   */
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

  // ==================== LISTADOS DE PÁGINAS ====================

  /**
   * Genera HTML con listado de páginas agrupadas por servidor
   * 
   * 
   * - Local Server: [lista de UUIDs locales con enlaces relativos]
   * - Server 2: [lista de UUIDs remotos con enlaces ABSOLUTOS]
   * 
   * IMPORTANTE: Los enlaces remotos deben apuntar AL SERVIDOR REMOTO, no al local
   */
  private String renderPageList(String resource) {
    final StringBuilder sb = new StringBuilder()
        .append(
            "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>");

    // === PÁGINAS LOCALES ===
    List<PageEntry> localEntries = listEntriesLocal(resource);
    sb.append("<h2>Local Server</h2><ul>");
    for (PageEntry entry : localEntries) {
      // Enlaces RELATIVOS para páginas locales
      sb.append("<li><a href=\"").append(resource).append("?uuid=").append(entry.getUuid()).append("\">")
          .append(entry.getUuid()).append("</a></li>");
    }
    sb.append("</ul>");

    // === PÁGINAS REMOTAS (P2P) ===
    // Agrupadas por nombre del servidor
    if (!remoteServers.isEmpty()) {
      List<PageEntry> remoteEntries = listEntriesRemote(resource);

      // Agrupar por servidor usando LinkedHashMap para mantener orden
      java.util.Map<String, java.util.List<PageEntry>> byServer = new java.util.LinkedHashMap<>();
      for (PageEntry entry : remoteEntries) {
        String name = entry.getServerName() != null ? entry.getServerName() : "Unknown";
        byServer.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(entry);
      }

      // Generar HTML para cada servidor
      for (java.util.Map.Entry<String, java.util.List<PageEntry>> serverGroup : byServer.entrySet()) {
        sb.append("<h2>").append(serverGroup.getKey()).append("</h2><ul>");
        for (PageEntry entry : serverGroup.getValue()) {
          // Enlaces ABSOLUTOS para páginas remotas
          // "los enlaces de los servidores remotos deben llevar a esos
          // servidores"
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

  /**
   * Lista UUIDs de documentos locales
   */
  private List<PageEntry> listEntriesLocal(String resource) {
    final String tableName = getTableName(resource);
    final String sql = "SELECT uuid FROM " + tableName;
    final List<PageEntry> entries = new ArrayList<>();

    try (Connection connection = openConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        // serverUrl y serverName son null para páginas locales
        entries.add(new PageEntry(rs.getString(1), null, null));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list pages", e);
    }

    return entries;
  }

  /**
   * Lista UUIDs de documentos remotos vía Web Service
   */
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

  // ==================== OPERACIONES CRUD EN BD ====================

  /**
   * Inserta documento en BD (HTML, XML, XSD)
   * Genera UUID con java.util.UUID.randomUUID()
   */
  private String insertPage(String resource, String content) {
    final String tableName = getTableName(resource);
    final String sql = "INSERT INTO " + tableName + " (uuid, content) VALUES (?, ?)";
    final String uuid = UUID.randomUUID().toString(); // Generar UUID único

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      stmt.setString(2, content);
      stmt.executeUpdate();
      return uuid;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create page", e);
    }
  }

  /**
   * Inserta XSLT en BD (caso especial: incluye referencia a XSD)
   */
  private String insertXslt(String content, String xsdUuid) {
    final String sql = "INSERT INTO XSLT (uuid, xsd, content) VALUES (?, ?, ?)";
    final String uuid = UUID.randomUUID().toString();

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      stmt.setString(2, xsdUuid); // UUID del XSD asociado
      stmt.setString(3, content);
      stmt.executeUpdate();
      return uuid;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create XSLT", e);
    }
  }

  /**
   * Verifica si existe un XSD con el UUID dado
   */
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

  /**
   * Obtiene el UUID del XSD asociado a un XSLT
   * Busca primero local, luego remoto
   */
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

  /**
   * Obtiene UUID del XSD asociado a XSLT desde BD local
   */
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

  /**
   * Elimina documento de BD
   * 
   * @return true si se eliminó, false si no existía
   */
  private boolean removePage(String resource, String uuid) {
    final String tableName = getTableName(resource);
    final String sql = "DELETE FROM " + tableName + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      return stmt.executeUpdate() > 0; // > 0 significa que se eliminó algo
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete page", e);
    }
  }

  // ==================== VALIDACIÓN Y TRANSFORMACIÓN XML ====================

  /**
   * Valida XML contra XSD usando javax.xml.validation
   * 
   * @return true si es válido, false si no
   */
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

  /**
   * Transforma XML con XSLT usando javax.xml.transform
   * 
   * @return HTML resultante de la transformación
   */
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

  // ==================== MÉTODOS DE UTILIDAD ====================

  /**
   * Mapea nombre de recurso a nombre de tabla en BD
   */
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

  /**
   * Obtiene MIME type según el recurso
   * - html → text/html
   * - xml, xsd, xslt → application/xml
   */
  private MIME getMimeType(String resource) {
    if ("html".equals(resource)) {
      return MIME.TEXT_HTML;
    }
    return MIME.APPLICATION_XML;
  }

  /**
   * Abre conexión con la base de datos
   * Se crea una nueva conexión por cada operación (según requisito)
   */
  private Connection openConnection() throws SQLException {
    if (this.user != null && !this.user.isBlank()) {
      return DriverManager.getConnection(this.url, this.user, this.password);
    }
    return DriverManager.getConnection(this.url);
  }

  // ==================== CREACIÓN DE RESPUESTAS HTTP ====================

  /**
   * Crea respuesta HTTP genérica
   */
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

  /**
   * Crea respuesta HTTP con HTML
   */
  private HTTPResponse createHtmlResponse(String version, HTTPResponseStatus status, String content) {
    return createResponse(version, status, content, MIME.TEXT_HTML);
  }

  /**
   * Crea respuesta HTTP de error con página HTML
   */
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
