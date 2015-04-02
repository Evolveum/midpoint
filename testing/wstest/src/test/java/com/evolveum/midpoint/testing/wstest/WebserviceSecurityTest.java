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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  This class has several tests, that aims on security
 *  of webservice interface provided by midpoint
 *
 *  @author Erik Suta
 * */

@ContextConfiguration(locations = {"classpath:ctx-wstest-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class WebserviceSecurityTest extends AbstractWebserviceTest {

    private static final String PACKAGE_API_TYPES_2 = "com.evolveum.midpoint.xml.ns._public.common.api_types_3";
    private static final String PACKAGE_COMMON_2A = "com.evolveum.midpoint.xml.ns._public.common.common_3";

    private static final String SECURITY_DIR_NAME = "src/test/resources/security/";

    private static final String USER_TEST_2_FILENAME = "user_test2.xml";
    private static final String USER_TEST_2_CLEAN_PASS_FILENAME = "user_test2_clean_pass.xml";

    private static final String SYSTEM_CONFIG_FILENAME = "system_config_without_pass_policy.xml";
    private static final String SYSTEM_CONFIG_NORMAL_FILENAME = "system_config_normal.xml";

    private static final String USER_TEST_5_FILENAME = "user_test5.xml";
    private static final String USER_TEST_6_FILENAME = "user_test6.xml";

    private static final String test_user_oid_2 = "c0c010c0-d34d-b33f-f00d-111111111112";
    private static final String system_config_oid = "00000000-0000-0000-0000-000000000001";

    private static final String UNIVERSAL_USER_PASSWORD = "ineedmorepower";

    
    /*===============================================================================================================*/
    /*                                                      TESTS                                                    */
    /*===============================================================================================================*/


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
        	assertSoapFault(e, "InvalidSecurity", "<wsse:Security> header");        	
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "No user");
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "No user");
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "No username");
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "No username");
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
        
        // WHEN
        String userNobodyOid = addObject(userNobody);
     
        // THEN
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        // GET user
        UserType userNobodyAfter = getObject(UserType.class, userNobodyOid);
        assertUser(userNobodyAfter, userNobodyOid, USER_NOBODY_USERNAME, USER_NOBODY_GIVEN_NAME, USER_NOBODY_FAMILY_NAME);
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "could not be authenticated or authorized");
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "could not be authenticated or authorized");
    }
    
    @Test
    public void test122GetConfigAsNobodyGoodPasswordDigest() throws Exception {
    	final String TEST_NAME = "test122GetConfigAsNobodyGoodPasswordDigest";
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
        	assertSoapFault(e, "FailedAuthentication", "could not be authenticated or authorized");        	
        }
        
        tailer.tail();
        assertAuditLoginFailed(tailer, "Not authorized");
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

        UserType user = ModelClientUtil.unmarshallFile(USER_CYCLOPS_FILE);
        String userCyclopsOid = addObject(user);
        
        user = ModelClientUtil.unmarshallFile(USER_SOMEBODY_FILE);
        addObject(user);
        
        // GET user
        UserType userAfter = getObject(UserType.class, userCyclopsOid);
        assertUser(userAfter, userCyclopsOid, USER_CYCLOPS_USERNAME);
        
        assertObjectCount(UserType.class, 4);
        assertObjectCount(RoleType.class, 4);
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
        displayAudit(tailer);
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
        tailer.tail();
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        tailer.assertAudit(2);
    }
    
    
    // TODO: user with no password

    // TODO: disabled user



}

