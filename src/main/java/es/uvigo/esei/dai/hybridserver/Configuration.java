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

import java.util.ArrayList;
import java.util.List;

public class Configuration {
  private int httpPort;
  private int numClients;
  private String webServiceURL;

  private String dbUser;
  private String dbPassword;
  private String dbURL;

  private List<ServerConfiguration> servers;

  public Configuration() {
    this(8888, 50, null, "hsdb", "hsdbpass", "jdbc:mysql://localhost:3306/hstestdb", new ArrayList<>());
  }

  public Configuration(
    int httpPort, int numClients, String webServiceURL, String dbUser, String dbPassword, String dbURL,
    List<ServerConfiguration> servers
  ) {
    this.httpPort = httpPort;
    this.numClients = numClients;
    this.webServiceURL = webServiceURL;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
    this.dbURL = dbURL;
    this.servers = servers;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public void setHttpPort(int httpPort) {
    this.httpPort = httpPort;
  }

  public int getNumClients() {
    return numClients;
  }

  public void setNumClients(int numClients) {
    this.numClients = numClients;
  }

  public String getWebServiceURL() {
    return webServiceURL;
  }

  public void setWebServiceURL(String webServiceURL) {
    this.webServiceURL = webServiceURL;
  }

  public String getDbUser() {
    return dbUser;
  }

  public void setDbUser(String dbUser) {
    this.dbUser = dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public void setDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
  }

  public String getDbURL() {
    return dbURL;
  }

  public void setDbURL(String dbURL) {
    this.dbURL = dbURL;
  }

  public List<ServerConfiguration> getServers() {
    return servers;
  }

  public void setServers(List<ServerConfiguration> servers) {
    this.servers = servers;
  }
}
