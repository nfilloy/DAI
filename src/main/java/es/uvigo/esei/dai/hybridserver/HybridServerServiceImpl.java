package es.uvigo.esei.dai.hybridserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.jws.WebService;

@WebService(
    endpointInterface = "es.uvigo.esei.dai.hybridserver.HybridServerService",
    serviceName = "HybridServerService",
    targetNamespace = "http://hybridserver.dai.esei.uvigo.es/"
)
public class HybridServerServiceImpl implements HybridServerService {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public HybridServerServiceImpl(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Override
    public List<String> listHtmlUuids() {
        return listUuids("HTML");
    }

    @Override
    public List<String> listXmlUuids() {
        return listUuids("XML");
    }

    @Override
    public List<String> listXsdUuids() {
        return listUuids("XSD");
    }

    @Override
    public List<String> listXsltUuids() {
        return listUuids("XSLT");
    }

    @Override
    public String getHtml(String uuid) {
        return getContent("HTML", uuid);
    }

    @Override
    public String getXml(String uuid) {
        return getContent("XML", uuid);
    }

    @Override
    public String getXsd(String uuid) {
        return getContent("XSD", uuid);
    }

    @Override
    public String getXslt(String uuid) {
        return getContent("XSLT", uuid);
    }

    @Override
    public String getXsdUuidForXslt(String xsltUuid) {
        final String sql = "SELECT xsd FROM XSLT WHERE uuid = ?";

        try (Connection connection = openConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, xsltUuid);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private List<String> listUuids(String tableName) {
        final String sql = "SELECT uuid FROM " + tableName;
        final List<String> uuids = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                uuids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            // Return empty list on error
        }

        return uuids;
    }

    private String getContent(String tableName, String uuid) {
        final String sql = "SELECT content FROM " + tableName + " WHERE uuid = ?";

        try (Connection connection = openConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private Connection openConnection() throws SQLException {
        if (this.dbUser != null && !this.dbUser.isBlank()) {
            return DriverManager.getConnection(this.dbUrl, this.dbUser, this.dbPassword);
        }
        return DriverManager.getConnection(this.dbUrl);
    }
}
