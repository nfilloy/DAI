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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLConfigurationLoader {
  private static final String NAMESPACE = "http://www.esei.uvigo.es/dai/hybridserver";

  public Configuration load(Reader reader) throws Exception {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(getClass().getResource("/configuration.xsd"));

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setSchema(schema);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setErrorHandler(new StrictErrorHandler());

    Document doc = db.parse(new InputSource(reader));

    Element root = doc.getDocumentElement();

    Element connectionsEl = getElement(root, "connections");
    int httpPort = Integer.parseInt(getElementText(connectionsEl, "http"));
    String webServiceURL = getElementText(connectionsEl, "webservice");
    int numClients = Integer.parseInt(getElementText(connectionsEl, "numClients"));

    Element databaseEl = getElement(root, "database");
    String dbUser = getElementText(databaseEl, "user");
    String dbPassword = getElementText(databaseEl, "password");
    String dbURL = getElementText(databaseEl, "url");

    Element serversEl = getElement(root, "servers");
    List<ServerConfiguration> servers = new ArrayList<>();
    NodeList serverNodes = serversEl.getElementsByTagNameNS(NAMESPACE, "server");
    for (int i = 0; i < serverNodes.getLength(); i++) {
      Element serverEl = (Element) serverNodes.item(i);
      servers.add(new ServerConfiguration(
        serverEl.getAttribute("name"),
        serverEl.getAttribute("wsdl"),
        serverEl.getAttribute("namespace"),
        serverEl.getAttribute("service"),
        serverEl.getAttribute("httpAddress")
      ));
    }

    return new Configuration(httpPort, numClients, webServiceURL, dbUser, dbPassword, dbURL, servers);
  }

  private Element getElement(Element parent, String localName) {
    NodeList nodes = parent.getElementsByTagNameNS(NAMESPACE, localName);
    return (Element) nodes.item(0);
  }

  private String getElementText(Element parent, String localName) {
    return getElement(parent, localName).getTextContent();
  }

  private static class StrictErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }
}
