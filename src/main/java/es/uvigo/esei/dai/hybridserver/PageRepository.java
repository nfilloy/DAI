package es.uvigo.esei.dai.hybridserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class PageRepository {
  private final Map<String, String> pages;

  PageRepository(Map<String, String> initialPages) {
    final Map<String, String> copy = new LinkedHashMap<>();
    if (initialPages != null) {
      initialPages.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          copy.put(key, value);
        }
      });
    }

    this.pages = Collections.synchronizedMap(copy);
  }

  String getPage(String uuid) {
    if (uuid == null) {
      return null;
    }

    return this.pages.get(uuid);
  }

  String createPage(String content) {
    final String uuid = generateUuid();
    this.pages.put(uuid, content);
    return uuid;
  }

  boolean deletePage(String uuid) {
    if (uuid == null) {
      return false;
    }

    return this.pages.remove(uuid) != null;
  }

  List<String> listUuids() {
    synchronized (this.pages) {
      return new ArrayList<>(this.pages.keySet());
    }
  }

  private String generateUuid() {
    String candidate;
    do {
      candidate = UUID.randomUUID().toString();
    } while (this.pages.containsKey(candidate));

    return candidate;
  }
}
