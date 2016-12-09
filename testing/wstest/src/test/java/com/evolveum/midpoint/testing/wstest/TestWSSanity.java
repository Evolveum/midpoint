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
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.PolicyViolationFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
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
	private String accountJackOid;

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
                
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(ResourceType.class));
    	delta.setChangeType(ChangeTypeType.ADD);
    	delta.setObjectToAdd(resource);
		deltaList.getDelta().add(delta);
		
		ModelExecuteOptionsType options = new ModelExecuteOptionsType();
    	options.setIsImport(Boolean.TRUE);
		
        XMLGregorianCalendar startTs = TestUtil.currentTime();
		
		// WHEN
		ObjectDeltaOperationListType deltaOpList = modelPort.executeChanges(deltaList, options);
		
		// THEN
		assertSuccess(deltaOpList);
		String oid = deltaOpList.getDeltaOperation().get(0).getObjectDelta().getOid();
     
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
        
        assertEquals("Wrong connector OID", connectorLdapOid, resourceAfter.getConnectorRef().getOid());
        
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
	
	@Test
    public void test032ResourceNonexistingTestConnection() throws Exception {
    	final String TEST_NAME = "test032ResourceNonexistingTestConnection";
    	displayTestTitle(TEST_NAME);
        
        try {
	        // WHEN
	        OperationResultType testResult = modelPort.testResource("56b53914-df90-11e4-8c8c-001e8c717e5b");
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (FaultMessage f) {
        	assertFaultMessage(f, ObjectNotFoundFaultType.class, "was not found");
        }	    
        
    }
	
	// TODO: test unreachable resource
	
	@Test
    public void test100AddUserJack() throws Exception {
    	final String TEST_NAME = "test100AddUserJack";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        UserType userNobody = ModelClientUtil.unmarshallFile(USER_JACK_FILE);
        
        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        // WHEN
        String oid = addObject(userNobody);
     
        // THEN
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        // GET user
        UserType userAfter = getObject(UserType.class, USER_JACK_OID);
        display(userAfter);
        assertUser(userAfter, oid, USER_JACK_USERNAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        
        assertCreateMetadata(userAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
        assertPasswordCreateMetadata(userAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
    }
	
	@Test
    public void test110AssignOpenDJAccountToJack() throws Exception {
    	final String TEST_NAME = "test110AssignOpenDJAccountToJack";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        ObjectDeltaType delta = ModelClientUtil.createConstructionAssignDelta(UserType.class, USER_JACK_OID, RESOURCE_OPENDJ_OID);
        
		// WHEN
        ObjectDeltaOperationListType executedDeltas = modelPort.executeChanges(ModelClientUtil.createDeltaList(delta), null);
     
        // THEN
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        assertSuccess(executedDeltas);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "MODIFY_OBJECT");
        tailer.assertAudit(4);
        
        // GET user
        UserType userAfter = getObject(UserType.class, USER_JACK_OID);
        display(userAfter);
        assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        
        assertModifyMetadata(userAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
        
        accountJackOid = getSingleLinkOid(userAfter);
        assertNotNull(accountJackOid);
        
        Entry ldapEntry = openDJController.fetchEntry("uid="+USER_JACK_USERNAME+","+openDJController.getSuffixPeople());
        display(ldapEntry.toLDIFString());
        OpenDJController.assertAttribute(ldapEntry, "uid", "jack");
        OpenDJController.assertAttribute(ldapEntry, "givenName", "Jack");
        OpenDJController.assertAttribute(ldapEntry, "sn", "Sparrow");
        OpenDJController.assertAttribute(ldapEntry, "cn", "Jack Sparrow");
        OpenDJController.assertAttribute(ldapEntry, "displayName", "Jack Sparrow");
       
    }
	
	@Test
    public void test111CheckJackAccountShadow() throws Exception {
    	final String TEST_NAME = "test111CheckJackAccountShadow";
    	displayTestTitle(TEST_NAME);
    	
		Holder<ObjectType> objectHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		
		// WHEN
        modelPort.getObject(ModelClientUtil.getTypeQName(ShadowType.class), accountJackOid, null, objectHolder, resultHolder);
     
        // THEN
        assertSuccess(resultHolder);
        ShadowType shadow = (ShadowType) objectHolder.value;
        
        display(shadow);
        
        assertAttribute(shadow, ATTR_ICF_NAME_NAME, "uid="+USER_JACK_USERNAME+","+openDJController.getSuffixPeople());
        assertAttribute(shadow, "uid", "jack");
        assertAttribute(shadow, "givenName", "Jack");
        assertAttribute(shadow, "sn", "Sparrow");
        assertAttribute(shadow, "cn", "Jack Sparrow");
        assertAttribute(shadow, "displayName", "Jack Sparrow");       
    }
	
	@Test
    public void test112CheckJackAccountShadowRaw() throws Exception {
    	final String TEST_NAME = "test112CheckJackAccountShadowRaw";
    	displayTestTitle(TEST_NAME);
    	
		Holder<ObjectType> objectHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		
		// WHEN
        modelPort.getObject(ModelClientUtil.getTypeQName(ShadowType.class), accountJackOid, 
        		ModelClientUtil.createRootGetOptions(ModelClientUtil.createRawGetOption()), objectHolder, resultHolder);
     
        // THEN
        assertSuccess(resultHolder);
        ShadowType shadow = (ShadowType) objectHolder.value;
        
        display(shadow);
        
        assertAttribute(shadow, ATTR_ICF_NAME_NAME, "uid="+USER_JACK_USERNAME+","+openDJController.getSuffixPeople());
        assertNoAttribute(shadow, "uid");
        assertNoAttribute(shadow, "givenName");
        assertNoAttribute(shadow, "sn");
        assertNoAttribute(shadow, "cn");
        assertNoAttribute(shadow, "displayName");       
    }

}

