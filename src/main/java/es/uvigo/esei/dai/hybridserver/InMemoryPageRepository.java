package es.uvigo.esei.dai.hybridserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class InMemoryPageRepository implements PageRepository {
  private final Map<String, String> pages;

  InMemoryPageRepository(Map<String, String> initialPages) {
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

  @Override
  public String getPage(String uuid) {
    if (uuid == null) {
      return null;
    }

    return this.pages.get(uuid);
  }

  @Override
  public List<String> listUuids() {
    synchronized (this.pages) {
      return new ArrayList<>(this.pages.keySet());
    }
  }

  @Override
  public String createPage(String content) {
    String uuid;
    do {
      uuid = UUID.randomUUID().toString();
    } while (this.pages.containsKey(uuid));

    this.pages.put(uuid, content);
    return uuid;
  }

  @Override
  public boolean deletePage(String uuid) {
    if (uuid == null) {
      return false;
    }

    return this.pages.remove(uuid) != null;
  }
}
