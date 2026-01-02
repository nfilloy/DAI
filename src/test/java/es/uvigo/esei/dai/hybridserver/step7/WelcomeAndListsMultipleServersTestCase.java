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
package es.uvigo.esei.dai.hybridserver.step7;

import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getContentWithType;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WelcomeAndListsMultipleServersTestCase
extends MultipleServersTestCase {
	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testWelcome(int serverIndex) throws IOException {
		final String url = serversHTTPURL[serverIndex];

		assertThat(getContentWithType(url, "text/html"), containsString("Hybrid Server"));
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testHtmlList(int serverIndex) throws IOException {
		testList("html", getAllHtmlUUIDs(), serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testXmlList(int serverIndex) throws IOException {
		testList("xml", getAllXmlUUIDs(), serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testXsdList(int serverIndex) throws IOException {
		testList("xsd", getAllXsdUUIDs(), serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testXsltList(int serverIndex) throws IOException {
		testList("xslt", getAllXsltUUIDs(), serverIndex);
	}

	protected void testList(final String resource, final String[] uuids, final int serverIndex)
	throws IOException {
		final String url = serversHTTPURL[serverIndex] + resource;
		
		final String content = getContentWithType(url, "text/html");
		
		for (String uuid : uuids) {
			assertThat(content, containsString(uuid));
		}
	}
}
