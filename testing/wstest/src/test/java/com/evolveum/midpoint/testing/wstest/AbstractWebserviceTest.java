/*
 * Copyright (c) 2013-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.wstest;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.crypto.AESProtector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;
import org.w3c.dom.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.util.*;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

/**
*   Test Framework Util Class
*
*   <p>
*       This class contains static methods and functionality needed
*       across all test suites in this framework
*       It takes care of initialization of modelPort - webService client,
*       which is used to communicate with midpoint in all tests
*   </p>
*
*
*   @author semancik
*   @author Erik Suta
*
* */

public class AbstractWebserviceTest extends AbstractModelIntegrationTest {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractWebserviceTest.class);

    public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    public static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";
    protected static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");

    protected static final QName COMMON_PATH = new QName(NS_COMMON, "path");
    protected static final QName COMMON_VALUE = new QName(NS_COMMON, "value");

    protected static DocumentBuilder domDocumentBuilder;
    protected static ModelPortType modelPort;
    protected static SystemConfigurationType configurationType;

    public static final String MULTIPLE_THREAD_USER_SEARCH_NAME = "Barbara";
    public static Element MULTIPLE_THREAD_SEARCH_FILTER;

    @Autowired(required=true)
    
    
    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// get the jetty server form the destination
		EndpointInfo ei = new EndpointInfo();
		ei.setAddress(serviceFactory.getAddress());
		Destination destination = df.getDestination(ei);
		JettyHTTPDestination jettyDestination = (JettyHTTPDestination) destination;
		ServerEngine engine = jettyDestination.getEngine();
		Handler handler = engine.getServant(new URL(serviceFactory.getAddress()));
		org.mortbay.jetty.Server server = handler.getServer(); // The Server
		 
		// We have to create a HandlerList structure that includes both a ResourceHandler for the static
		// content as well as the ContextHandlerCollection created by CXF (which we retrieve as serverHandler).
		Handler serverHandler = server.getHandler();
		HandlerList handlerList = new HandlerList();
		ResourceHandler resourceHandler = new ResourceHandler();
		handlerList.addHandler(resourceHandler);
		handlerList.addHandler(serverHandler);
		 
		// replace the CXF servlet connect collection with the list.
		server.setHandler(handlerList);
		// and tell the handler list that it is alive.
		handlerList.start();
		 
		// setup the resource handler
		File staticContentFile = new File(staticContentPath); // ordinary pathname.
		URL targetURL = new URL("file://" + staticContentFile.getCanonicalPath());
		FileResource fileResource = new FileResource(targetURL);
		resourceHandler.setBaseResource(fileResource);
	}

	/**
     * Takes care of system inicialization. Need to be done before any tests are to be run.
     * */
    protected static void initSystem(){
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            domDocumentBuilder = factory.newDocumentBuilder();

            MULTIPLE_THREAD_SEARCH_FILTER = parseElement(
                    "<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2a' >" +
                            "<path>c:givenName</path>" +
                            "<value>" + MULTIPLE_THREAD_USER_SEARCH_NAME + "</value>" +
                            "</equal>"
            );

        }catch(ParserConfigurationException ex){
            throw new IllegalStateException("Error creating XML document " + ex.getMessage());
        }
        catch (SAXException se){
            throw new IllegalStateException("Error creating XML document " + se.getMessage());
        }
        catch (IOException ioe){
            throw new IllegalStateException("Error creating XML document " + ioe.getMessage());
        }
    }

    /**
     * Creates webservice client connecting to midpoint
     * */
    protected static ModelPortType createModelPort(){
        String endpointUrl = "http://localhost:8080/midpoint/model/model-1";
        String username = "administrator";

        LOGGER.info("OpenDJtest: #createModelPort: Endpoint URL: " + endpointUrl);

        ModelService modelService = new ModelService();
        ModelPortType modelPort = modelService.getModelPort();
        BindingProvider bp = (BindingProvider)modelPort;
        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
        org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.USER, username);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(wssOut);

        return modelPort;
    }

    /**
     * Retrieves and returns actual system configuration
     * */
    protected static SystemConfigurationType getConfiguration(ModelPortType modelPort) throws FaultMessage{
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

		modelPort.getObject(getTypeUri(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, objectHolder, resultHolder);

        return (SystemConfigurationType) objectHolder.value;
    }

    /**
     * returns URI of type passed as argument
     * */
    protected static String getTypeUri(Class<? extends ObjectType> type){
        String typeUri = NS_COMMON + "#" + type.getSimpleName();
        return typeUri;
    }

    /**
     * Returns documentBuilder instance - used to create documents and polystrings
     * */
    protected static Document getDocument(){
        return domDocumentBuilder.newDocument();
    }

    /**
     *  Creates polystring type with String value passed in argument
     * */
    protected static PolyStringType createPolyStringType(String string, Document doc) {
        PolyStringType polyStringType = new PolyStringType();
        Element origElement = createTextElement(TYPES_POLYSTRING_ORIG, string, doc);
        polyStringType.getContent().add(origElement);
        return polyStringType;
    }

    /**
     *
     * */
    protected static Element createTextElement(QName qname, String value, Document doc) {
        Element element = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        element.setTextContent(value);
        return element;
    }

    /**
     * Creates CredentialsType - type used to store password. It's value is String password
     * representation passed via argument
     * */
    protected static CredentialsType createPasswordCredentials(String password) {
        CredentialsType credentialsType = new CredentialsType();
        credentialsType.setPassword(createPasswordType(password));
        return credentialsType;
    }

    protected static PasswordType createPasswordType(String password) {
        PasswordType passwordType = new PasswordType();
        passwordType.setValue(createProtectedString(password));
        return passwordType;
    }

    protected static ProtectedStringType createProtectedString(String clearValue) {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue(clearValue);
        return protectedString;
    }

    protected static Element createPathElement(String stringPath, Document doc) {
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        return createTextElement(COMMON_PATH, pathDeclaration, doc);
    }

    protected static <T> JAXBElement<T> toJaxbElement(QName name, T value){
        return new JAXBElement<T>(name, (Class<T>) value.getClass(), value);
    }

    protected static Element parseElement(String stringXml) throws SAXException, IOException {
        Document document = domDocumentBuilder.parse(IOUtils.toInputStream(stringXml, "utf-8"));
        return getFirstChildElement(document);
    }

    protected static Element getFirstChildElement(Node parent) {
        if (parent == null || parent.getChildNodes() == null) {
            return null;
        }

        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
        }

        return null;
    }

    /**
     *  Clean the repository after tests. Preserves user administrator
     * */
    protected void cleanRepository() throws FaultMessage {
        OperationOptionsType options = new OperationOptionsType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        PagingType paging = new PagingType();
        String adminOid = "00000000-0000-0000-0000-000000000002";

        modelPort.listObjects(getTypeUri(UserType.class), paging, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        List<ObjectType> objects = objectList.getObject();

        for(int i = 0; i < objects.size(); i++){
            if(objects.get(i).getOid().compareTo(adminOid) != 0)
                modelPort.deleteObject(getTypeUri(UserType.class), objects.get(i).getOid());
        }
    }

    /**
     *  Stand-alone test - if manual repository clean-up is needed
     * */
    @Test
    public void cleanRepositoryTest() throws FaultMessage {
        initSystem();
        modelPort = createModelPort();
        configurationType = getConfiguration(modelPort);
        cleanRepository();
    }

}
