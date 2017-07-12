/*
 * Copyright (c) 2013-2016 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.PolicyViolationFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  This class has several tests, that aims on security
 *  of webservice interface provided by midpoint
 *
 *  @author Radovan Semancik
 *  @author Erik Suta
 * */

@ContextConfiguration(locations = {"classpath:ctx-wstest-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestWSSecurity extends AbstractWebserviceTest {

	private static final String USER_DARTHADDER_PASSWORD_NEW1 = "iamyourgreatgranduncle";
	private static final String USER_DARTHADDER_PASSWORD_NEW2 = "iamyourdog";
	private XMLGregorianCalendar dartAdderLastPasswordChangeStartTs;
	private XMLGregorianCalendar dartAdderLastPasswordChangeEndTs;
	private PasswordType dartAdderLastPassword;

    @Test
    public void test100GetConfigNoSecurity() throws Exception {
    	final String TEST_NAME = "test100GetConfigNoSecurity";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(null, null);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "InvalidSecurity", "<wsse:Security> header");
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "<wsse:Security> header");
    }

	@Test
    public void test101GetConfigWrongPasswordDigest() throws Exception {
    	final String TEST_NAME = "test101GetConfigWrongPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, "wrongAdministratorPassword", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "could not be authenticated or authorized");
    }

	@Test
    public void test102GetConfigWrongPasswordText() throws Exception {
    	final String TEST_NAME = "test102GetConfigWrongPasswordText";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, "wrongAdministratorPassword", WSConstants.PW_TEXT);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
    }

    @Test
    public void test103GetConfigEmptyPasswordDigest() throws Exception {
    	final String TEST_NAME = "test103GetConfigEmptyPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, "", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "could not be authenticated or authorized");
        
    }
    
    @Test
    public void test104GetConfigEmptyPasswordText() throws Exception {
    	final String TEST_NAME = "test104GetConfigEmptyPasswordText";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, "", WSConstants.PW_TEXT);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "could not be authenticated or authorized");
    }
    
    @Test
    public void test105GetConfigWrongUsernameDigest() throws Exception {
    	final String TEST_NAME = "test105GetConfigWrongUsernameDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort("admin", USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no user");
    }
    
    @Test
    public void test106GetConfigWrongUsernameText() throws Exception {
    	final String TEST_NAME = "test106GetConfigWrongUsernameText";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort("admin", USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_TEXT);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no user");
    }
    
    @Test
    public void test107GetConfigBlankUsernameDigest() throws Exception {
    	final String TEST_NAME = "test107GetConfigBlankUsernameDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(" ", USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no username");
    }
    
    @Test
    public void test108GetConfigBlankUsernameText() throws Exception {
    	final String TEST_NAME = "test108GetConfigBlankUsernameText";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(" ", USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_TEXT);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no username");
    }
    
    @Test
    public void test110GetConfigGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test110GetConfigGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        
        tailer.tail();
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        tailer.assertAudit(2);
    }
    
    @Test
    public void test111GetConfigGoodPasswordText() throws Exception {
    	final String TEST_NAME = "test111GetConfigGoodPasswordText";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_TEXT);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        
        // THEN
        assertSuccess(resultHolder);
        		
        tailer.tail();
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        tailer.assertAudit(2);
    }
    
    @Test
    public void test120AddUserNobodyAsAdministrator() throws Exception {
    	final String TEST_NAME = "test120AddUserNobodyAsAdministrator";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_DIGEST);

        UserType userNobody = ModelClientUtil.unmarshallFile(USER_NOBODY_FILE);
        
        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        // WHEN
        String userNobodyOid = addObject(userNobody);
     
        // THEN
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        // GET user
        UserType userNobodyAfter = getObject(UserType.class, userNobodyOid);
        display(userNobodyAfter);
        assertUser(userNobodyAfter, userNobodyOid, USER_NOBODY_USERNAME, USER_NOBODY_GIVEN_NAME, USER_NOBODY_FAMILY_NAME);
        
        assertPasswordCreateMetadata(userNobodyAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
    }
    
    @Test
    public void test121GetConfigAsNobodyWrongPasswordDigest() throws Exception {
    	final String TEST_NAME = "test121GetConfigAsNobodyWrongPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_NOBODY_USERNAME, "wrongNobodyPassword", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no authorizations");
    }
    
    @Test
    public void test122GetConfigAsNobodyEmptyPasswordDigest() throws Exception {
    	final String TEST_NAME = "test103GetConfigEmptyPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_NOBODY_USERNAME, "", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no authorizations");
    }
    
    @Test
    public void test123GetConfigAsNobodyGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test123GetConfigAsNobodyGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no authorizations");
    }
    
    @Test
    public void test130AddRolesAndUsersAsAdministrator() throws Exception {
    	final String TEST_NAME = "test130AddRolesAndUsersAsAdministrator";
    	displayTestTitle(TEST_NAME);
    	
        modelPort = createModelPort(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD, WSConstants.PW_DIGEST);

        RoleType role = ModelClientUtil.unmarshallFile(ROLE_WS_FILE);
        addObject(role);

        role = ModelClientUtil.unmarshallFile(ROLE_READER_FILE);
        addObject(role);
        
        role = ModelClientUtil.unmarshallFile(ROLE_ADDER_FILE);
        addObject(role);
        
        role = ModelClientUtil.unmarshallFile(ROLE_WHATEVER_FILE);
        addObject(role);

        UserType user = ModelClientUtil.unmarshallFile(USER_CYCLOPS_FILE);
        String userCyclopsOid = addObject(user);
        
        user = ModelClientUtil.unmarshallFile(USER_SOMEBODY_FILE);
        addObject(user);
        
        user = ModelClientUtil.unmarshallFile(USER_DARTHADDER_FILE);
        addObject(user);
        
        user = ModelClientUtil.unmarshallFile(USER_NOPASSWORD_FILE);
        addObject(user);
        
        // GET user
        UserType userAfter = getObject(UserType.class, userCyclopsOid);
        assertUser(userAfter, userCyclopsOid, USER_CYCLOPS_USERNAME);
        
        assertObjectCount(UserType.class, 6);
        assertObjectCount(RoleType.class, 6);
    }

	@Test
    public void test131GetConfigAsCyclopsGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test131GetConfigAsCyclopsGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
        modelPort = createModelPort(USER_CYCLOPS_USERNAME, USER_CYCLOPS_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        LogfileTestTailer tailer = createLogTailer();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginLogout(tailer);
    }
	
	@Test
    public void test132GetConfigAsSomebodyGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test132GetConfigAsSomebodyGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_SOMEBODY_USERNAME, USER_SOMEBODY_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        /// WHEN
        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        
        // THEN
        assertSuccess(resultHolder);
        
        tailer.tail();
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        tailer.assertAudit(2);
    }
	
	@Test
    public void test133ModifyConfigAsSomebody() throws Exception {
    	final String TEST_NAME = "test133ModifyConfigAsSomebody";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c4e998e6-d903-11e4-9aaf-001e8c717e5b"); // fake
        
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(SystemConfigurationType.class, 
        		SystemObjectsType.SYSTEM_CONFIGURATION.value(), "globalSecurityPolicyRef", ModificationTypeType.REPLACE, 
        		ref);
        
        try {
    		// WHEN
            modelPort.executeChanges(deltaList, null);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT", OperationResultStatusType.FATAL_ERROR, "not authorized");
        tailer.assertAudit(4);
    }
	
	@Test
    public void test134GetConfigAsDarthAdderGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test134GetConfigAsDarthAdderGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        /// WHEN
        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        
        // THEN
        assertSuccess(resultHolder);
        
        tailer.tail();
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        tailer.assertAudit(2);
    }
	
	@Test
    public void test135ModifyConfigAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test135ModifyConfigAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c4e998e6-d903-11e4-9aaf-001e8c717e5b"); // fake
        
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(SystemConfigurationType.class, 
        		SystemObjectsType.SYSTEM_CONFIGURATION.value(), "globalSecurityPolicyRef", ModificationTypeType.REPLACE, 
        		ref);
        
        try {
    		// WHEN
            modelPort.executeChanges(deltaList, null);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT", OperationResultStatusType.FATAL_ERROR, "not authorized");
        tailer.assertAudit(4);
    }
	
	@Test
    public void test136AddRoleAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test136AddRoleAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        
        RoleType role = ModelClientUtil.unmarshallFile(ROLE_MODIFIER_FILE);
        
        /// WHEN
        addObject(role);
        
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        assertObjectCount(UserType.class, 6);
        assertObjectCount(RoleType.class, 7);
    }
	
	@Test
    public void test135AssignRoleAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test135ModifyConfigAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        
        ObjectDeltaListType deltaList = ModelClientUtil.createAssignDeltaList(UserType.class, USER_DARTHADDER_OID, 
        		RoleType.class, ROLE_WHATEVER_OID);
        
        try {
    		// WHEN
        	displayWhen(TEST_NAME);
            modelPort.executeChanges(deltaList, null);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	displayThen(TEST_NAME);
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT", OperationResultStatusType.FATAL_ERROR, "not authorized");
        tailer.assertAudit(4);
        
    }
	
	@Test
    public void test140AssignRoleToDarthAdderAsAdministrator() throws Exception {
    	final String TEST_NAME = "test140AssignRoleToDarthAdderAsAdministrator";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
    	modelPort = createModelPort();
        
        ObjectDeltaListType deltaList = ModelClientUtil.createAssignDeltaList(UserType.class, USER_DARTHADDER_OID, 
        		RoleType.class, ROLE_MODIFIER_OID);
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);
    }
	
	@Test
    public void test141ModifyTitleAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test141ModifyTitleAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD, WSConstants.PW_DIGEST);

        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"title", ModificationTypeType.REPLACE, ModelClientUtil.createPolyStringType("Dark Lord"));
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);
        
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        PolyStringType title = user.getTitle();
        assertEquals("Wrong title", "Dark Lord", ModelClientUtil.getOrig(title));
    }
    
	@Test
	public void test142DisableHimselfAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test142DisableHimselfAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"activation/administrativeStatus", ModificationTypeType.REPLACE, ActivationStatusType.DISABLED);
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);
        
        modelPort = createModelPort();
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        assertEquals("Wrong administrative status in "+ModelClientUtil.toString(user), ActivationStatusType.DISABLED, user.getActivation().getAdministrativeStatus());
    }
	
	@Test
    public void test143GetConfigAsDarthAdderGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test143GetConfigAsDarthAdderGoodPasswordDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        try {
	        /// WHEN
	        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
	        		null, objectHolder, resultHolder);
	        
	        AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        assertAuditLoginFailed(tailer, "user disabled");
    }
    
	@Test
    public void test145ModifyConfigAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test145ModifyConfigAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c4e998e6-d903-11e4-9aaf-001e8c717e5b"); // fake
        
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(SystemConfigurationType.class, 
        		SystemObjectsType.SYSTEM_CONFIGURATION.value(), "globalSecurityPolicyRef", ModificationTypeType.REPLACE, 
        		ref);
        
        try {
    		// WHEN
            modelPort.executeChanges(deltaList, null);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        assertAuditLoginFailed(tailer, "user disabled");
    }
	
	@Test
	public void test146EnableDarthAdder() throws Exception {
    	final String TEST_NAME = "test146EnableDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

    	modelPort = createModelPort();
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"activation/administrativeStatus", ModificationTypeType.REPLACE, ActivationStatusType.ENABLED);
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);
        
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD, WSConstants.PW_DIGEST);
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        assertEquals("Wrong administrative status in "+ModelClientUtil.toString(user), ActivationStatusType.ENABLED, user.getActivation().getAdministrativeStatus());
    }
	
	@Test
    public void test150GetConfigNoPasswordWrongDigest() throws Exception {
    	final String TEST_NAME = "test150GetConfigNoPasswordWrongDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_NOPASSWORD_USERNAME, "wrongPassword", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no credentials in user");
    }
	
	@Test
    public void test152GetConfigNoPasswordEmptyDigest() throws Exception {
    	final String TEST_NAME = "test152GetConfigNoPasswordEmptyDigest";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_NOPASSWORD_USERNAME, " ", WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        // WHEN
        try {
        	modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
        		null, objectHolder, resultHolder);
        	
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "no credentials in user");
    }
	
	@Test
	public void test160ChangeDarthAdderPasswordSatisfiesPolicyShortcut() throws Exception {
    	final String TEST_NAME = "test160ChangeDarthAdderPasswordSatisfiesPolicyShortcut";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
    	modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD, WSConstants.PW_DIGEST);

    	ProtectedStringType protectedString = new ProtectedStringType();
    	protectedString.getContent().add(USER_DARTHADDER_PASSWORD_NEW1);
    	
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"credentials/password/value", ModificationTypeType.REPLACE, protectedString);
        
        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);     
        
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD_NEW1, WSConstants.PW_DIGEST);
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        
        assertPasswordModifyMetadata(user, USER_DARTHADDER_OID, startTs, endTs);
    }

	@Test
	public void test161ChangeDarthAdderPasswordSatisfiesPolicyStrict() throws Exception {
    	final String TEST_NAME = "test160ChangeDarthAdderPasswordSatisfiesPolicyShortcut";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

    	ProtectedStringType protectedString = ModelClientUtil.createProtectedString(USER_DARTHADDER_PASSWORD_NEW2);
    	
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"credentials/password/value", ModificationTypeType.REPLACE, protectedString);
        
        dartAdderLastPasswordChangeStartTs = TestUtil.currentTime();
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        dartAdderLastPasswordChangeEndTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);     
        
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD_NEW2, WSConstants.PW_DIGEST);
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        
        dartAdderLastPassword = user.getCredentials().getPassword();
        assertNotNull("No password for DarthAdder", dartAdderLastPassword);
        assertPasswordModifyMetadata(user, USER_DARTHADDER_OID, dartAdderLastPasswordChangeStartTs, dartAdderLastPasswordChangeEndTs);
    }
	
	@Test
	public void test162ChangeDarthAdderPasswordViolatesPolicy() throws Exception {
    	final String TEST_NAME = "test162ChangeDarthAdderPasswordViolatesPolicy";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

    	ProtectedStringType protectedString = ModelClientUtil.createProtectedString("x");
    	
        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"credentials/password/value", ModificationTypeType.REPLACE, protectedString);
        
        try {
	        // WHEN
	        modelPort.executeChanges(deltaList, null);
        	        
        	AssertJUnit.fail("Unexpected success");
        	
        } catch (FaultMessage e) {
        	assertFaultMessage(e, PolicyViolationFaultType.class, "password does not satisfy");        	
        }
        
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT", OperationResultStatusType.FATAL_ERROR, "password does not satisfy");
        tailer.assertAudit(4);
        
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        
        PasswordType dartAdderPassword = user.getCredentials().getPassword();
        assertEquals("Password of DarthAdder has changed", 
        		ModelClientUtil.marshallToSting(new QName("http://whatever/","fake"), dartAdderLastPassword, false), 
        		ModelClientUtil.marshallToSting(new QName("http://whatever/","fake"), dartAdderPassword, false));
        assertPasswordModifyMetadata(user, USER_DARTHADDER_OID, dartAdderLastPasswordChangeStartTs, dartAdderLastPasswordChangeEndTs);
    }
	
	
	@Test
	public void test165DarthAdderDeleteOwnPassword() throws Exception {
    	final String TEST_NAME = "test165DarthAdderDeleteOwnPassword";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        ObjectDeltaListType deltaList = ModelClientUtil.createModificationDeltaList(UserType.class, USER_DARTHADDER_OID,
        		"credentials/password", ModificationTypeType.REPLACE); // no values here
        
        // WHEN
        ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, null);
        	        
        // THEN
        assertSuccess(deltaOpList);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);     
        
        modelPort = createModelPort();
        UserType user = getObject(UserType.class, USER_DARTHADDER_OID);
        display(user);
        assertNull("Credentials sneaked in: "+user.getCredentials(), user.getCredentials());        
    }
	
	/**
	 * Darth Adder has no password.
	 */
	@Test
    public void test166GetConfigAsDarthAdder() throws Exception {
    	final String TEST_NAME = "test166GetConfigAsDarthAdder";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
        modelPort = createModelPort(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD_NEW2, WSConstants.PW_DIGEST);

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        
        try {
	        /// WHEN
	        modelPort.getObject(getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
	        		null, objectHolder, resultHolder);
	        
	        AssertJUnit.fail("Unexpected success");
        	
        } catch (SOAPFaultException e) {
        	assertSoapSecurityFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        // THEN
        tailer.tail();
        assertAuditLoginFailed(tailer, "no credentials in user");
    }

}

