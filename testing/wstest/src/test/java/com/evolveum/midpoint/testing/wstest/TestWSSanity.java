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
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.PolicyViolationFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Testing basic web service operations
 *
 *  @author Radovan Semancik
 * */

@ContextConfiguration(locations = {"classpath:ctx-wstest-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestWSSanity extends AbstractWebserviceTest {
	
	protected static OpenDJController openDJController = new OpenDJController();
	private String connectorLdapOid = null;

    @Override
	protected void startResources() throws Exception {
		super.startResources();
		openDJController.startCleanServer();
	}
    
	@Override
	protected void stopResources() throws Exception {
		super.stopResources();
		openDJController.stop();
	}
	
	// TODO: fetch&parse schema http://..?WSDL

	@Test
    public void test015SearchLdapConnector() throws Exception {
    	final String TEST_NAME = "test015SearchLdapConnector";
    	displayTestTitle(TEST_NAME);
    	
        QueryType query = new QueryType();
        query.setFilter(ModelClientUtil.parseSearchFilterType(
        		    "<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
      				  "<path>c:connectorType</path>" +
      				  "<value>" + CONNECTOR_LDAP_TYPE + "</value>" +
      				"</equal>"
        ));
		
        Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		
		// WHEN
        modelPort.searchObjects(ModelClientUtil.getTypeQName(ConnectorType.class), query, null, objectListHolder, resultHolder);
     
        // THEN
        assertSuccess(resultHolder);
        ObjectListType objectList = objectListHolder.value;
        assertEquals("Unexpected number of LDAP connectors", 1, objectList.getObject().size());
        ConnectorType ldapConnector = (ConnectorType) objectList.getObject().get(0);
        assertNotNull("Null LDAP connector", ldapConnector);
        connectorLdapOid = ldapConnector.getOid();
        assertNotNull("Null LDAP connector OID", connectorLdapOid);
    }
	
	@Test
    public void test020AddResourceOpenDj() throws Exception {
    	final String TEST_NAME = "test020AddResourceOpenDj";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();
    	ResourceType resource = ModelClientUtil.unmarshallFile(RESOURCE_OPENDJ_FILE);
    	resource.getConnectorRef().setOid(connectorLdapOid);
        
        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        // WHEN
        String oid = addObject(resource);
     
        // THEN
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        assertEquals("Wrong OID", RESOURCE_OPENDJ_OID, oid);
        
        ResourceType resourceAfter = getObject(ResourceType.class, RESOURCE_OPENDJ_OID);
        display(resourceAfter);
        
        assertCreateMetadata(resourceAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
    }

	@Test
    public void test030ResourceOpenDjTestConnection() throws Exception {
    	final String TEST_NAME = "test030ResourceOpenDjTestConnection";
    	displayTestTitle(TEST_NAME);
    	
    	ResourceType resource = ModelClientUtil.unmarshallFile(RESOURCE_OPENDJ_FILE);
        
        // WHEN
        OperationResultType testResult = modelPort.testResource(RESOURCE_OPENDJ_OID);
     
        // THEN
        display(testResult);
        assertSuccess(testResult);
    }
	
	// TODO: test non-existing resource

}

