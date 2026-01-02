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
package es.uvigo.esei.dai.hybridserver.step6;

import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.openReaderToFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.Reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.uvigo.esei.dai.hybridserver.Configuration;
import es.uvigo.esei.dai.hybridserver.ServerConfiguration;
import es.uvigo.esei.dai.hybridserver.XMLConfigurationLoader;

public class XMLConfigurationLoaderTest {
	private XMLConfigurationLoader xmlConfiguration;

	@BeforeEach
	public void setUp() throws Exception {
		this.xmlConfiguration = new XMLConfigurationLoader();
	}

	@Test
	public final void testLoad() throws Exception {
		try (Reader xmlReader = openReaderToFile("/configuration.xml")) {
			final Configuration configuration = this.xmlConfiguration.load(xmlReader);

			assertThat(configuration.getHttpPort(), is(equalTo(10000)));
			assertThat(configuration.getNumClients(), is(equalTo(15)));
			assertThat(configuration.getWebServiceURL(), is(equalTo("http://localhost:20000/hybridserver")));

			assertThat(configuration.getDbUser(), is(equalTo("hsdb")));
			assertThat(configuration.getDbPassword(), is(equalTo("hsdbpass")));
			assertThat(configuration.getDbURL(), is(equalTo("jdbc:mysql://localhost:3306/hsdb")));

			assertThat(configuration.getServers(), hasSize(3));
			ServerConfiguration server = configuration.getServers().get(0);
			assertThat(server.getName(), is(equalTo("Server 2")));
			assertThat(server.getWsdl(), is(equalTo("http://localhost:20001/hs?wsdl")));
			assertThat(server.getNamespace(), is(equalTo("http://hybridserver.dai.esei.uvigo.es/")));
			assertThat(server.getService(), is(equalTo("ControllerService")));
			assertThat(server.getHttpAddress(), is(equalTo("http://localhost:10001/")));

			server = configuration.getServers().get(1);
			assertThat(server.getName(), is(equalTo("Server 3")));
			assertThat(server.getWsdl(), is(equalTo("http://localhost:20002/hs?wsdl")));
			assertThat(server.getNamespace(), is(equalTo("http://hybridserver.dai.esei.uvigo.es/")));
			assertThat(server.getService(), is(equalTo("ControllerService")));
			assertThat(server.getHttpAddress(), is(equalTo("http://localhost:10002/")));

			server = configuration.getServers().get(2);
			assertThat(server.getName(), is(equalTo("Server 4")));
			assertThat(server.getWsdl(), is(equalTo("http://localhost:20003/hs?wsdl")));
			assertThat(server.getNamespace(), is(equalTo("http://hybridserver.dai.esei.uvigo.es/")));
			assertThat(server.getService(), is(equalTo("ControllerService")));
			assertThat(server.getHttpAddress(), is(equalTo("http://localhost:10003/")));
		}
	}
}
