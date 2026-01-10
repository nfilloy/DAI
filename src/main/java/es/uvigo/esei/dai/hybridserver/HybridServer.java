/**
 *  HybridServer
 *  Copyright (C) 2025 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
  private static final int DEFAULT_PORT = 8888;
  private static final int DEFAULT_POOL_SIZE = 50;
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

  private final int port;
  private final int workerThreads;
  private final String welcomePage;
  private final Supplier<PageRepository> repositoryFactory;
  private final Configuration configuration;

  private Thread serverThread;
  private ExecutorService executor;
  private volatile boolean stop;
  private Endpoint webServiceEndpoint;

  public HybridServer() {
    this(
        DEFAULT_PORT,
        DEFAULT_POOL_SIZE,
        DEFAULT_PAGE,
        createDefaultRepository(),
        null);
  }

  public HybridServer(Map<String, String> pages) {
    this(
        DEFAULT_PORT,
        DEFAULT_POOL_SIZE,
        DEFAULT_PAGE,
        createInMemorySupplier(pages != null ? new LinkedHashMap<>(pages) : new LinkedHashMap<>()),
        null);
  }

  public HybridServer(Properties properties) {
    this(
        parsePort(properties),
        parseWorkerThreads(properties),
        extractWelcomePage(properties),
        createRepository(properties),
        null);
  }

  public HybridServer(Configuration configuration) {
    this(
        configuration.getHttpPort(),
        configuration.getNumClients(),
        DEFAULT_PAGE,
        createRepositoryFromConfiguration(configuration),
        configuration);
  }

  private HybridServer(int port, int workerThreads, String welcomePage, Supplier<PageRepository> repositoryFactory,
      Configuration configuration) {
    this.port = port;
    this.workerThreads = Math.max(1, workerThreads);
    this.welcomePage = welcomePage != null ? welcomePage : DEFAULT_PAGE;
    this.repositoryFactory = repositoryFactory;
    this.configuration = configuration;
  }

  public int getPort() {
    return this.port;
  }

  public void start() {
    if (this.executor != null) {
      throw new IllegalStateException("Server already started");
    }

    this.stop = false;
    this.executor = Executors.newFixedThreadPool(this.workerThreads);

    if (this.configuration != null &&
        this.configuration.getWebServiceURL() != null &&
        !this.configuration.getWebServiceURL().isBlank()) {
      HybridServerServiceImpl serviceImpl = new HybridServerServiceImpl(
          this.configuration.getDbURL(),
          this.configuration.getDbUser(),
          this.configuration.getDbPassword());
      this.webServiceEndpoint = Endpoint.publish(this.configuration.getWebServiceURL(), serviceImpl);
    }

    this.serverThread = new Thread(() -> {
      try (ServerSocket serverSocket = new ServerSocket(this.port)) {
        while (true) {
          final Socket socket = serverSocket.accept();
          if (stop) {
            socket.close();
            break;
          }

          this.executor.execute(new ClientHandler(socket, this.repositoryFactory, this.welcomePage));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }, "HybridServer-Acceptor");

    this.serverThread.start();
  }

  @Override
  public void close() {
    if (this.webServiceEndpoint != null) {
      this.webServiceEndpoint.stop();
      this.webServiceEndpoint = null;
    }

    this.stop = true;

    try (Socket socket = new Socket("localhost", this.port)) {
      // Wake up the acceptor thread
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    this.serverThread = null;
    shutdownExecutor();
  }

  private void shutdownExecutor() {
    if (this.executor != null) {
      this.executor.shutdown();
      try {
        // Esperar indefinidamente a que terminen todas las tareas
        while (!this.executor.awaitTermination(1, TimeUnit.MINUTES)) {
          // Seguir esperando
        }
      } catch (InterruptedException e) {
        this.executor.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        this.executor = null;
      }
    }
  }

  private static String extractWelcomePage(Properties properties) {
    if (properties == null) {
      return DEFAULT_PAGE;
    }

    final String value = properties.getProperty("welcome.page");
    return value != null && !value.isBlank() ? value : DEFAULT_PAGE;
  }

  private static int parsePort(Properties properties) {
    if (properties == null) {
      return DEFAULT_PORT;
    }

    final String value = properties.getProperty("port");
    if (value == null) {
      return DEFAULT_PORT;
    }

    try {
      final int port = Integer.parseInt(value.trim());
      return port > 0 ? port : DEFAULT_PORT;
    } catch (NumberFormatException ex) {
      return DEFAULT_PORT;
    }
  }

  private static int parseWorkerThreads(Properties properties) {
    if (properties == null) {
      return DEFAULT_POOL_SIZE;
    }

    final String value = properties.getProperty("numClients");
    if (value == null) {
      return DEFAULT_POOL_SIZE;
    }

    try {
      final int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : DEFAULT_POOL_SIZE;
    } catch (NumberFormatException ex) {
      return DEFAULT_POOL_SIZE;
    }
  }

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

  private static Supplier<PageRepository> createRepositoryFromConfiguration(Configuration configuration) {
    final String databaseUrl = configuration.getDbURL();
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      final String user = configuration.getDbUser();
      final String password = configuration.getDbPassword();
      final List<ServerConfiguration> servers = configuration.getServers();
      return () -> new DatabasePageRepository(databaseUrl, user, password, servers);
    }

    return createDefaultRepository();
  }

  private static Supplier<PageRepository> createInMemorySupplier(Map<String, String> pages) {
    final InMemoryPageRepository repository = new InMemoryPageRepository(pages);
    return () -> repository;
  }

  private static Supplier<PageRepository> createDefaultRepository() {
    // Default database parameters as specified in the project requirements
    final String defaultUrl = "jdbc:mysql://localhost:3306/hstestdb";
    final String defaultUser = "hsdb";
    final String defaultPassword = "hsdbpass";
    return () -> new DatabasePageRepository(defaultUrl, defaultUser, defaultPassword);
  }
}
