package es.uvigo.esei.dai.hybridserver;

import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(
    name = "HybridServerService",
    targetNamespace = "http://hybridserver.dai.esei.uvigo.es/"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public interface HybridServerService {

    @WebMethod
    List<String> listHtmlUuids();

    @WebMethod
    List<String> listXmlUuids();

    @WebMethod
    List<String> listXsdUuids();

    @WebMethod
    List<String> listXsltUuids();

    @WebMethod
    String getHtml(String uuid);

    @WebMethod
    String getXml(String uuid);

    @WebMethod
    String getXsd(String uuid);

    @WebMethod
    String getXslt(String uuid);

    @WebMethod
    String getXsdUuidForXslt(String xsltUuid);
}
