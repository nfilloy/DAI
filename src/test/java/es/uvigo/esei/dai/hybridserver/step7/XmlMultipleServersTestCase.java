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
import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import es.uvigo.esei.dai.hybridserver.http.MIME;

public class XmlMultipleServersTestCase
extends AbstractGetMultipleServersTestCase {
	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetLocales(int serverIndex) throws IOException {
		final String[] uuids = getLocalXmlUUIDs(serverIndex);
		final String[] contents = getLocalXmlFiles(serverIndex);
		
		testMultipleGets(uuids, contents, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetRemotes(int serverIndex) throws IOException {
		final String[] uuids = getRemoteXmlUUIDs(serverIndex);
		final String[] contents = getRemoteXmlFiles(serverIndex);
		
		testMultipleGets(uuids, contents, serverIndex);
	}

	@Override
	protected String getContentType() {
		return MIME.APPLICATION_XML.getMime();
	}

	@Override
	protected String getResourceName() {
		return "xml";
	}
}
