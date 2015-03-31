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

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
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

public class AbstractWebserviceTest {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractWebserviceTest.class);

    public static final String ENDPOINT = "http://localhost:8080/midpoint/ws/model-3";
    private static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
    
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

    @BeforeClass
    public void beforeTests(){
    	displayTestTitle("beforeTests");
        initSystem();
    }
    
	/**
     * Takes care of system initialization. Need to be done before any tests are to be run.
     * */
    protected static void initSystem() {
        try {
			MULTIPLE_THREAD_SEARCH_FILTER = parseElement(
			        "<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2a' >" +
			                "<path>c:givenName</path>" +
			                "<value>" + MULTIPLE_THREAD_USER_SEARCH_NAME + "</value>" +
			                "</equal>"
			);
		} catch (SAXException | IOException e) {
			throw new IllegalStateException("Error creating XML document " + e.getMessage(), e);
		}
    }
    
    @AfterClass
    public void afterTests() throws FaultMessage {
    	displayTestTitle("afterTests");
        modelPort = createModelPort();
        cleanRepository();
        LOGGER.info("WebService test suite finished.");
    }

    protected static ModelPortType createModelPort() {
    	return createModelPort("administrator", "5ecr3t");
    }

    /**
     * Creates webservice client connecting to midpoint
     * */
    protected static ModelPortType createModelPort(String username, String password) {

        LOGGER.info("Creating model client endpoint: {} , username={}, password={}", 
        		new Object[] {ENDPOINT, username, password});

        ModelService modelService = new ModelService();
        ModelPortType modelPort = modelService.getModelPort();
        BindingProvider bp = (BindingProvider)modelPort;
        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);

        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
        org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

        Map<String, Object> outProps = new HashMap<String, Object>();
        if (username != null) {
	        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
	        outProps.put(WSHandlerConstants.USER, username);
	        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
	        ClientPasswordHandler.setPassword(password);
	        outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());

	        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
	        cxfEndpoint.getOutInterceptors().add(wssOut);
        }

        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());
		cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());

        return modelPort;
    }
    
    private Object unmarshallFromFile(File file, String context) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(context);
        javax.xml.bind.Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement jaxbElement = (JAXBElement)jaxbUnmarshaller.unmarshal(file);
        return jaxbElement.getValue();
    }

    /**
     * Retrieves and returns actual system configuration
     * */
    protected static SystemConfigurationType getConfiguration() throws FaultMessage {
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        
		modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);

        return (SystemConfigurationType) objectHolder.value;
    }

    /**
     * returns URI of type passed as argument
     * */
    protected static String getTypeUri(Class<? extends ObjectType> type){
        String typeUri = NS_COMMON + "#" + type.getSimpleName();
        return typeUri;
    }

    protected static QName getTypeQName(Class<? extends ObjectType> type){
        return new QName(NS_COMMON, type.getSimpleName());
    }

    /**
     * Returns documentBuilder instance - used to create documents and polystrings
     * */
    protected static Document getDocument(){
        return DOMUtil.getDocument();
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

    protected static Element createTextElement(QName qname, String value, Document doc) {
        Element element = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        element.setTextContent(value);
        return element;
    }

//    /**
//     * Creates CredentialsType - type used to store password. It's value is String password
//     * representation passed via argument
//     * */
//    protected static CredentialsType createPasswordCredentials(String password) {
//        CredentialsType credentialsType = new CredentialsType();
//        credentialsType.setPassword(createPasswordType(password));
//        return credentialsType;
//    }
//
//    protected static PasswordType createPasswordType(String password) {
//        PasswordType passwordType = new PasswordType();
//        passwordType.setValue(createProtectedString(password));
//        return passwordType;
//    }
//
//    protected static ProtectedStringType createProtectedString(String clearValue) {
//        ProtectedStringType protectedString = new ProtectedStringType();
//        protectedString.setClearValue(clearValue);
//        return protectedString;
//    }

    protected static Element createPathElement(String stringPath, Document doc) {
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        return createTextElement(COMMON_PATH, pathDeclaration, doc);
    }

    protected static <T> JAXBElement<T> toJaxbElement(QName name, T value){
        return new JAXBElement<T>(name, (Class<T>) value.getClass(), value);
    }

    protected static Element parseElement(String stringXml) throws SAXException, IOException {
    	return DOMUtil.getFirstChildElement(DOMUtil.parseDocument(stringXml));
    }

    /**
     *  Clean the repository after tests. Preserves user administrator
     * */
    protected void cleanRepository() throws FaultMessage {
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        PagingType paging = new PagingType();

        modelPort.searchObjects(getTypeQName(UserType.class), null, null, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        List<ObjectType> objects = objectList.getObject();

        for(int i = 0; i < objects.size(); i++){
        	UserType user = (UserType) objects.get(i);
            if(!USER_ADMINISTRATOR_OID.equals(user.getOid())) {
            	display("Deleting user "+ModelClientUtil.toString(user));
            	deleteObject(UserType.class, user.getOid());
            }
        }
    }
    
	protected <O extends ObjectType> void deleteObject(Class<O> type, String oid) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(type));
    	delta.setChangeType(ChangeTypeType.DELETE);
    	delta.setOid(oid);
		deltaList.getDelta().add(delta);
		ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
		assertSuccess(deltaOpList);
    }

    protected void assertSuccess(ObjectDeltaOperationListType deltaOpList) {
    	for (ObjectDeltaOperationType deltaOperation: deltaOpList.getDeltaOperation()) {
    		OperationResultType result = deltaOperation.getExecutionResult();
    		assertSuccess(result);
    	}
	}

	protected void assertSuccess(OperationResultType result) {
		assertEquals("Operation "+result.getOperation()+" failed:"+result.getStatus()+": " + result.getMessage(),
				OperationResultStatusType.SUCCESS, result.getStatus());
		// TODO: look inside
	}
	
	protected <F extends FaultType> void assertFault(FaultMessage fault, Class<F> expectedFaultInfoClass) {
    	FaultType faultInfo = fault.getFaultInfo();
    	if (expectedFaultInfoClass != null && !expectedFaultInfoClass.isAssignableFrom(faultInfo.getClass())) {
    		AssertJUnit.fail("Expected that faultInfo will be of type "+expectedFaultInfoClass+", but it was "+faultInfo.getClass());
    	}
    	OperationResultType result = faultInfo.getOperationResult();
    	assertEquals("Expected that resut in FaultInfo will be fatal error, but it was "+result.getStatus(),
    			OperationResultStatusType.FATAL_ERROR, result.getStatus());
	}


	protected void displayFault(FaultMessage fault) {
		System.out.println("Got fault:");
		fault.printStackTrace(System.out);
		LOGGER.error("Got fault:\n{}",fault,fault);
	}

	protected void displayTestTitle(String testName) {		
		TestUtil.displayTestTile(testName);
	}

	protected void display(String msg) {
		System.out.println(msg);
		LOGGER.info("{}", msg);
	}

	
    @Test
    public void test000SanityAndCleanup() throws Exception {
    	final String TEST_NAME = "test000SanityAndCleanup";
    	displayTestTitle(TEST_NAME);
        modelPort = createModelPort();
        configurationType = getConfiguration();
        cleanRepository();
    }

}
