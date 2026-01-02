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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.Reader;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import es.uvigo.esei.dai.hybridserver.XMLConfigurationLoader;

public class InvalidXMLConfigurationLoaderTest {
	private XMLConfigurationLoader xmlConfiguration;

	public InvalidXMLConfigurationLoaderTest() {
		this.xmlConfiguration = new XMLConfigurationLoader();
	}

	public static Stream<Arguments> invalidConfigurationFiles() {
		return Stream.of(
			arguments(named("Missing http parameter", "/invalid-configuration1.xml")),
			arguments(named("Missing database configuration", "/invalid-configuration2.xml")),
			arguments(named("Invalid http port number", "/invalid-configuration3.xml")),
			arguments(named("Invalid number of clients", "/invalid-configuration4.xml")),
			arguments(named("Missing attributes in server", "/invalid-configuration5.xml"))
		);
	}

	@ParameterizedTest
	@MethodSource("invalidConfigurationFiles")
	public final void testLoad(String xmlFilePath) {
		assertThrows(Exception.class, () -> {
			try (Reader xmlReader = openReaderToFile(xmlFilePath)) {
				xmlConfiguration.load(xmlReader);
			}
		});
	}
}
