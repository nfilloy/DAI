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

import org.junit.jupiter.api.BeforeEach;

import es.uvigo.esei.dai.hybridserver.http.MIME;

public class HTMLClientRequestsWithDatabaseTest extends AbstractClientRequestWithDatabaseTest {
	@BeforeEach
	public void initAttributes() {
		this.invalidUUID = "12345678-abcd-1234-ab12-9876543210ab";
		
		// Estas páginas se insertan en la base de datos al inicio del test.
		this.pages = new String[][] {
		//{ "uuid",                                 "texto contenido por la página" }
			{ "6df1047e-cf19-4a83-8cf3-38f5e53f7725", "<html><body>This is the html page 6df1047e-cf19-4a83-8cf3-38f5e53f7725.</body></html>" },
			{ "79e01232-5ea4-41c8-9331-1c1880a1d3c2", "<html><body>This is the html page 79e01232-5ea4-41c8-9331-1c1880a1d3c2.</body></html>" },
			{ "a35b6c5e-22d6-4707-98b4-462482e26c9e", "<html><body>This is the html page a35b6c5e-22d6-4707-98b4-462482e26c9e.</body></html>" },
			{ "3aff2f9c-0c7f-4630-99ad-27a0cf1af137", "<html><body>This is the html page 3aff2f9c-0c7f-4630-99ad-27a0cf1af137.</body></html>" },
			{ "77ec1d68-84e1-40f4-be8e-066e02f4e373", "<html><body>This is the html page 77ec1d68-84e1-40f4-be8e-066e02f4e373.</body></html>" }
		};
	}

	@Override
	protected String getTableName() {
		return "HTML";
	}

	@Override
	protected String getResourceName() {
		return "html";
	}

	@Override
	protected String getContentType() {
		return MIME.TEXT_HTML.getMime();
	}
}
