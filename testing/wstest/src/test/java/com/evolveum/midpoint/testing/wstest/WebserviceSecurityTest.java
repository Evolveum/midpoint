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
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.evolveum.midpoint.testing.wstest.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.testing.wstest.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.testing.wstest.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.testing.wstest.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.testing.wstest.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.testing.wstest.xml.ns._public.model.model_1_wsdl.ModelService;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    /**
     *  Creates modelPortType
     * */
    protected static ModelPortType createModelPort(String username, String testName){
        String endpointUrl = "http://localhost:8080/midpoint/model/model-1";

        LOGGER.info("WSSecurityTests: #createModelPort: Endpoint URL: " + endpointUrl + " for test: " + testName);

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
        outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, SecurityClientPasswordHandler.class.getName());

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(wssOut);

        return modelPort;
    }

    private Object unmarshallFromFile(File file, String context) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(context);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement jaxbElement = (JAXBElement)jaxbUnmarshaller.unmarshal(file);
        return jaxbElement.getValue();
    }

    /*===============================================================================================================*/
    /*                                                      TESTS                                                    */
    /*===============================================================================================================*/

    @BeforeClass
    public void beforeTests(){
        LOGGER.info("WebService security test suite started.");
        initSystem();
    }

    @AfterClass
    public void afterTests() throws FaultMessage, JAXBException{
        modelPort = createModelPort();
        cleanRepository();
        LOGGER.info("WebService security test suite finished successfully. All tests passed.");
    }

    /**
     *  In this test, we try to use webservice as administrator, but we use wrong password.
     *  First, we create modelPort with wrong password, then we try to access modelPort
     *  operations.
     *
     *  SoapFault is expected, or other security exception
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurityTest01() throws FaultMessage, SoapFault{
        SecurityClientPasswordHandler.setClientPassword("wrongAdministratorPassword");
        modelPort = createModelPort("administrator", "wsSecurityTest01");

        //Try to retrieve system configuration should fail
        configurationType = getConfiguration(modelPort);
    }

    /**
     *  In this test, we first create and add user with admin privileges and empty password.
     *  Next step is to try to use webservice with this user, so we create model port with
     *  this user and set empty logon password.
     *
     *  Exception is expected
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurity02() throws FaultMessage, JAXBException{
        modelPort = createModelPort();
        configurationType = getConfiguration(modelPort);
        File file = new File(SECURITY_DIR_NAME + USER_TEST_2_FILENAME);
        UserType user = (UserType)unmarshallFromFile(file, PACKAGE_COMMON_2A);

        modelPort.addObject(user, new Holder<String>(), new Holder<OperationResultType>());
        modelPort.deleteObject(getTypeUri(SystemConfigurationType.class), configurationType.getOid());

        file = new File(SECURITY_DIR_NAME + SYSTEM_CONFIG_FILENAME);
        SystemConfigurationType config = (SystemConfigurationType)unmarshallFromFile(file, PACKAGE_COMMON_2A);
        modelPort.addObject(config, new Holder<String>(), new Holder<OperationResultType>());

        file = new File(SECURITY_DIR_NAME + USER_TEST_2_CLEAN_PASS_FILENAME);
        ObjectModificationType objectChange = (ObjectModificationType)unmarshallFromFile(file, PACKAGE_API_TYPES_2);
        modelPort.modifyObject(getTypeUri(UserType.class), objectChange);

        modelPort.deleteObject(getTypeUri(SystemConfigurationType.class), system_config_oid);
        file = new File(SECURITY_DIR_NAME + SYSTEM_CONFIG_NORMAL_FILENAME);
        config = (SystemConfigurationType)unmarshallFromFile(file, PACKAGE_COMMON_2A);
        modelPort.addObject(config, new Holder<String>(), new Holder<OperationResultType>());

        SecurityClientPasswordHandler.setClientPassword("");
        modelPort = createModelPort("Anakin2", "wsSecurity02");
        modelPort.getObject(getTypeUri(UserType.class), test_user_oid_2, new OperationOptionsType(), new Holder<ObjectType>(), new Holder<OperationResultType>());
    }

    /**
     *  In this test, we try to establish connection without webservice token
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurity03() throws FaultMessage{
        String endpointUrl = "http://localhost:8080/midpoint/model/model-1";

        LOGGER.info("WSSecurityTests: #createModelPort: Endpoint URL: " + endpointUrl + " for test: wsSecurity03");

        ModelService modelService = new ModelService();
        ModelPortType modelPort = modelService.getModelPort();
        BindingProvider bp = (BindingProvider)modelPort;
        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
        org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);

        //Here we don't send token to webService
        //outProps.put(WSHandlerConstants.USER, username);
        //outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        //outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, SecurityClientPasswordHandler.class.getName());

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(wssOut);

        configurationType = getConfiguration(modelPort);
    }

    /**
     *  Here will be test with user with non-existing username
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurity04() throws FaultMessage{
        modelPort = createModelPort("Not Existing user", "wsSecurity04");
        configurationType = getConfiguration(modelPort);
    }

    /**
     *  In this test, we test, if user with no admin privilages, but correct credentials is able to
     *  log on midpoint webservices and use them. So, first we create non-admin user and then we
     *  simply try to use we operations with him
     *
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurity05() throws JAXBException, FaultMessage{
        modelPort = createModelPort();
        File file = new File(SECURITY_DIR_NAME + USER_TEST_5_FILENAME);
        UserType user = (UserType)unmarshallFromFile(file, PACKAGE_COMMON_2A);
        modelPort.addObject(user, new Holder<String>(), new Holder<OperationResultType>());

        SecurityClientPasswordHandler.setClientPassword(UNIVERSAL_USER_PASSWORD);
        modelPort = createModelPort("Anakin5", "wsSecurity05");
        configurationType = getConfiguration(modelPort);
    }

    /**
     *  In this test, we test disabled user accessibility to midpoint webservice. To prevent
     *  other potential faults, test user has enabled admin privileges and provided password
     *  is correct
     *
     *  Exception is expected
     * */
    @Test(expectedExceptions = Exception.class)
    public void wsSecurity06() throws JAXBException, FaultMessage{
        modelPort = createModelPort();
        File file = new File(SECURITY_DIR_NAME + USER_TEST_6_FILENAME);
        UserType user = (UserType)unmarshallFromFile(file, PACKAGE_COMMON_2A);
        modelPort.addObject(user, new Holder<String>(), new Holder<OperationResultType>());

        SecurityClientPasswordHandler.setClientPassword(UNIVERSAL_USER_PASSWORD);
        modelPort = createModelPort("Anakin6", "wsSecurity06");
        configurationType = getConfiguration(modelPort);
    }
}



/**
 *  Custom Password Handler for these tests, we need to create customized modelPorts
 *  (With custom passwords)
 *
 *  @author Erik Suta
 *
 */
class SecurityClientPasswordHandler implements CallbackHandler {

    private static String clientPassword = "5ecr3t";

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        // set the password for our message.
        pc.setPassword(clientPassword);
    }

    public static String getClientPassword() {
        return clientPassword;
    }

    public static void setClientPassword(String clientPassword) {
        SecurityClientPasswordHandler.clientPassword = clientPassword;
    }
}

