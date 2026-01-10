/**
 * HybridServer - Servidor HTTP Principal
 * 
 * Esta clase es el corazón del servidor. Se encarga de:
 * 1. Gestionar el pool de hilos para atender múltiples clientes
 * 2. Aceptar conexiones HTTP entrantes
 * 3. Publicar el Web Service para comunicación P2P
 * 4. Coordinar el cierre ordenado del servidor
 * 
 * IMPORTANTE para el examen:
 * - Los constructores determinan qué tipo de repositorio usar
 * - El método start() crea el pool de hilos y el Web Service
 * - El método close() cierra todo ordenadamente sin perder peticiones
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.xml.ws.Endpoint;

public class HybridServer implements AutoCloseable {

  // ==================== CONSTANTES POR DEFECTO ====================
  // Estos valores se usan cuando no se proporciona configuración
  // "En el caso de que no se le pase un fichero de
  // configuración..."

  private static final int DEFAULT_PORT = 8888; // Puerto HTTP por defecto
  private static final int DEFAULT_POOL_SIZE = 50; // Número de hilos (numClients)

  // Página de bienvenida - se muestra cuando accedes a http://localhost:8888/
  // "página de bienvenida donde aparecerá el nombre 'Hybrid Server'
  // y los autores"
  private static final String DEFAULT_PAGE = "<!DOCTYPE html>\n"
      + "<html lang=\"es\">\n"
      + "  <head>\n"
      + "    <meta charset=\"UTF-8\"/>\n"
      + "    <title>Hybrid Server</title>\n"
      + "  </head>\n"
      + "  <body>\n"
      + "    <h1>Hybrid Server</h1>\n"
      + "    <p>Bienvenido a Hybrid Server.</p>\n"
      + "    <p>Nicolas Filloy Montesino y Jose Carlos Peña Otero.</p>\n"
      + "  </body>\n"
      + "</html>\n";

  // ==================== CAMPOS DE INSTANCIA ====================

  private final int port; // Puerto donde escucha el servidor
  private final int workerThreads; // Tamaño del pool de hilos
  private final String welcomePage; // Contenido de la página de bienvenida
  private final Supplier<PageRepository> repositoryFactory; // Fábrica de repositorios (patrón Factory)
  private final Configuration configuration; // Configuración XML (puede ser null)

  private Thread serverThread; // Hilo que acepta conexiones (ServerSocket.accept())
  private ExecutorService executor; // Pool de hilos para procesar peticiones
  private volatile boolean stop; // Flag para señalar que debe pararse (volatile para visibilidad entre hilos)
  private Endpoint webServiceEndpoint; // Endpoint del Web Service JAX-WS (segunda entrega)

  // ==================== CONSTRUCTORES ====================

  /**
   * Constructor vacío - usa valores por defecto
   * Requisito: HybridServer() debe conectar a la BD con parámetros por defecto
   * BD: jdbc:mysql://localhost:33068888/hstestdb, user=hsdb, password=hsdbpass
   */
  public HybridServer() {
    this(
        DEFAULT_PORT,
        DEFAULT_POOL_SIZE,
        DEFAULT_PAGE,
        createDefaultRepository(), // Conecta a BD por defecto
        null);
  }

  /**
   * Constructor con mapa de páginas - para TESTS únicamente
   * Usa InMemoryPageRepository (almacenamiento en memoria, sin BD)
   * 
   * @param pages - Mapa uuid -> contenido HTML
   */
  public HybridServer(Map<String, String> pages) {
    this(
        DEFAULT_PORT,
        DEFAULT_POOL_SIZE,
        DEFAULT_PAGE,
        createInMemorySupplier(pages != null ? new LinkedHashMap<>(pages) : new LinkedHashMap<>()),
        null);
  }

  /**
   * Constructor con Properties - Primera entrega (A6)
   * Lee configuración desde archivo .props
   * 
   * Campos esperados: port, numClients, db.url, db.user, db.password
   */
  public HybridServer(Properties properties) {
    this(
        parsePort(properties),
        parseWorkerThreads(properties),
        extractWelcomePage(properties),
        createRepository(properties),
        null);
  }

  /**
   * Constructor con Configuration - Segunda entrega (B4)
   * Lee configuración desde archivo XML validado contra configuration.xsd
   * 
   * Incluye configuración de servidores P2P
   */
  public HybridServer(Configuration configuration) {
    this(
        configuration.getHttpPort(),
        configuration.getNumClients(),
        DEFAULT_PAGE,
        createRepositoryFromConfiguration(configuration), // Incluye lista de servidores P2P
        configuration);
  }

  /**
   * Constructor privado - todos los demás delegan en este
   * Centraliza la inicialización de campos
   */
  private HybridServer(int port, int workerThreads, String welcomePage, Supplier<PageRepository> repositoryFactory,
      Configuration configuration) {
    this.port = port;
    this.workerThreads = Math.max(1, workerThreads); // Mínimo 1 hilo
    this.welcomePage = welcomePage != null ? welcomePage : DEFAULT_PAGE;
    this.repositoryFactory = repositoryFactory;
    this.configuration = configuration;
  }

  // ==================== MÉTODOS PÚBLICOS ====================

  public int getPort() {
    return this.port;
  }

  /**
   * Inicia el servidor
   * 
   * Pasos:
   * 1. Crea pool de hilos (ExecutorService)
   * 2. Publica Web Service si hay configuración P2P
   * 3. Inicia hilo aceptador que escucha conexiones
   */
  public void start() {
    // Evitar iniciar dos veces
    if (this.executor != null) {
      throw new IllegalStateException("Server already started");
    }

    this.stop = false;

    // Crear pool de hilos fijo
    // "El número de clientes al que se dará servicio será un
    // parámetro de configuración"
    this.executor = Executors.newFixedThreadPool(this.workerThreads);

    // Publicar Web Service para P2P (segunda entrega)
    // Solo si hay URL de webservice en la configuración
    if (this.configuration != null &&
        this.configuration.getWebServiceURL() != null &&
        !this.configuration.getWebServiceURL().isBlank()) {

      // Crear implementación del servicio con acceso a BD
      HybridServerServiceImpl serviceImpl = new HybridServerServiceImpl(
          this.configuration.getDbURL(),
          this.configuration.getDbUser(),
          this.configuration.getDbPassword());

      // Publicar en la URL configurada (ej: http://localhost:10888/ws/HybridServer)
      this.webServiceEndpoint = Endpoint.publish(this.configuration.getWebServiceURL(), serviceImpl);
    }

    // Crear y arrancar hilo aceptador
    this.serverThread = new Thread(() -> {
      try (ServerSocket serverSocket = new ServerSocket(this.port)) {
        while (true) {
          // accept() bloquea hasta que llega una conexión
          final Socket socket = serverSocket.accept();

          // Verificar si debemos parar (se usa una conexión "dummy" para despertar)
          if (stop) {
            socket.close();
            break;
          }

          // Enviar la conexión al pool de hilos
          // ClientHandler procesará la petición HTTP
          this.executor.execute(new ClientHandler(socket, this.repositoryFactory, this.welcomePage));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }, "HybridServer-Acceptor");

    this.serverThread.start();
  }

  /**
   * Cierra el servidor ordenadamente
   * 
   * Pasos:
   * 1. Detiene Web Service
   * 2. Marca stop=true
   * 3. Despierta al hilo aceptador con conexión dummy
   * 4. Espera a que termine el hilo aceptador
   * 5. Cierra el pool de hilos (esperando que terminen todas las tareas)
   */
  @Override
  public void close() {
    // 1. Detener Web Service
    if (this.webServiceEndpoint != null) {
      this.webServiceEndpoint.stop();
      this.webServiceEndpoint = null;
    }

    // 2. Marcar para parar
    this.stop = true;

    // 3. Despertar al hilo aceptador (está bloqueado en accept())
    // Creamos una conexión dummy que será rechazada
    try (Socket socket = new Socket("localhost", this.port)) {
      // La conexión se cierra automáticamente, despertando al accept()
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // 4. Esperar a que el hilo aceptador termine
    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    this.serverThread = null;

    // 5. Cerrar pool de hilos
    shutdownExecutor();
  }

  /**
   * Cierra el pool de hilos esperando a que todas las tareas terminen
   * 
   * "evitando, en cualquier caso, descartar clientes"
   * Por eso esperamos indefinidamente en lugar de forzar con shutdownNow()
   */
  private void shutdownExecutor() {
    if (this.executor != null) {
      // Rechazar nuevas tareas
      this.executor.shutdown();
      try {
        // Esperar indefinidamente a que terminen las tareas pendientes
        while (!this.executor.awaitTermination(1, TimeUnit.MINUTES)) {
          // Seguir esperando (no descartar clientes)
        }
      } catch (InterruptedException e) {
        // Si nos interrumpen, ahí sí forzamos el cierre
        this.executor.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        this.executor = null;
      }
    }
  }

  // ==================== MÉTODOS ESTÁTICOS DE AYUDA ====================

  /**
   * Extrae página de bienvenida del Properties (no se usa normalmente)
   */
  private static String extractWelcomePage(Properties properties) {
    if (properties == null) {
      return DEFAULT_PAGE;
    }
    final String value = properties.getProperty("welcome.page");
    return value != null && !value.isBlank() ? value : DEFAULT_PAGE;
  }

  /**
   * Parsea el puerto desde Properties
   * Si falla o es inválido, devuelve DEFAULT_PORT (8888)
   */
  private static int parsePort(Properties properties) {
    if (properties == null)
      return DEFAULT_PORT;

    final String value = properties.getProperty("port");
    if (value == null)
      return DEFAULT_PORT;

    try {
      final int port = Integer.parseInt(value.trim());
      return port > 0 ? port : DEFAULT_PORT;
    } catch (NumberFormatException ex) {
      return DEFAULT_PORT;
    }
  }

  /**
   * Parsea numClients (tamaño pool) desde Properties
   * Si falla o es inválido, devuelve DEFAULT_POOL_SIZE (50)
   */
  private static int parseWorkerThreads(Properties properties) {
    if (properties == null)
      return DEFAULT_POOL_SIZE;

    final String value = properties.getProperty("numClients");
    if (value == null)
      return DEFAULT_POOL_SIZE;

    try {
      final int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : DEFAULT_POOL_SIZE;
    } catch (NumberFormatException ex) {
      return DEFAULT_POOL_SIZE;
    }
  }

  /**
   * Crea repositorio desde Properties (primera entrega)
   * Si hay db.url, usa DatabasePageRepository; sino, usa valores por defecto
   */
  private static Supplier<PageRepository> createRepository(Properties properties) {
    if (properties != null) {
      final String databaseUrl = properties.getProperty("db.url");
      if (databaseUrl != null && !databaseUrl.isBlank()) {
        final String user = properties.getProperty("db.user");
        final String password = properties.getProperty("db.password");
        return () -> new DatabasePageRepository(databaseUrl, user, password);
      }
    }
    return createDefaultRepository();
  }

  /**
   * Crea repositorio desde Configuration XML (segunda entrega)
   * Incluye lista de servidores P2P para comunicación entre servidores
   */
  private static Supplier<PageRepository> createRepositoryFromConfiguration(Configuration configuration) {
    final String databaseUrl = configuration.getDbURL();
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      final String user = configuration.getDbUser();
      final String password = configuration.getDbPassword();
      final List<ServerConfiguration> servers = configuration.getServers(); // Servidores P2P
      return () -> new DatabasePageRepository(databaseUrl, user, password, servers);
    }
    return createDefaultRepository();
  }

  /**
   * Crea repositorio en memoria (para tests)
   * Reutiliza la misma instancia (singleton) para todos los hilos
   */
  private static Supplier<PageRepository> createInMemorySupplier(Map<String, String> pages) {
    final InMemoryPageRepository repository = new InMemoryPageRepository(pages);
    return () -> repository;
  }

  /**
   * Crea repositorio con parámetros de BD por defecto
   * Usado cuando se llama a HybridServer() sin parámetros
   */
  private static Supplier<PageRepository> createDefaultRepository() {
    final String defaultUrl = "jdbc:mysql://localhost:3306/hstestdb";
    final String defaultUser = "hsdb";
    final String defaultPassword = "hsdbpass";
    return () -> new DatabasePageRepository(defaultUrl, defaultUser, defaultPassword);
  }
}
