package es.uvigo.esei.dai.hybridserver;

import java.util.List;

interface PageRepository {
  String getPage(String uuid);

  List<String> listUuids();

  String createPage(String content);

  boolean deletePage(String uuid);
}
