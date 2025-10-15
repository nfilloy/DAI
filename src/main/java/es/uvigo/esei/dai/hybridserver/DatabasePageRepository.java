package es.uvigo.esei.dai.hybridserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class DatabasePageRepository implements PageRepository {
  private static final String TABLE_NAME = "HTML";

  private final String url;
  private final String user;
  private final String password;

  DatabasePageRepository(String url, String user, String password) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Database URL must not be null or blank");
    }

    this.url = url;
    this.user = user;
    this.password = password;
  }

  @Override
  public String getPage(String uuid) {
    final String sql = "SELECT content FROM " + TABLE_NAME + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to retrieve page", e);
    }
  }

  @Override
  public List<String> listUuids() {
    final String sql = "SELECT uuid FROM " + TABLE_NAME;
    final List<String> uuids = new ArrayList<>();

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        uuids.add(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list pages", e);
    }

    return uuids;
  }

  @Override
  public String createPage(String content) {
    final String sql = "INSERT INTO " + TABLE_NAME + " (uuid, content) VALUES (?, ?)";

    for (int i = 0; i < 5; i++) {
      final String uuid = UUID.randomUUID().toString();

      try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, uuid);
        stmt.setString(2, content);
        stmt.executeUpdate();

        return uuid;
      } catch (SQLException e) {
        if (isDuplicateKey(e)) {
          continue;
        }

        throw new RuntimeException("Failed to create page", e);
      }
    }

    throw new RuntimeException("Unable to generate unique UUID for page");
  }

  @Override
  public boolean deletePage(String uuid) {
    final String sql = "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?";

    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid);
      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete page", e);
    }
  }

  private Connection openConnection() throws SQLException {
    if (this.user != null && !this.user.isBlank()) {
      return DriverManager.getConnection(this.url, this.user, this.password);
    }

    return DriverManager.getConnection(this.url);
  }

  private static boolean isDuplicateKey(SQLException ex) {
    final String state = ex.getSQLState();
    return state != null && state.startsWith("23");
  }
}
