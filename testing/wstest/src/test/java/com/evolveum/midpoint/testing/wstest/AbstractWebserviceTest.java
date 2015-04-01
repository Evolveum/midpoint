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
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
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
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

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

public abstract class AbstractWebserviceTest {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractWebserviceTest.class);

    public static final File COMMON_DIR = new File("src/test/resources/common");
    
    public static final String ENDPOINT = "http://localhost:8080/midpoint/ws/model-3";
    public static final String USER_ADMINISTRATOR_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
    public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";
    
    // No authorization
 	public static final File USER_NOBODY_FILE = new File(COMMON_DIR, "user-nobody.xml");
 	public static final String USER_NOBODY_USERNAME = "nobody";
 	public static final String USER_NOBODY_GIVEN_NAME = "No";
 	public static final String USER_NOBODY_FAMILY_NAME = "Body";
 	public static final String USER_NOBODY_PASSWORD = "nopassword";

 	// WS authorization only
 	public static final File USER_CYCLOPS_FILE = new File(COMMON_DIR, "user-cyclops.xml");
 	public static final String USER_CYCLOPS_USERNAME = "cyclops";
 	public static final String USER_CYCLOPS_PASSWORD = "cyclopassword";
 	
 	// WS and reader authorization
 	public static final File USER_SOMEBODY_FILE = new File(COMMON_DIR, "user-somebody.xml");
 	public static final String USER_SOMEBODY_USERNAME = "somebody";
 	public static final String USER_SOMEBODY_PASSWORD = "somepassword";
 	
 	public static final File ROLE_WS_FILE = new File(COMMON_DIR, "role-ws.xml");
	public static final File ROLE_READER_FILE = new File(COMMON_DIR, "role-reader.xml");
 	
	protected static final Pattern PATTERN_AUDIT_EVENT_ID = Pattern.compile(".*\\seid=([^,]+),\\s.*");
	protected static final Pattern PATTERN_AUDIT_SESSION_ID = Pattern.compile(".*\\ssid=([^,]+),\\s.*");
	protected static final Pattern PATTERN_AUDIT_TASK_ID = Pattern.compile(".*\\stid=([^,]+),\\s.*");
	
    
    public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    public static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";
    protected static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");

    protected static final QName COMMON_PATH = new QName(NS_COMMON, "path");
    protected static final QName COMMON_VALUE = new QName(NS_COMMON, "value");

    protected static DocumentBuilder domDocumentBuilder;
    protected static ModelPortType modelPort;
    protected static SystemConfigurationType configurationType;

    public static final String MULTIPLE_THREAD_USER_SEARCH_NAME = "Barbara";

	private static final File SERVER_LOG_FILE = new File("/opt/tomcat/logs/idm.log");
	private static final String AUDIT_LOGGER_NAME = "com.evolveum.midpoint.audit.log";
	
    public static Element MULTIPLE_THREAD_SEARCH_FILTER;

    @BeforeClass
    public void beforeTests(){
    	displayTestTitle("beforeTests");
        init();
    }
    
	/**
     * Takes care of system initialization. Need to be done before any tests are to be run.
     * */
    protected void init() {
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
    	return createModelPort(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
    }
    
    protected static ModelPortType createModelPort(String username, String password) {
    	return createModelPort(username, password, WSConstants.PW_DIGEST);
    }

    /**
     * Creates webservice client connecting to midpoint
     * */
    protected static ModelPortType createModelPort(String username, String password, String passwordType) {

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
	        outProps.put(WSHandlerConstants.PASSWORD_TYPE, passwordType);
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
    protected SystemConfigurationType getConfiguration() throws FaultMessage {
        return getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value());
    }

    protected <O extends ObjectType> O getObject(Class<O> type, String oid) throws FaultMessage {
		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
		modelPort.getObject(getTypeQName(type), oid, null, objectHolder, resultHolder);

		assertSuccess(resultHolder.value);
        return (O) objectHolder.value;
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
    	cleanObjects(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value());
    	cleanObjects(RoleType.class, SystemObjectsType.ROLE_SUPERUSER.value(), SystemObjectsType.ROLE_END_USER.value());
    }
    
    private <O extends ObjectType> void cleanObjects(Class<O> type, String... protectedOids) throws FaultMessage {
    	Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        PagingType paging = new PagingType();

        modelPort.searchObjects(getTypeQName(type), null, null, objectListHolder, resultHolder);

        List<String> protectedOidList = Arrays.asList(protectedOids);
        ObjectListType objectList = objectListHolder.value;
        for (ObjectType object: objectList.getObject()) {
        	if (!protectedOidList.contains(object.getOid())) {
        		display("Deleting "+type.getSimpleName()+" "+ModelClientUtil.toString(object));
            	deleteObject(type, object.getOid());
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
	
	protected <O extends ObjectType> String addObject(O object) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(object.getClass()));
    	delta.setChangeType(ChangeTypeType.ADD);
    	delta.setObjectToAdd(object);
		deltaList.getDelta().add(delta);
		ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
		assertSuccess(deltaOpList);
		return deltaOpList.getDeltaOperation().get(0).getObjectDelta().getOid();
    }
	
	protected <O extends ObjectType> void assertObjectCount(Class<O> type, int expCount) throws FaultMessage {
		assertEquals("Unexpected count of "+type.getSimpleName(), expCount, countObjects(type));
	}
	
	protected <O extends ObjectType> int countObjects(Class<O> type) throws FaultMessage {
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		modelPort.searchObjects(getTypeQName(type), null, null, objectListHolder, resultHolder);
		assertSuccess(resultHolder.value);
		Integer count = objectListHolder.value.getCount();
		if (count != null) {
			assertEquals("Wrong count", (Integer)objectListHolder.value.getObject().size(), count);
		}
		return objectListHolder.value.getObject().size();
	}

	
	protected void assertUser(UserType user, String expOid, String expName) {
		assertEquals("Wrong user oid", expOid, user.getOid());
		assertEquals("Wrong user name", expName, ModelClientUtil.getOrig(user.getName()));
	}

	protected void assertUser(UserType user, String expOid, String expName, String expGivenName, String expFamilyName) {
		assertEquals("Wrong user oid", expOid, user.getOid());
		assertEquals("Wrong user name", expName, ModelClientUtil.getOrig(user.getName()));
		assertEquals("Wrong user givenName", expGivenName, ModelClientUtil.getOrig(user.getGivenName()));
		assertEquals("Wrong user familyName", expFamilyName, ModelClientUtil.getOrig(user.getFamilyName()));
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
	
	protected <F extends FaultType> void assertFaultMessage(FaultMessage fault, Class<F> expectedFaultInfoClass) {
    	FaultType faultInfo = fault.getFaultInfo();
    	if (expectedFaultInfoClass != null && !expectedFaultInfoClass.isAssignableFrom(faultInfo.getClass())) {
    		AssertJUnit.fail("Expected that faultInfo will be of type "+expectedFaultInfoClass+", but it was "+faultInfo.getClass());
    	}
    	OperationResultType result = faultInfo.getOperationResult();
    	assertEquals("Expected that resut in FaultInfo will be fatal error, but it was "+result.getStatus(),
    			OperationResultStatusType.FATAL_ERROR, result.getStatus());
	}
	
	protected void assertSoapFault(SOAPFaultException e, String expectedCode, String expectedMessage) {
    	SOAPFault fault = e.getFault();
    	String faultCode = fault.getFaultCode();
    	display("SOAP fault code: "+faultCode);
    	assertTrue("Unexpected fault code: "+faultCode, faultCode.endsWith(expectedCode));
    	String message = e.getMessage();
    	assertTrue("Unexpected fault message: "+message, message.contains(expectedMessage));
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

	protected LogfileTestTailer createLogTailer() throws IOException {
		return new LogfileTestTailer(SERVER_LOG_FILE, AUDIT_LOGGER_NAME, true);
	}

	private void checkAuditEnabled(SystemConfigurationType configurationType) throws FaultMessage {
		LoggingConfigurationType loggingConfig = configurationType.getLogging();
        AuditingConfigurationType auditConfig = loggingConfig.getAuditing();
        if (auditConfig == null) {
        	auditConfig = new AuditingConfigurationType();
        	auditConfig.setEnabled(true);
        	loggingConfig.setAuditing(auditConfig);
        } else {
        	if (BooleanUtils.isTrue(auditConfig.isEnabled())) {
        		return;
        	}
        	auditConfig.setEnabled(true);
        }
        
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(SystemConfigurationType.class));
    	delta.setChangeType(ChangeTypeType.MODIFY);
    	delta.setOid(SystemObjectsType.SYSTEM_CONFIGURATION.value());
    	ItemDeltaType itemDelta = new ItemDeltaType();
    	itemDelta.setPath(ModelClientUtil.createItemPathType("logging"));
    	itemDelta.setModificationType(ModificationTypeType.REPLACE);
    	itemDelta.getValue().add(loggingConfig);
		delta.getItemDelta().add(itemDelta);
		deltaList.getDelta().add(delta);
		
		ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
		
		assertSuccess(deltaOpList);
	}
	
	protected void assertAuditLoginFailed(LogfileTestTailer tailer, String expectedMessage) {
        tailer.assertAudit(1);
        String auditMessage = tailer.getAuditMessages().get(0);
        assertTrue("Audit: not login: "+auditMessage, auditMessage.contains("et=CREATE_SESSION"));
        assertTrue("Audit: not failure: "+auditMessage, auditMessage.contains("o=FATAL_ERROR"));
        assertTrue("Audit: wrong message: "+auditMessage, auditMessage.contains(expectedMessage));
	}
	
	protected void displayAudit(LogfileTestTailer tailer) {
        display("Audit:");
        for (String auditMessage: tailer.getAuditMessages()) {
        	display(auditMessage);
        }
	}
	
	protected void assertAuditIds(LogfileTestTailer tailer) {
		List<String> eids = new ArrayList<String>();
		String sid = null;
		String tid = null;
		for (String auditMessage: tailer.getAuditMessages()) {
        	
			String msgEid = getAuditEid(auditMessage);
        	if (eids.contains(msgEid)) {
        		AssertJUnit.fail("Event ID "+msgEid+" is not unique: "+auditMessage);
        	}
        	
        	String msgTid = getAuditTid(auditMessage);
        	if (msgTid == null) {
        		AssertJUnit.fail("Audit message without task ID: "+auditMessage);
        	}
        	if (tid == null) {
        		tid = msgTid;
        	} else {
        		assertEquals("Unmatched audit task ID: "+auditMessage, tid, msgTid);
        	}
        	
        	String msgSid = getAuditSid(auditMessage);
        	if (msgSid != null) {
	        	if (sid == null) {
	        		sid = msgSid;
	        	} else {
	        		assertEquals("Unmatched audit session ID: "+auditMessage, sid, msgSid);
	        	}
        	}
        }
	}
	
	protected String getAuditEid(String auditMessage) {
		return getAuditId(auditMessage, PATTERN_AUDIT_EVENT_ID);
	}
	
	protected String getAuditSid(String auditMessage) {
		return getAuditId(auditMessage, PATTERN_AUDIT_SESSION_ID);
	}
	
	protected String getAuditTid(String auditMessage) {
		return getAuditId(auditMessage, PATTERN_AUDIT_TASK_ID);
	}

	protected String getAuditId(String auditMessage, Pattern pattern) {
		Matcher matcher = pattern.matcher(auditMessage);
		if (!matcher.matches()) {
			return null;
		}
		String match = matcher.group(1);
		if (match == null || match.equals("null")) {
			return null;
		}
		return match;
	}

	protected void assertAuditLoginLogout(LogfileTestTailer tailer) {
		List<String> msgs = tailer.getAuditMessages();
        String firstMsg = msgs.get(0);
        assertTrue("Audit: first: not login: "+firstMsg, firstMsg.contains("et=CREATE_SESSION"));
        assertTrue("Audit: first: not success: "+firstMsg, firstMsg.contains("o=SUCCESS"));
        
        String lastMsg = msgs.get(msgs.size() - 1);
        assertTrue("Audit: last: not logout: "+lastMsg, lastMsg.contains("et=TERMINATE_SESSION"));
        assertTrue("Audit: last: not success: "+lastMsg, lastMsg.contains("o=SUCCESS"));
	}
	
	protected void assertAuditOperation(LogfileTestTailer tailer, String expectedEventType) {
		List<String> msgs = tailer.getAuditMessages();
		String reqMsg = msgs.get(1);
        assertTrue("Audit: request: wrong operation: "+reqMsg, reqMsg.contains("et="+expectedEventType));
        assertTrue("Audit: request: not request: "+reqMsg, reqMsg.contains("es=REQUEST"));
        
        String execMsg = msgs.get(2);
        assertTrue("Audit: exec: wrong operation: "+execMsg, execMsg.contains("et="+expectedEventType));
        assertTrue("Audit: exec: not execution: "+execMsg, execMsg.contains("es=EXECUTION"));
        assertTrue("Audit: exec: not success: "+execMsg, execMsg.contains("o=SUCCESS"));
	}


	
    @Test
    public void test000SanityAndCleanup() throws Exception {
    	final String TEST_NAME = "test000SanityAndCleanup";
    	displayTestTitle(TEST_NAME);
        modelPort = createModelPort();
        
        configurationType = getConfiguration();
        checkAuditEnabled(configurationType);
        
        
        cleanRepository();
    }

}
