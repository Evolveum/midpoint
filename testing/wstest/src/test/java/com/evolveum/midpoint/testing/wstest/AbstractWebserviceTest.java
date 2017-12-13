/*
 * Copyright (c) 2013-2017 Evolveum
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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
    public static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();
    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
    public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";
    
    public static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
 	public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
 	public static final String USER_JACK_USERNAME = "jack";
 	public static final String USER_JACK_GIVEN_NAME = "Jack";
 	public static final String USER_JACK_FAMILY_NAME = "Sparrow";
    
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

 	// WS, reader and adder authorization
 	public static final File USER_DARTHADDER_FILE = new File(COMMON_DIR, "user-darthadder.xml");
 	public static final String USER_DARTHADDER_OID = "1696229e-d90a-11e4-9ce6-001e8c717e5b";
 	public static final String USER_DARTHADDER_USERNAME = "darthadder";
 	public static final String USER_DARTHADDER_PASSWORD = "iamyouruncle";
 	
 	// Authorizations, but no password
 	public static final File USER_NOPASSWORD_FILE = new File(COMMON_DIR, "user-nopassword.xml");
 	public static final String USER_NOPASSWORD_USERNAME = "nopassword";

 	public static final File ROLE_WS_FILE = new File(COMMON_DIR, "role-ws.xml");
 	
	public static final File ROLE_READER_FILE = new File(COMMON_DIR, "role-reader.xml");
	public static final String ROLE_READER_OID = "eb243068-d48d-11e4-a83a-001e8c717e5b";
	
	public static final File ROLE_ADDER_FILE = new File(COMMON_DIR, "role-adder.xml");
	
	public static final File ROLE_MODIFIER_FILE = new File(COMMON_DIR, "role-modifier.xml");
	public static final String ROLE_MODIFIER_OID = "82005ae4-d90b-11e4-bdcc-001e8c717e5b";
	
	public static final File ROLE_WHATEVER_FILE = new File(COMMON_DIR, "role-whatever.xml");
	public static final String ROLE_WHATEVER_OID = "7385820e-5cb3-11e7-96a3-1ba6f1dc6281";
	
	public static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj.xml");
	public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	public static final String CONNECTOR_LDAP_TYPE = "com.evolveum.polygon.connector.ldap.LdapConnector";
 	
	protected static final Pattern PATTERN_AUDIT_EVENT_ID = Pattern.compile(".*\\seid=([^,]+),\\s.*");
	protected static final Pattern PATTERN_AUDIT_SESSION_ID = Pattern.compile(".*\\ssid=([^,]+),\\s.*");
	protected static final Pattern PATTERN_AUDIT_TASK_ID = Pattern.compile(".*\\stid=([^,]+),\\s.*");
	
    
    public static final String NS_COMMON = ModelClientUtil.NS_COMMON;
    public static final String NS_TYPES = ModelClientUtil.NS_TYPES;
    public static final String NS_RI = ModelClientUtil.NS_RI;
    public static final String NS_ICFS = ModelClientUtil.NS_ICFS;
    protected static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");
    protected static final String CHANNEL_WS = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#webService";
    
    public static final QName ATTR_ICF_NAME_NAME = new QName(NS_ICFS, "name");

    protected static final QName COMMON_PATH = new QName(NS_COMMON, "path");
    protected static final QName COMMON_VALUE = new QName(NS_COMMON, "value");

    protected static DocumentBuilder domDocumentBuilder;
    protected static ModelPortType modelPort;
    protected static SystemConfigurationType configurationType;

	private static final String SERVER_LOG_FILE_SUFFIX = "log/midpoint.log";
	private static final String AUDIT_LOGGER_NAME = "com.evolveum.midpoint.audit.log";
	
	private File serverLogFile = null;
	
    @BeforeClass
    public void beforeTests() throws Exception {
    	displayTestTitle("beforeTests");
    	startResources();
    }
    
	/**
     * Takes care of system initialization. Need to be done before any tests are to be run.
     * */
    protected void startResources() throws Exception {
    }
    
    @AfterClass
    public void afterTests() throws Exception {
    	displayTestTitle("afterTests");
        modelPort = createModelPort();
        cleanRepository();
        stopResources();
        LOGGER.info("WebService test suite finished.");
    }
    
    protected void stopResources() throws Exception {
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

    	String endpoint = ENDPOINT;
    	if (System.getProperty("midpoint.endpoint") != null) {
    		endpoint = System.getProperty("midpoint.endpoint");
    	}
        LOGGER.info("Creating model client endpoint: {} , username={}, password={}", 
        		new Object[] {endpoint, username, password});

        ModelService modelService = new ModelService();
        ModelPortType modelPort = modelService.getModelPort();
        BindingProvider bp = (BindingProvider)modelPort;
        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

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

	private File getServerLogFile() {
		if (serverLogFile == null) {
			if (System.getProperty("midpoint.serverLogFile") != null) {
				serverLogFile = new File(System.getProperty("midpoint.serverLogFile"));
	    	} else if (System.getenv("MIDPOINT_HOME") != null) {
	    		serverLogFile = new File(System.getenv("MIDPOINT_HOME"), SERVER_LOG_FILE_SUFFIX);
	    	} else if (System.getProperty("midpoint.home") != null) {
	    		serverLogFile = new File(System.getProperty("midpoint.home"), SERVER_LOG_FILE_SUFFIX);
	    	} else {
	    		throw new IllegalStateException("Cannot determine server log file");
	    	}
		}
		return serverLogFile;
	}

    
    /**
     * returns URI of type passed as argument
     * */
    protected static String getTypeUri(Class<? extends ObjectType> type){
    	return ModelClientUtil.getTypeUri(type);
    }

    protected static QName getTypeQName(Class<? extends ObjectType> type){
    	return ModelClientUtil.getTypeQName(type);
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
    
    protected <O extends ObjectType> void deleteObject(Class<O> type, String oid) throws FaultMessage {
    	deleteObject(type, oid, null);
    }
    
	protected <O extends ObjectType> void deleteObject(Class<O> type, String oid, ModelExecuteOptionsType options) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(type));
    	delta.setChangeType(ChangeTypeType.DELETE);
    	delta.setOid(oid);
		deltaList.getDelta().add(delta);
		ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, options);
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
	
	protected <F extends FocusType> String getSingleLinkOid(F focus) {
		List<ObjectReferenceType> linkRefs = focus.getLinkRef();
		assertEquals("Unexpected number of links for "+focus, 1, linkRefs.size());
		return linkRefs.get(0).getOid();
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

    protected void assertSuccess(Holder<OperationResultType> resultHolder) {
		assertSuccess(resultHolder.value);
	}
    
	protected void assertSuccess(OperationResultType result) {
		assertEquals("Operation "+result.getOperation()+" failed:"+result.getStatus()+": " + result.getMessage(),
				OperationResultStatusType.SUCCESS, result.getStatus());
		// TODO: look inside
	}
	
	protected <F extends FaultType> void assertFaultMessage(FaultMessage fault, Class<F> expectedFaultInfoClass, String expectedMessage) {
    	FaultType faultInfo = fault.getFaultInfo();
    	assertNotNull("No fault info in "+fault);
    	if (expectedFaultInfoClass != null && !expectedFaultInfoClass.isAssignableFrom(faultInfo.getClass())) {
    		AssertJUnit.fail("Expected that faultInfo will be of type "+expectedFaultInfoClass+", but it was "+faultInfo.getClass());
    	}
    	if (expectedMessage != null) {
    		assertTrue("Wrong message in fault: "+fault.getMessage(), fault.getMessage().contains(expectedMessage));
    		assertTrue("Wrong message in fault info: "+faultInfo.getMessage(), faultInfo.getMessage().contains(expectedMessage));
    	}
    	OperationResultType result = faultInfo.getOperationResult();
    	assertNotNull("No result in faultInfo in "+fault, result);
    	assertEquals("Expected that resut in FaultInfo will be fatal error, but it was "+result.getStatus(),
    			OperationResultStatusType.FATAL_ERROR, result.getStatus());
	}
	
	
	protected void assertSoapSecurityFault(SOAPFaultException e, String expectedCode, String expectedMessage) {
		// CXF by default replaces the real error with "safe" values
		assertSoapFault(e, "SecurityError", "security error");
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
		TestUtil.displayTestTitle(testName);
	}
	
	protected void displayWhen(String testName) {		
		TestUtil.displayWhen(testName);
	}
	
	protected void displayThen(String testName) {		
		TestUtil.displayThen(testName);
	}

	protected void display(String msg) {
		System.out.println(msg);
		LOGGER.info("{}", msg);
	}
	
	protected <O extends ObjectType> void display(O object) throws JAXBException {
		String xmlString = ModelClientUtil.marshallToSting(object);
		System.out.println(xmlString);
		LOGGER.info("{}", xmlString);
	}
	
	protected void display(OperationResultType result) throws JAXBException {
		String xmlString = ModelClientUtil.marshallToSting(new QName(NS_COMMON,"result"), result, true);
		System.out.println("Result:");
		System.out.println(xmlString);
		LOGGER.info("Result:\n{}", xmlString);
	}

	protected LogfileTestTailer createLogTailer() throws IOException {
		return new LogfileTestTailer(getServerLogFile(), AUDIT_LOGGER_NAME, true);
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
        
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(SystemConfigurationType.class, 
        		SystemObjectsType.SYSTEM_CONFIGURATION.value(), "logging", ModificationTypeType.REPLACE, loggingConfig);
		
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
		assertAuditOperation(tailer, expectedEventType, OperationResultStatusType.SUCCESS, null);
	}
	
	protected void assertAuditOperation(LogfileTestTailer tailer, String expectedEventType, 
			OperationResultStatusType expectedStatus, String expectedMessage) {
		List<String> msgs = tailer.getAuditMessages();
		String reqMsg = msgs.get(1);
        assertTrue("Audit: request: wrong operation: "+reqMsg, reqMsg.contains("et="+expectedEventType));
        assertTrue("Audit: request: not request: "+reqMsg, reqMsg.contains("es=REQUEST"));
        
        String execMsg = msgs.get(2);
        assertTrue("Audit: exec: wrong operation: "+execMsg, execMsg.contains("et="+expectedEventType));
        assertTrue("Audit: exec: not execution: "+execMsg, execMsg.contains("es=EXECUTION"));
        assertTrue("Audit: exec: wrong status, expected '"+expectedStatus+"': "+execMsg, execMsg.contains("o="+expectedStatus));
        if (expectedMessage != null) {
        	assertTrue("Audit: exec: wrong message, expected '"+expectedMessage+"': "+execMsg, execMsg.matches(".*m=.*"+expectedMessage+".*"));
        }
	}

	protected <O extends ObjectType> void assertModifyMetadata(O object, String actorOid, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        MetadataType metadata = object.getMetadata();
        assertEquals("Wrong metadata modifierRef in "+object, actorOid, metadata.getModifierRef().getOid());
        assertEquals("Wrong metadata modify channel in "+object, CHANNEL_WS, metadata.getModifyChannel());
        TestUtil.assertBetween("Wrong password modifyTimestamp in "+object, startTs, endTs, metadata.getModifyTimestamp());		
	}

	protected <O extends ObjectType> void assertCreateMetadata(O object, String actorOid, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        MetadataType metadata = object.getMetadata();
        assertEquals("Wrong metadata creatorRef in "+object, actorOid, metadata.getCreatorRef().getOid());
        assertEquals("Wrong metadata create channel in "+object, CHANNEL_WS, metadata.getCreateChannel());
        TestUtil.assertBetween("Wrong createTimestamp in "+object, startTs, endTs, metadata.getCreateTimestamp());		
	}
	
	protected void assertPasswordModifyMetadata(UserType user, String actorOid, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        MetadataType passwordMetadata = user.getCredentials().getPassword().getMetadata();
        assertEquals("Wrong password metadata modifierRef", actorOid, passwordMetadata.getModifierRef().getOid());
        assertEquals("Wrong password metadata modify channel", CHANNEL_WS, passwordMetadata.getModifyChannel());
        TestUtil.assertBetween("Wrong password modifyTimestamp", startTs, endTs, passwordMetadata.getModifyTimestamp());		
	}

	protected void assertPasswordCreateMetadata(UserType user, String actorOid, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        MetadataType passwordMetadata = user.getCredentials().getPassword().getMetadata();
        assertEquals("Wrong password metadata creatorRef", actorOid, passwordMetadata.getCreatorRef().getOid());
        assertEquals("Wrong password metadata create channel", CHANNEL_WS, passwordMetadata.getCreateChannel());
        TestUtil.assertBetween("Wrong password createTimestamp", startTs, endTs, passwordMetadata.getCreateTimestamp());		
	}
	
	protected void assertAttribute(ShadowType shadow, String attrName, String attrVal) {
		assertAttribute(shadow, new QName(NS_RI, attrName), attrVal);
	}
	
	protected void assertAttribute(ShadowType shadow, QName attrName, String attrVal) {
		ShadowAttributesType attributes = shadow.getAttributes();
		for (Object any: attributes.getAny()) {
			if (any instanceof Element) {
				Element element = (Element)any;
				if (DOMUtil.getQName(element).equals(attrName)) {
					assertEquals("Wrong attribute "+attrName+" in shadow "+ModelClientUtil.toString(shadow), attrVal, element.getTextContent());
				}
			} else if (any instanceof JAXBElement<?>) {
				JAXBElement<?> jaxbElement = (JAXBElement<?>)any;
				if (jaxbElement.getName().equals(attrName)) {
					assertEquals("Wrong attribute "+attrName+" in shadow "+ModelClientUtil.toString(shadow), attrVal, jaxbElement.getValue().toString());
				}
			} else {
				AssertJUnit.fail("Unexpected thing "+any+" in shadow attributes");
			}
		}
	}
	
	protected void assertNoAttribute(ShadowType shadow, String attrName) {
		assertNoAttribute(shadow, new QName(NS_RI, attrName));
	}
	
	protected void assertNoAttribute(ShadowType shadow, QName attrName) {
		ShadowAttributesType attributes = shadow.getAttributes();
		for (Object any: attributes.getAny()) {
			if (any instanceof Element) {
				Element element = (Element)any;
				if (DOMUtil.getQName(element).equals(attrName)) {
					AssertJUnit.fail("Unexpected attribute "+attrName+" in shadow "+ModelClientUtil.toString(shadow)+": "+element.getTextContent());
				}
			} else if (any instanceof JAXBElement<?>) {
				JAXBElement<?> jaxbElement = (JAXBElement<?>)any;
				if (jaxbElement.getName().equals(attrName)) {
					AssertJUnit.fail("Unexpected attribute "+attrName+" in shadow "+ModelClientUtil.toString(shadow)+": "+jaxbElement.getValue());
				}
			} else {
				AssertJUnit.fail("Unexpected thing "+any+" in shadow attributes");
			}
		}
		
	}
	
    /**
     *  Clean the repository after tests. Preserves user administrator
     * */
    protected void cleanRepository() throws FaultMessage {
    	cleanObjects(UserType.class, false, SystemObjectsType.USER_ADMINISTRATOR.value());
    	cleanObjects(RoleType.class, false, SystemObjectsType.ROLE_SUPERUSER.value(), SystemObjectsType.ROLE_END_USER.value());
    	cleanObjects(ResourceType.class, false);
    	cleanObjects(ShadowType.class, true);
    }
    
    private <O extends ObjectType> void cleanObjects(Class<O> type, boolean raw, String... protectedOids) throws FaultMessage {
    	Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();

        SelectorQualifiedGetOptionsType rootOpts = null;
        ModelExecuteOptionsType execOpts = null;
        if (raw) {
        	rootOpts = ModelClientUtil.createRootGetOptions(ModelClientUtil.createRawGetOption());
        	execOpts = ModelClientUtil.createRawExecuteOption();
        }
        
        modelPort.searchObjects(getTypeQName(type), null, rootOpts, objectListHolder, resultHolder);

        List<String> protectedOidList = Arrays.asList(protectedOids);
        ObjectListType objectList = objectListHolder.value;
        for (ObjectType object: objectList.getObject()) {
        	if (!protectedOidList.contains(object.getOid())) {
        		display("Deleting "+type.getSimpleName()+" "+ModelClientUtil.toString(object));
				deleteObject(type, object.getOid(), execOpts);
        	}
        }
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
