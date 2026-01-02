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
package es.uvigo.esei.dai.hybridserver.step4;

import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.deleteStatus;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.extractUUIDFromText;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getContent;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getContentWithType;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getStatus;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.postContent;
import static es.uvigo.esei.dai.hybridserver.utils.matchers.EqualsToIgnoringSpacesMatcher.equalsToIgnoringSpaces;
import static es.uvigo.esei.dai.hybridserver.utils.matchers.TableMatcher.hasTable;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.utils.JdbcTestCase;

@Timeout(5L)
public abstract class AbstractClientRequestWithDatabaseTest 
extends JdbcTestCase {
	protected abstract String getTableName();
	protected abstract String getResourceName();
	protected abstract String getContentType();

	protected HybridServer server;
	protected String url;
	protected String invalidUUID;
	protected String[][] pages;
	
	
	@BeforeEach
	public void startServer() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty("port", Integer.toString(8888));
		properties.setProperty("numClients", "50");
		properties.setProperty("db.url", getConnectionUrl());
		properties.setProperty("db.user", getUsername());
		properties.setProperty("db.password", getPassword());
		
		this.server = new HybridServer(properties);
		this.url = String.format("http://localhost:%d/", this.server.getPort());
		
		this.server.start();
	}

	@AfterEach
	public void stopServer() {
		this.server.close();
	}

	@Test
	public void testGetList() throws IOException {
		final String pageURL = url + getResourceName();
		final String content = getContentWithType(pageURL, "text/html");
		
		for (String[] page : pages) {
			final String uuid = page[0];
			
			assertThat(content, containsString(uuid));
		}
	}

	@Test
	public void testGet() throws IOException {
		final String pageURL = url + getResourceName();
		
		for (String[] page : pages) {
			final String uuid = page[0];
			final String uuidPageURL = pageURL + "?uuid=" + uuid;
			
			final String content = getContentWithType(uuidPageURL, getContentType());
	
			assertThat(content, is(equalsToIgnoringSpaces(page[1])));
		}
	}

	@Test
	public void testPost() throws Exception {
		final String content = "<xml><name>Testing POST</name></xml>";
		
		// Envío de la página y extracción del uuid de la nueva página
		final String responseContent = postContent(url + getResourceName(), singletonMap(getResourceName(), content));
		final String uuid = extractUUIDFromText(responseContent);
		assertThat(uuid, is(notNullValue()));
		
		// Verificación de que la página de respuesta contiene un enlace a la nueva página
		final String uuidHyperlink = "<a href=\"" + getResourceName() + "?uuid=" + uuid + "\">" + uuid + "</a>";
		assertThat(responseContent, containsString(uuidHyperlink));
		
		// Recuperación de la nueva página
		final String url = this.url + getResourceName() + "?uuid=" + uuid;
		assertThat("The new page couldn't be retrieved", getContent(url), is(equalTo(content)));
		
		// Comprobación de la inserción en la base de datos
		assertThat(getConnection(), hasTable(getSchema(), getTableName())
			.withColumn("uuid").withValue(uuid)
		);
	}

	@Test
	public void testDelete() throws Exception {
		final String uuid = pages[0][0];
		final String url = this.url + getResourceName() + "?uuid=" + uuid;
	
		// Eliminación de la página
		assertThat("The page couldn't be deleted", deleteStatus(url), is(equalTo(200)));
		
		// Comprobación de la eliminación en la base de datos
		assertThat(getConnection(), hasTable(getSchema(), getTableName())
			.withColumn("uuid").withoutValue(uuid)
		);
	
		// Comprobación de la eliminación vía web
		assertThat("The page already exists", getStatus(url), is(equalTo(404)));
	}

	@Test
	public void testGetInvalidPage() throws IOException {
		final String pageURL = url + getResourceName() + "?uuid=" + invalidUUID;

		assertThat(getStatus(pageURL), is(equalTo(404)));
	}

	@Test
	public void testDeleteNonexistentPage() throws IOException {
		final String pageURL = this.url + getResourceName() + "?uuid=" + invalidUUID;

		assertThat(getStatus(pageURL), is(equalTo(404)));
	}
}