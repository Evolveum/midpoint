/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.model.client.sample;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.RetrieveOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An attempt to have an automated test for Exchange connector.
 *
 * To minimalize technical problems of communication with remote Windows machine
 * the commands to the connectors are sent via midPoint (Web Service) API.
 *
 * To run this test the following is required:
 * (1) A Windows machine (anywhere in the network), with the following set up and running:
 *     a) Microsoft Exchange
 *     b) Connector Server and Exchange connector
 * (2) A midPoint machine (anywhere in the network), with the following elements configured:
 *     a) ConnectorHost pointing to the Windows machine
 *     b) discovered Exchange connector
 *     c) adequately defined Exchange resource (TODO specify more exactly)
 *
 * To run this test, the following system properties have to be provided:
 *
 *  - resourceOid (e.g. "11111111-2222-1111-1111-000000000010")
 *     -> the OID of Exchange resource (see 2c above)
 *  - container (e.g. "OU=ConnectorTest,DC=xxxx,DC=yyyy,DC=com")
 *     -> in which container the testing would take place
 *        (must correspond to the container defined in the Exchange resource)
 *  - mailDomain (e.g. "xxxx.yyyy.com")
 *     -> used for testing mail addresses for user accounts created by the connector
 *        (must correspond to address policies specified on the Exchange server)
 *
 * Again, this is HIGHLY EXPERIMENTAL.
 *
 * @author mederly
 *
 */
public class TestExchangeConnector {
	
	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost.:8080/midpoint/model/model-3";
	
	// Object OIDs

    // Other
    private static final String NS_RI = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
    private static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";

    private static final QName OC_ACCOUNT = new QName(NS_RI, "AccountObjectClass");
    private static final QName OC_ACCEPTED_DOMAIN = new QName(NS_RI, "CustomAcceptedDomainObjectClass");
    private static final QName OC_GLOBAL_ADDRESS_LIST = new QName(NS_RI, "CustomGlobalAddressListObjectClass");
    private static final QName OC_ADDRESS_LIST = new QName(NS_RI, "CustomAddressListObjectClass");
    private static final QName OC_OFFLINE_ADDRESS_BOOK = new QName(NS_RI, "CustomOfflineAddressBookObjectClass");
    private static final QName OC_ADDRESS_BOOK_POLICY = new QName(NS_RI, "CustomAddressBookPolicyObjectClass");
    private static final QName OC_DISTRIBUTION_GROUP = new QName(NS_RI, "CustomDistributionGroupObjectClass");
    private static final QName OC_EMAIL_ADDRESS_POLICY = new QName(NS_RI, "CustomEmailAddressPolicyObjectClass");

    private static final String NEWTON_GIVEN_NAME = "Isaac";
    private static final String NEWTON_SN = "Newton";
    private String newtonOid;

    private static final String LEIBNIZ_GIVEN_NAME = "Gottfried Wilhelm";
    private static final String LEIBNIZ_SN = "Leibniz";
    private String leibnizOid;

    private static final String PASCAL_GIVEN_NAME = "Blaise";
    private static final String PASCAL_SN = "Pascal";
    private String pascalOid;

    private List<String> acceptedDomains = new ArrayList<>();
    private List<String> globalAddressLists = new ArrayList<>();
    private List<String> addressLists = new ArrayList<>();
    private List<String> offlineAddressBooks = new ArrayList<>();
    private List<String> addressBookPolicies = new ArrayList<>();
    private List<String> distributionGroups = new ArrayList<>();
    private List<String> emailAddressPolicies = new ArrayList<>();

    private String dn(String givenName, String sn) {
        return "CN=" + givenName + " " + sn + "," + getContainer();
    }

    private String mail(String givenName, String sn) {
        return sn.toLowerCase() + "@" + getMailDomain();
    }

    private String getContainer() {
        return System.getProperty("container");
    }
    
    private String getResourceOid() {
        return System.getProperty("resourceOid");
    }

    private ModelPortType modelPort;

    public String getMailDomain() {
        return System.getProperty("mailDomain");
    }

    @BeforeClass
	public void initialize() throws Exception {
        modelPort = createModelPort(new String[0]);
    }

//    @Test
//    public void test000() throws InvalidNameException {
//        System.out.println(distributionGroupOU());
//        System.exit(0);
//    }

    @Test
    public void test001GetResource() throws Exception {
        System.out.println("Getting Exchange resource...");
        ResourceType exchangeResource = getResource(getResourceOid());
        if (exchangeResource == null) {
            throw new IllegalStateException("Exchange resource was not found");
        }
        System.out.println("Got it; name = " + getOrig(exchangeResource.getName()));
    }

    // =============== AcceptedDomain ===============

    @Test
    public void test010CreateAcceptedDomain() throws Exception {
        createAndCheckAcceptedDomain("Scientists Domain", "scientists.com", "Authoritative");
    }

    @Test
    public void test012ModifyAcceptedDomain() throws Exception {
        ShadowType domain = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, "Scientists Domain");
        modifyShadow(domain.getOid(), "attributes/name", ModificationTypeType.REPLACE, "Scientists Domain Updated");
        modifyShadow(domain.getOid(), "attributes/DomainType", ModificationTypeType.REPLACE, "InternalRelay");
        checkAcceptedDomain("Scientists Domain Updated", "scientists.com", "InternalRelay");
    }

    @Test
    public void test015CreateAndDeleteAcceptedDomain() throws Exception {
        String oid = createAndCheckAcceptedDomain("Temporary domain", "temp.com", "Authoritative");
        deleteShadow(getResourceOid(), oid, false);
        acceptedDomains.remove(oid);
        ShadowType tempDomain = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, "Temporary domain");
        AssertJUnit.assertNull("Temporary domain was not removed", tempDomain);
    }

    private String createAndCheckAcceptedDomain(String name, String domainName, String domainType) throws Exception {
        System.out.println("Creating accepted domain " + name);
        String oid = createAcceptedDomain(name, domainName, domainType);
        System.out.println("Done; OID = " + oid);
        acceptedDomains.add(oid);
        return checkAcceptedDomain(name, domainName, domainType).getOid();
    }

    private String createAcceptedDomain(String name, String domainName, String domainType) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ACCEPTED_DOMAIN);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("accepted-domain");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "DomainName"), domainName, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "DomainType"), domainType, doc));

        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkAcceptedDomain(String name, String domainName, String domainType) throws Exception {
        System.out.println("Retrieving AcceptedDomain " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, name);
        AssertJUnit.assertNotNull("AcceptedDomain " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "DomainName", domainName);
        assertAttributeEquals(attrs, "DomainType", domainType);
        return shadowType;
    }

    // =============== GAL ===============

    @Test
    public void test020CreateGlobalAddressList() throws Exception {
        createAndCheckGlobalAddressList("Scientists Global Address List", "Scientist");
    }

    @Test
    public void test022ModifyGlobalAddressList() throws Exception {
        ShadowType gal = getShadowByName(getResourceOid(), OC_GLOBAL_ADDRESS_LIST, "Scientists Global Address List");
        modifyShadow(gal.getOid(), "attributes/name", ModificationTypeType.REPLACE, "Scientists Global Address List Updated");
        modifyShadow(gal.getOid(), "attributes/RecipientFilter", ModificationTypeType.REPLACE, "CustomAttribute1 -eq 'Scientist'");
        checkGlobalAddressList("Scientists Global Address List Updated", "CustomAttribute1 -eq 'Scientist'");
    }

    @Test
    public void test025CreateAndDeleteGlobalAddressList() throws Exception {
        String oid = createAndCheckGlobalAddressList("Temporary GAL", "TEMP");
        deleteShadow(getResourceOid(), oid, false);
        globalAddressLists.remove(oid);
        ShadowType tempGal = getShadowByName(getResourceOid(), OC_GLOBAL_ADDRESS_LIST, "Temporary GAL");
        AssertJUnit.assertNull("Temporary GAL was not removed", tempGal);
    }

    private String createAndCheckGlobalAddressList(String name, String valueToExpect) throws Exception {
        System.out.println("Creating GAL " + name);
        String oid = createGlobalAddressList(name, valueToExpect);
        System.out.println("Done; OID = " + oid);
        globalAddressLists.add(oid);
        return checkGlobalAddressList(name, galFilter(valueToExpect)).getOid();
    }

    private String galFilter(String valueToExpect) {
        return "((Alias -ne $null) -and (CustomAttribute1 -eq '" + valueToExpect + "'))";
    }

    private String createGlobalAddressList(String name, String valueToExpect) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_GLOBAL_ADDRESS_LIST);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("global-address-list");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "RecipientFilter"), galFilter(valueToExpect), doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkGlobalAddressList(String name, String recipientFilter) throws Exception {
        System.out.println("Retrieving GAL " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_GLOBAL_ADDRESS_LIST, name);
        AssertJUnit.assertNotNull("GAL " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "RecipientFilter", recipientFilter);
        return shadowType;
    }

    // =============== Address Lists ===============

    enum AddressListType {

        USERS("address-list-all-users", "((((Alias -ne $null) -and (CustomAttribute1 -eq '$$$'))) -and (ObjectCategory -like 'person'))"),
        GROUPS("address-list-all-groups", "((((Alias -ne $null) -and (CustomAttribute1 -eq '$$$'))) -and (ObjectCategory -like 'group'))"),
        CONTACTS("address-list-all-contacts", "((((Alias -ne $null) -and (CustomAttribute1 -eq '$$$'))) -and (((ObjectCategory -like 'person') -and (ObjectClass -eq 'contact'))))"),
        ROOMS("address-list-all-rooms", "((((Alias -ne $null) -and (CustomAttribute1 -eq '$$$'))) -and (((RecipientDisplayType -eq 'ConferenceRoomMailbox') -or (RecipientDisplayType -eq 'SyncedConferenceRoomMailbox'))))");

        private String intent;
        private final String filterTemplate;

        AddressListType(String intent, String filterTemplate) {
            this.intent = intent;
            this.filterTemplate = filterTemplate;
        }

        public String createFilter(String valueToExpect) {
            return filterTemplate.replace("$$$", valueToExpect);
        }

        public String getIntent() {
            return intent;
        }
    }

    @Test
    public void test030CreateAddressLists() throws Exception {
        createAndCheckAddressList("Scientists All Users", "Scientist", AddressListType.USERS);
        createAndCheckAddressList("Scientists All Groups", "Scientist", AddressListType.GROUPS);
        createAndCheckAddressList("Scientists All Contacts", "Scientist", AddressListType.CONTACTS);
        createAndCheckAddressList("Scientists All Rooms", "Scientist", AddressListType.ROOMS);
    }

    @Test
    public void test032ModifyAddressList() throws Exception {
        ShadowType gal = getShadowByName(getResourceOid(), OC_ADDRESS_LIST, "Scientists All Users");
        modifyShadow(gal.getOid(), "attributes/name", ModificationTypeType.REPLACE, "Scientists All Users Updated");
        modifyShadow(gal.getOid(), "attributes/RecipientFilter", ModificationTypeType.REPLACE, "CustomAttribute1 -eq 'Scientist'");
        checkAddressList("Scientists All Users Updated", "CustomAttribute1 -eq 'Scientist'");
    }

    @Test
    public void test035CreateAndDeleteAddressList() throws Exception {
        String oid = createAndCheckAddressList("Temporary Address List", "TEMP", AddressListType.USERS);
        deleteShadow(getResourceOid(), oid, false);
        addressLists.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_ADDRESS_LIST, "Temporary Address List");
        AssertJUnit.assertNull("Temporary AddressList was not removed", temp);
    }

    private String createAndCheckAddressList(String name, String valueToExpect, AddressListType type) throws Exception {
        System.out.println("Creating AddressList " + name);
        String oid = createAddressList(name, valueToExpect, type);
        System.out.println("Done; OID = " + oid);
        addressLists.add(oid);
        return checkAddressList(name, type.createFilter(valueToExpect)).getOid();
    }

    private String createAddressList(String name, String valueToExpect, AddressListType type) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ADDRESS_LIST);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent(type.getIntent());

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "RecipientFilter"), type.createFilter(valueToExpect), doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkAddressList(String name, String recipientFilter) throws Exception {
        System.out.println("Retrieving AddressList " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_ADDRESS_LIST, name);
        AssertJUnit.assertNotNull("AddressList " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "RecipientFilter", recipientFilter);
        return shadowType;
    }

    // =============== OfflineAddressBook ===============

    @Test
    public void test040CreateOfflineAddressBook() throws Exception {
        createAndCheckOfflineAddressBook("Scientists Offline Address Book", "Scientists Global Address List Updated", "Scientists");
    }

    @Test
    public void test042ModifyOfflineAddressBook() throws Exception {
        String tempGal = createGlobalAddressList("Scientists Global Address List - Temporary", "TEMP");
        ShadowType shadow = getShadowByName(getResourceOid(), OC_OFFLINE_ADDRESS_BOOK, "Scientists Offline Address Book");
        modifyShadow(shadow.getOid(), "attributes/name", ModificationTypeType.REPLACE, "Scientists Offline Address Book Updated");
        modifyShadow(shadow.getOid(), "attributes/AddressLists", ModificationTypeType.REPLACE, "Scientists Global Address List - Temporary");
        checkOfflineAddressBook("Scientists Offline Address Book Updated", "Scientists Global Address List - Temporary");

        modifyShadow(shadow.getOid(), "attributes/AddressLists", ModificationTypeType.REPLACE, "Scientists Global Address List Updated");
        checkOfflineAddressBook("Scientists Offline Address Book Updated", "Scientists Global Address List Updated");

        deleteShadow(getResourceOid(), tempGal, false);
        globalAddressLists.remove(tempGal);
    }

    @Test
    public void test045CreateAndDeleteOfflineAddressBook() throws Exception {
        String oid = createAndCheckOfflineAddressBook("Temporary OAB", "Scientists Global Address List Updated", "Scientists");
        deleteShadow(getResourceOid(), oid, false);
        offlineAddressBooks.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_OFFLINE_ADDRESS_BOOK, "Temporary OAB");
        AssertJUnit.assertNull("Temporary OAB was not removed", temp);
    }

    private String createAndCheckOfflineAddressBook(String name, String addressList, String tenantName) throws Exception {
        System.out.println("Creating OAB " + name);
        String oid = createOfflineAddressBook(name, addressList, tenantName);
        System.out.println("Done; OID = " + oid);
        offlineAddressBooks.add(oid);
        return checkOfflineAddressBook(name, addressList).getOid();
    }

    private String createOfflineAddressBook(String name, String addressList, String tenantName) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_OFFLINE_ADDRESS_BOOK);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("offline-address-book");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "AddressLists"), addressList, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "_TenantName"), tenantName, doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkOfflineAddressBook(String name, String addressList) throws Exception {
        System.out.println("Retrieving OAB " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_OFFLINE_ADDRESS_BOOK, name);
        AssertJUnit.assertNotNull("OAB " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "AddressLists", "\\" + addressList);
        return shadowType;
    }

    // TODO checking access rights to download OAB

    // =============== AddressBookPolicy ===============

    @Test
    public void test050CreateAddressBookPolicy() throws Exception {
        createAndCheckAddressBookPolicy("Scientists Address Book Policy",
                Arrays.asList("\\Scientists All Users Updated",
                        "\\Scientists All Groups",
                        "\\Scientists All Contacts"),
                "\\Scientists Global Address List Updated",
                "\\Scientists Offline Address Book Updated",
                "\\Scientists All Rooms");
    }

    @Test
    public void test052ModifyAddressBookPolicy() throws Exception {
        ShadowType shadow = getShadowByName(getResourceOid(), OC_ADDRESS_BOOK_POLICY, "Scientists Address Book Policy");
        modifyShadow(shadow.getOid(), "attributes/name", ModificationTypeType.REPLACE, "Scientists Address Book Policy Updated");
        // TODO enable this
//        modifyShadow(shadow.getOid(), "attributes/AddressLists", ModificationTypeType.DELETE, "Scientists All Contacts");
//        checkAddressBookPolicy("Scientists Address Book Policy Updated",
//                new HashSet(Arrays.asList("Scientists All Users Updated", "Scientists All Groups")),
//                "Scientists Global Address List Updated",
//                "Scientists Offline Address Book Updated",
//                "Scientists All Rooms");
    }

    @Test
    public void test055CreateAndDeleteAddressBookPolicy() throws Exception {
        String oid = createAndCheckAddressBookPolicy("Temporary Address Book Policy",
                Arrays.asList("\\Scientists All Users Updated",
                        "\\Scientists All Groups",
                        "\\Scientists All Contacts"),
                "\\Scientists Global Address List Updated",
                "\\Scientists Offline Address Book Updated",
                "\\Scientists All Rooms");

        deleteShadow(getResourceOid(), oid, false);
        addressBookPolicies.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_ADDRESS_BOOK_POLICY, "Temporary Address Book Policy");
        AssertJUnit.assertNull("Temporary Address Book Policy was not removed", temp);
    }

    private String createAndCheckAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws Exception {
        System.out.println("Creating ABP " + name);
        String oid = createAddressBookPolicy(name, addressLists, gal, oab, rooms);
        System.out.println("Done; OID = " + oid);
        addressBookPolicies.add(oid);
        return checkAddressBookPolicy(name, addressLists, gal, oab, rooms).getOid();
    }

    private String createAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ADDRESS_BOOK_POLICY);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("address-book-policy");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        for (String addressList : addressLists) {
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "AddressLists"), addressList, doc));
        }
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "GlobalAddressList"), gal, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "OfflineAddressBook"), oab, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "RoomList"), rooms, doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws Exception {
        System.out.println("Retrieving ABP " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_ADDRESS_BOOK_POLICY, name);
        AssertJUnit.assertNotNull("ABP " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "AddressLists", new HashSet<>(addressLists));
        assertAttributeEquals(attrs, "GlobalAddressList", gal);
        assertAttributeEquals(attrs, "OfflineAddressBook", oab);
        assertAttributeEquals(attrs, "RoomList", rooms);
        return shadowType;
    }

    // =============== DistributionGroup ===============

    private String distributionGroupName(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    private String distributionGroupPrimaryAddress(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    private Collection<String> distributionGroupMembers(String name) {
        return Arrays.asList("Mail-" + name + "@AllUsers");
    }

    private String distributionGroupDisplayName(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    private String distributionGroupOU() throws InvalidNameException {
        LdapName container = new LdapName(getContainer());
        List<String> ous = new ArrayList<>();
        List<String> dcs = new ArrayList<>();
        String retval = "";
        for (Rdn rdn : container.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("OU")) {
                ous.add(rdn.getValue().toString());
            } else if (rdn.getType().equalsIgnoreCase("DC")) {
                dcs.add(rdn.getValue().toString());
            }
        }
        for (int i = dcs.size()-1; i >= 0; i--) {
            if (!retval.isEmpty()) {
                retval += ".";
            }
            retval += dcs.get(i);
        }
        for (int i = 0; i < ous.size(); i++) {
            retval += "/" + ous.get(i);
        }
        return retval;
    }

    @Test
    public void test060CreateDistributionGroup() throws Exception {
        String n = "Scientists";
        createAndCheckDistributionGroup(distributionGroupName(n),
                distributionGroupPrimaryAddress(n),
                distributionGroupMembers(n),
                distributionGroupOU(),
                distributionGroupDisplayName(n),
                "Scientist");
    }


    @Test
    public void test062ModifyDistributionGroup() throws Exception {
        String n = "Scientists";
        String newName = distributionGroupName(n) + "-Updated";
        String newAddress = distributionGroupPrimaryAddress(n) + "-Updated";
        String newDisplayName = distributionGroupDisplayName(n) + " Updated";
        String newCustomAttribute1 = "GreatScientist";
        ShadowType shadow = getShadowByName(getResourceOid(), OC_DISTRIBUTION_GROUP, distributionGroupName(n));
        modifyShadow(shadow.getOid(), "attributes/name", ModificationTypeType.REPLACE, newName);
        modifyShadow(shadow.getOid(), "attributes/PrimarySmtpAddress", ModificationTypeType.REPLACE, newAddress);
        modifyShadow(shadow.getOid(), "attributes/DisplayName", ModificationTypeType.REPLACE, newDisplayName);
        modifyShadow(shadow.getOid(), "attributes/CustomAttribute1", ModificationTypeType.REPLACE, newCustomAttribute1);

        checkDistributionGroup(newName,
                newAddress,
                distributionGroupMembers(n),
                distributionGroupOU(),
                newDisplayName,
                newCustomAttribute1);
    }

    @Test
    public void test065CreateAndDeleteDistributionGroup() throws Exception {
        String n = "TEMP";
        String oid = createAndCheckDistributionGroup(distributionGroupName(n),
                distributionGroupPrimaryAddress(n),
                distributionGroupMembers("Scientists"),
                distributionGroupOU(),
                distributionGroupDisplayName(n),
                "TEMP");

        deleteShadow(getResourceOid(), oid, false);
        distributionGroups.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_DISTRIBUTION_GROUP, distributionGroupName(n));
        AssertJUnit.assertNull("Temporary DistributionGroup was not removed", temp);
    }

    private String createAndCheckDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws Exception {
        System.out.println("Creating DG " + name);
        String oid = createDistributionGroup(name, primaryAddress, members, ou, displayName, valueForFilter);
        System.out.println("Done; OID = " + oid);
        distributionGroups.add(oid);
        return checkDistributionGroup(name, primaryAddress, members, ou, displayName, valueForFilter).getOid();
    }

    private String createDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_DISTRIBUTION_GROUP);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("distribution-group");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "Type"), "Security", doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "PrimarySmtpAddress"), primaryAddress, doc));
        for (String member : members) {
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "Members"), member, doc));
        }
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "OrganizationalUnit"), ou, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "DisplayName"), displayName, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "HiddenFromAddressListsEnabled"), "true", doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "CustomAttribute1"), valueForFilter, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "BypassSecurityGroupManagerCheck"), "true", doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    private ShadowType checkDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws Exception {
        System.out.println("Retrieving DistributionGroup " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_DISTRIBUTION_GROUP, name);
        AssertJUnit.assertNotNull("DistributionGroup " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        assertAttributeEquals(attrs, "RecipientType", "MailUniversalSecurityGroup");
        assertAttributeEquals(attrs, "PrimarySmtpAddress", primaryAddress);
        //Members cannot be retrieved in this way
        //assertAttributeEquals(attrs, "Members", members);
        assertAttributeEquals(attrs, "OrganizationalUnit", ou);
        assertAttributeEquals(attrs, "DisplayName", displayName);
        assertAttributeEquals(attrs, "HiddenFromAddressListsEnabled", "true");
        assertAttributeEquals(attrs, "CustomAttribute1", valueForFilter);
        return shadowType;
    }

    // =============== Users ===============

    @Test
    public void test110CreateNewton() throws Exception {
        System.out.println("Creating account for Newton...");
        newtonOid = createAccount(NEWTON_GIVEN_NAME, NEWTON_SN, "User");
        System.out.println("Done; OID = " + newtonOid);
    }

    @Test
    public void test112GetNewton() throws Exception {
        ShadowType newton = checkAccount(NEWTON_GIVEN_NAME, NEWTON_SN);
        Map<String,Object> attrs = getAttributesAsMap(newton);
        assertAttributeEquals(attrs, "RecipientType", "User");
    }

    @Test
    public void test120CreateLeibniz() throws Exception {
        System.out.println("Creating account for Leibniz...");
        leibnizOid = createAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, "UserMailbox");
        System.out.println("Done; OID = " + leibnizOid);
    }

    @Test
    public void test122GetLeibniz() throws Exception {
        String mail = mail(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN);
        ShadowType leibniz = checkAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN);
        Map<String,Object> attrs = getAttributesAsMap(leibniz);
        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
        assertAttributeExists(attrs, "homeMDB");
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
        assertAttributeEquals(attrs, "mail", mail);
        assertAttributeEquals(attrs, "Alias", LEIBNIZ_SN.toLowerCase());
        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", LEIBNIZ_GIVEN_NAME + " " + LEIBNIZ_SN);
    }

    @Test
    public void test130CreatePascal() throws Exception {
        System.out.println("Creating account for Pascal...");
        pascalOid = createAccount(PASCAL_GIVEN_NAME, PASCAL_SN, "UserMailbox");
        System.out.println("Done; OID = " + pascalOid);

        String mail = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN);
        Map<String,Object> attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
        assertAttributeExists(attrs, "homeMDB");
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
        assertAttributeEquals(attrs, "mail", mail);
        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
    }

    @Test
    public void test132AddSecondaryAddressToPascal() throws Exception {
        String mail1 = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
        String mail2 = "pascal@clermont-ferrand.fr";

        System.out.println("Setting new secondary address to Pascal...");
        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.ADD, "smtp:" + mail2);
        System.out.println("Done");

        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN);
        Map<String,Object> attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail1);
        assertAttributeEquals(attrs, "mail", mail1);
        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
        assertAttributeContains(attrs, "EmailAddresses", new HashSet<>(Arrays.asList("SMTP:" + mail1, "smtp:" + mail2)));           // FIXME
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
    }

    @Test
    public void test134SwapAddressesForPascal() throws Exception {
        String mail1 = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
        String mail2 = "pascal@clermont-ferrand.fr";

        System.out.println("Disabling email address policy for Pascal...");
        modifyShadow(pascalOid, "attributes/EmailAddressPolicyEnabled", ModificationTypeType.REPLACE, false);
        System.out.println("Done");
        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN);
        Map<String,Object> attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "false");

        System.out.println("Setting new email addresses for Pascal...");
        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.REPLACE, new HashSet<>(Arrays.asList("smtp:" + mail1, "SMTP:" + mail2)));
        System.out.println("Done");

        pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN);
        attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail2);
        assertAttributeEquals(attrs, "mail", mail2);
        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
        assertAttributeEquals(attrs, "EmailAddresses", new HashSet<>(Arrays.asList("SMTP:"+mail2, "smtp:"+mail1)));
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
    }

    // non-existing objects
    @Test
    public void test200FetchingNonexistingPowerShellObject() throws Exception {
        ShadowType domain = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, "Non-existing domain");
        AssertJUnit.assertNull("Non-existing domain was found somehow", domain);
    }

    @Test
    public void test201FetchingNonexistingAccount() throws Exception {
        ShadowType account = getShadowByName(getResourceOid(), OC_ACCOUNT, "Non-existing user account");
        AssertJUnit.assertNull("Non-existing account was found somehow", account);
    }

    // TODO some tests for modifying non-existing objects

    @Test
    public void test900Cleanup() throws Exception {
        deleteShadow(getResourceOid(), newtonOid, true);
        deleteShadow(getResourceOid(), leibnizOid, true);
        deleteShadow(getResourceOid(), pascalOid, true);
        deleteShadows(getResourceOid(), distributionGroups, true);
        deleteShadows(getResourceOid(), emailAddressPolicies, true);
        deleteShadows(getResourceOid(), addressBookPolicies, true);
        deleteShadows(getResourceOid(), offlineAddressBooks, true);
        deleteShadows(getResourceOid(), addressLists, true);
        deleteShadows(getResourceOid(), globalAddressLists, true);
        deleteShadows(getResourceOid(), acceptedDomains, true);
    }

    private void deleteShadows(String resourceOid, List<String> oids, boolean ignoreFailures) throws FaultMessage {
        for (String oid : oids) {
            deleteShadow(resourceOid, oid, ignoreFailures);
        }
    }

    private void deleteShadow(String resourceOid, String shadowOid, boolean ignoreFailures) throws FaultMessage {
        System.out.println("Deleting shadow " + shadowOid);
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(ShadowType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(shadowOid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        ModelExecuteOptionsType executeOptionsType = new ModelExecuteOptionsType();
        if (!ignoreFailures) {
            modelPort.executeChanges(deltaListType, executeOptionsType);
        } else {
            try {
                modelPort.executeChanges(deltaListType, executeOptionsType);
            } catch (Exception e) {
                System.err.println("Cannot remove " + shadowOid + ": " + e.getMessage());
            }
        }
    }

    private void modifyShadow(String oid, String path, ModificationTypeType modType, Object value) throws Exception {
        System.out.println("Modifying resource object " + oid + " (" + path + ")");
        ItemDeltaType itemDelta = new ItemDeltaType();
        itemDelta.setModificationType(modType);
        itemDelta.setPath(createNonDefaultItemPathType(path));
        if (!(value instanceof Collection)) {
            itemDelta.getValue().add(value);
        } else {
            itemDelta.getValue().addAll((Collection) value);
        }

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(ShadowType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);
        deltaType.getItemDelta().add(itemDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType odolist = modelPort.executeChanges(deltaListType, null);
        assertExecuteChangesSuccess(odolist);
    }

    private void assertExecuteChangesSuccess(ObjectDeltaOperationListType odolist) {
        for (ObjectDeltaOperationType odo : odolist.getDeltaOperation()) {
            AssertJUnit.assertNotNull("Operation result is null", odo.getExecutionResult());
            if (odo.getExecutionResult().getStatus() != OperationResultStatusType.SUCCESS) {
                System.out.println("!!! Operation result is " + odo.getExecutionResult().getStatus() + ": " + odo.getExecutionResult());
                AssertJUnit.assertEquals("Unexpected operation result status", OperationResultStatusType.SUCCESS, odo.getExecutionResult().getStatus());
            }
        }
    }

    private void assertAttributeExists(Map<String, Object> attrs, String name) {
        AssertJUnit.assertTrue("Attribute " + name + " is missing", attrs.containsKey(name));
    }

    private ShadowType checkAccount(String givenName, String sn) throws Exception {
        String dn = dn(givenName, sn);
        System.out.println("Retrieving " + sn + " account...");
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_ACCOUNT, dn);
        AssertJUnit.assertNotNull(sn + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "sAMAccountName", sn.toLowerCase());
        assertAttributeEquals(attrs, "distinguishedName", dn);
        assertAttributeEquals(attrs, "givenName", givenName);
        assertAttributeEquals(attrs, "sn", sn);
        assertAttributeEquals(attrs, "passwordExpired", true);
        assertAttributeEquals(attrs, "PasswordNeverExpires", "false");
        assertAttributeEquals(attrs, "ad_container", getContainer());
        assertAttributeEquals(attrs, "objectClass", new HashSet(Arrays.asList("top", "person", "organizationalPerson", "user")));
        return shadowType;
	}

    private void assertAttributeEquals(Map<String, Object> attrs, String name, Object expectedValue) {
        Object realValue = attrs.get(name);
        AssertJUnit.assertEquals("Unexpected value of attribute " + name, expectedValue, realValue);
    }

    private void assertAttributeContains(Map<String, Object> attrs, String name, Object expectedValue) {
        if (expectedValue instanceof Collection) {
            for (Object singleValue : (Collection) expectedValue) {
                assertAttributeContains(attrs, name, singleValue);
            }
        } else {
            Object realValue = attrs.get(name);
            Collection realCollection;
            if (realValue == null) {
                realCollection = new ArrayList();
            } else if (!(realValue instanceof Collection)) {
                realCollection = Arrays.asList(realValue);
            } else {
                realCollection = (Collection) realValue;
            }
            if (!realCollection.contains(expectedValue)) {
                AssertJUnit.assertTrue("Attribute " + name + " was expected to contain " + expectedValue + " but it doesn't: " + realCollection, false);
            }
        }
    }


    private void dumpAttributes(ShadowType shadowType) {
        ShadowAttributesType attributes = shadowType.getAttributes();
        System.out.println("Attributes for " + shadowType.getObjectClass().getLocalPart() + " " + getOrig(shadowType.getName()) + " {" + attributes.getAny().size() + " entries):");
        for (Object item : attributes.getAny()) {
            if (item instanceof Element) {
                Element e = (Element) item;
                System.out.println(" - " + e.getLocalName() + ": " + e.getTextContent());
            } else if (item instanceof JAXBElement) {
                JAXBElement je = (JAXBElement) item;
                String typeInfo = je.getValue() instanceof String ? "" : (" (" + je.getValue().getClass().getSimpleName() + ")");
                System.out.println(" - " + je.getName().getLocalPart() + ": " + je.getValue() + typeInfo);
            } else {
                System.out.println(" - "  + item);
            }
        }
    }

    private Map<String,Object> getAttributesAsMap(ShadowType shadowType) {
        Map<String,Object> rv = new HashMap<>();

        ShadowAttributesType attributes = shadowType.getAttributes();
        for (Object item : attributes.getAny()) {
            if (item instanceof Element) {
                Element e = (Element) item;
                put(rv, e.getLocalName(), e.getTextContent());
            } else if (item instanceof JAXBElement) {
                JAXBElement je = (JAXBElement) item;
                put(rv, je.getName().getLocalPart(), je.getValue());
            } else {
                // nothing to do here
            }
        }
        return rv;
    }

    private void put(Map<String, Object> map, String name, Object value) {
        Object existing = map.get(name);
        if (existing == null) {
            map.put(name, value);
        } else if (!(existing instanceof Set)) {
            Set set = new HashSet();
            set.add(existing);
            set.add(value);
            map.put(name, set);
        } else {
            ((Set) existing).add(value);
        }
    }

    // TODO move to ModelClientUtil
    private static String getOrig(PolyStringType polyStringType) {
        if (polyStringType == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : polyStringType.getContent()) {
            if (o instanceof String) {
                sb.append(o);
            } else if (o instanceof Element) {
                Element e = (Element) o;
                if ("orig".equals(e.getLocalName())) {
                    return e.getTextContent();
                }
            } else if (o instanceof JAXBElement) {
                JAXBElement je = (JAXBElement) o;
                if ("orig".equals(je.getName().getLocalPart())) {
                    return (String) je.getValue();
                }
            }
        }
        return sb.toString();
    }

    private static void dump(Collection<? extends ObjectType> objects) {
        System.out.println("Objects returned: " + objects.size());
        for (ObjectType objectType : objects) {
            System.out.println(" - " + getOrig(objectType.getName()) + ": " + objectType);
        }
    }

    private SystemConfigurationType getConfiguration() throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		
		modelPort.getObject(ModelClientUtil.getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
				objectHolder, resultHolder);
		
		return (SystemConfigurationType) objectHolder.value;
	}
	
	private Collection<ResourceType> listResources() throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

		modelPort.searchObjects(ModelClientUtil.getTypeQName(ResourceType.class), null, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}

    private ResourceType getResource(String oid) throws SAXException, IOException, FaultMessage {
        Holder<ObjectType> objectHolder = new Holder<>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

        modelPort.getObject(ModelClientUtil.getTypeQName(ResourceType.class), oid, options, objectHolder, resultHolder);

        return (ResourceType) objectHolder.value;
    }


    private Collection<UserType> listUsers() throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        // let's say we want to get first 3 users, sorted alphabetically by user name
        QueryType queryType = new QueryType();          // holds search query + paging options
        PagingType pagingType = new PagingType();
        pagingType.setMaxSize(3);
        pagingType.setOrderBy(ModelClientUtil.createItemPathType("name"));
        pagingType.setOrderDirection(OrderDirectionType.ASCENDING);
        queryType.setPaging(pagingType);

        modelPort.searchObjects(ModelClientUtil.getTypeQName(UserType.class), queryType, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        return (Collection) objectList.getObject();
    }

    private Collection<TaskType> listTasks() throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType operationOptions = new SelectorQualifiedGetOptionsType();

        // Let's say we want to retrieve tasks' next scheduled time (because this may be a costly operation if
        // JDBC based quartz scheduler is used, the fetching of this attribute has to be explicitly requested)
        SelectorQualifiedGetOptionType getNextScheduledTimeOption = new SelectorQualifiedGetOptionType();

        // prepare a selector (described by path) + options (saying to retrieve that attribute)
        ObjectSelectorType selector = new ObjectSelectorType();
        selector.setPath(ModelClientUtil.createItemPathType("nextRunStartTimestamp"));
        getNextScheduledTimeOption.setSelector(selector);
        GetOperationOptionsType selectorOptions = new GetOperationOptionsType();
        selectorOptions.setRetrieve(RetrieveOptionType.INCLUDE);
        getNextScheduledTimeOption.setOptions(selectorOptions);

        // add newly created option to the list of operation options
        operationOptions.getOption().add(getNextScheduledTimeOption);

        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.searchObjects(ModelClientUtil.getTypeQName(TaskType.class), null, operationOptions, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        return (Collection) objectList.getObject();
    }

    private String createUserGuybrush(RoleType role) throws FaultMessage {
		Document doc = ModelClientUtil.getDocumnent();
		
		UserType user = new UserType();
		user.setName(ModelClientUtil.createPolyStringType("guybrush", doc));
		user.setFullName(ModelClientUtil.createPolyStringType("Guybrush Threepwood", doc));
		user.setGivenName(ModelClientUtil.createPolyStringType("Guybrush", doc));
		user.setFamilyName(ModelClientUtil.createPolyStringType("Threepwood", doc));
		user.setEmailAddress("guybrush@meleeisland.net");
		user.getOrganization().add(ModelClientUtil.createPolyStringType("Pirate Brethren International", doc));
		user.getOrganizationalUnit().add(ModelClientUtil.createPolyStringType("Pirate Wannabes", doc));
		user.setCredentials(ModelClientUtil.createPasswordCredentials("IwannaBEaPIRATE"));
		
		if (role != null) {
			// create user with a role assignment
			AssignmentType roleAssignment = createRoleAssignment(role.getOid());
			user.getAssignment().add(roleAssignment);
		}
		
		return createUser(user);
	}

    private String createAccount(String givenName, String sn, String recipientType) throws FaultMessage {
        String name = dn(givenName, sn);
        String samAccountName = sn.toLowerCase();

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType user = new ShadowType();
        user.setName(ModelClientUtil.createPolyStringType(name, doc));
        user.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        user.setObjectClass(OC_ACCOUNT);
        user.setKind(ShadowKindType.ACCOUNT);

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "givenName"), givenName, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "sn"), sn, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "RecipientType"), recipientType, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "sAMAccountName"), samAccountName, doc));

        user.setAttributes(attributes);

        return createShadow(user);
    }

    private ShadowType getShadowByName(String resourceOid, QName objectClass, String name) throws JAXBException, SAXException, IOException, FaultMessage {
        // WARNING: in a real case make sure that the username is properly escaped before putting it in XML
        SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
                "                        <q:and xmlns:q='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3'>\n" +
                        "                            <q:ref>\n" +
                        "                                <q:path>resourceRef</q:path>\n" +
                        "                                <q:value>\n" +
                        "                                    <oid>" + resourceOid + "</oid>\n" +
                        "                                    <type>ResourceType</type>\n" +
                        "                                </q:value>\n" +
                        "                            </q:ref>\n" +
                        "                            <q:equal>\n" +
                        "                                <q:path>objectClass</q:path>\n" +
                        "                                <q:value xmlns:a=\"" + objectClass.getNamespaceURI() + "\">a:" + objectClass.getLocalPart() + "</q:value>\n" +
                        "                            </q:equal>\n" +
                        "                            <q:equal>\n" +
                        "                                <q:path>attributes/name</q:path>\n" +
                        "                                <q:value>" + name + "</q:value>\n" +
                        "                            </q:equal>\n" +
                        "                        </q:and>\n");
        QueryType query = new QueryType();
        query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<>();
        Holder<OperationResultType> resultHolder = new Holder<>();

        modelPort.searchObjects(ModelClientUtil.getTypeQName(ShadowType.class), query, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        List<ObjectType> objects = objectList.getObject();
        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() == 1) {
            return (ShadowType) objects.get(0);
        }
        throw new IllegalStateException("Expected to find a single shadow with name '"+name+"' but found "+objects.size()+" ones instead");
    }

    // TODO move to Util
    private static ObjectReferenceType createObjectReferenceType(Class<? extends ObjectType> typeClass, String oid) {
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(oid);
        ort.setType(ModelClientUtil.getTypeQName(typeClass));
        return ort;
    }


    private String createUserFromSystemResource(String resourcePath) throws FileNotFoundException, JAXBException, FaultMessage {
		UserType user = unmarshallResource(resourcePath);
		
		return createUser(user);
	}
	
	private static <T> T unmarshallFile(File file) throws JAXBException, FileNotFoundException {
		JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
		Unmarshaller unmarshaller = jc.createUnmarshaller(); 
		 
		InputStream is = null;
		JAXBElement<T> element = null;
		try {
			is = new FileInputStream(file);
			element = (JAXBElement<T>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}
	
	private static <T> T unmarshallResource(String path) throws JAXBException, FileNotFoundException {
		JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
		Unmarshaller unmarshaller = jc.createUnmarshaller(); 
		 
		InputStream is = null;
		JAXBElement<T> element = null;
		try {
			is = TestExchangeConnector.class.getClassLoader().getResourceAsStream(path);
			if (is == null) {
				throw new FileNotFoundException("System resource "+path+" was not found");
			}
			element = (JAXBElement<T>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}

    private String createUser(UserType userType) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(userType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
		ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
		return ModelClientUtil.getOidFromDeltaOperationList(operationListType, deltaType);
	}

    private String createShadow(ShadowType shadowType) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(ShadowType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(shadowType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
        assertExecuteChangesSuccess(operationListType);;
//        Holder<OperationResultType> holder = new Holder<>();
//        String oid = getOidFromDeltaOperationList(operationListType, deltaType, holder);
//        AssertJUnit.assertNotNull("No operation result from create shadow operation", holder.value);
//        if (holder.value != null && holder.value.getStatus() != OperationResultStatusType.SUCCESS) {
//            System.out.println("!!! Operation result is " + holder.value.getStatus() + ": " + holder.value);
//            AssertJUnit.assertEquals("Unexpected operation result status", OperationResultStatusType.SUCCESS, holder.value.getStatus());
//        }
        return ModelClientUtil.getOidFromDeltaOperationList(operationListType, deltaType);
    }

    /**
     * Retrieves OID and OperationResult created by model Web Service from the returned list of ObjectDeltaOperations.
     *
     * @param operationListType result of the model web service executeChanges call
     * @param originalDelta original request used to find corresponding ObjectDeltaOperationType instance. Must be of ADD type.
     * @param operationResultHolder where the result will be put
     * @return OID if found
     *
     * PRELIMINARY IMPLEMENTATION. Currently the first returned ADD delta with the same object type as original delta is returned.
     *
     * TODO move to ModelClientUtil
     */
    public static String getOidFromDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta, Holder<OperationResultType> operationResultTypeHolder) {
        Validate.notNull(operationListType);
        Validate.notNull(originalDelta);
        if (originalDelta.getChangeType() != ChangeTypeType.ADD) {
            throw new IllegalArgumentException("Original delta is not of ADD type");
        }
        if (originalDelta.getObjectToAdd() == null) {
            throw new IllegalArgumentException("Original delta contains no object-to-be-added");
        }
        for (ObjectDeltaOperationType operationType : operationListType.getDeltaOperation()) {
            ObjectDeltaType objectDeltaType = operationType.getObjectDelta();
            if (objectDeltaType.getChangeType() == ChangeTypeType.ADD &&
                    objectDeltaType.getObjectToAdd() != null) {
                ObjectType objectAdded = (ObjectType) objectDeltaType.getObjectToAdd();
                if (objectAdded.getClass().equals(originalDelta.getObjectToAdd().getClass())) {
                    operationResultTypeHolder.value = operationType.getExecutionResult();
                    return objectAdded.getOid();
                }
            }
        }
        return null;
    }

    private void changeUserPassword(String oid, String newPassword) throws FaultMessage {
		ItemDeltaType passwordDelta = new ItemDeltaType();
		passwordDelta.setModificationType(ModificationTypeType.REPLACE);
		passwordDelta.setPath(ModelClientUtil.createItemPathType("credentials/password/value"));
        passwordDelta.getValue().add(ModelClientUtil.createProtectedString(newPassword));

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);
        deltaType.getItemDelta().add(passwordDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        modelPort.executeChanges(deltaListType, null);
	}

    private void changeUserGivenName(String oid, String newValue) throws FaultMessage {
        Document doc = ModelClientUtil.getDocumnent();

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setOid(oid);
        userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        userDelta.setChangeType(ChangeTypeType.MODIFY);

        ItemDeltaType itemDelta = new ItemDeltaType();
        itemDelta.setModificationType(ModificationTypeType.REPLACE);
        itemDelta.setPath(ModelClientUtil.createItemPathType("givenName"));
        itemDelta.getValue().add(ModelClientUtil.createPolyStringType(newValue, doc));
        userDelta.getItemDelta().add(itemDelta);
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(userDelta);
        modelPort.executeChanges(deltaList, null);
    }

	private void assignRoles(String userOid, String... roleOids) throws FaultMessage {
		modifyRoleAssignment(userOid, true, roleOids);
	}
	
	private void unAssignRoles(String userOid, String... roleOids) throws FaultMessage {
		modifyRoleAssignment(userOid, false, roleOids);
	}
	
	private void modifyRoleAssignment(String userOid, boolean isAdd, String... roleOids) throws FaultMessage {
		ItemDeltaType assignmentDelta = new ItemDeltaType();
		if (isAdd) {
			assignmentDelta.setModificationType(ModificationTypeType.ADD);
		} else {
			assignmentDelta.setModificationType(ModificationTypeType.DELETE);
		}
        assignmentDelta.setPath(ModelClientUtil.createItemPathType("assignment"));
		for (String roleOid: roleOids) {
			assignmentDelta.getValue().add(createRoleAssignment(roleOid));
		}

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(userOid);
        deltaType.getItemDelta().add(assignmentDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(deltaListType, null);
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
            }
        }
	}

	private AssignmentType createRoleAssignment(String roleOid) {
		AssignmentType roleAssignment = new AssignmentType();
		ObjectReferenceType roleRef = new ObjectReferenceType();
		roleRef.setOid(roleOid);
		roleRef.setType(ModelClientUtil.getTypeQName(RoleType.class));
		roleAssignment.setTargetRef(roleRef);
		return roleAssignment;
	}

	private UserType searchUserByName(String username) throws SAXException, IOException, FaultMessage, JAXBException {
		// WARNING: in a real case make sure that the username is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:name</path>" +
				  "<value>" + username + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(UserType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		List<ObjectType> objects = objectList.getObject();
		if (objects.isEmpty()) {
			return null;
		}
		if (objects.size() == 1) {
			return (UserType) objects.get(0);
		}
		throw new IllegalStateException("Expected to find a single user with username '"+username+"' but found "+objects.size()+" users instead");
	}
	
	private RoleType searchRoleByName(String roleName) throws SAXException, IOException, FaultMessage, JAXBException {
		// WARNING: in a real case make sure that the role name is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:name</path>" +
				  "<value>" + roleName + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		List<ObjectType> objects = objectList.getObject();
		if (objects.isEmpty()) {
			return null;
		}
		if (objects.size() == 1) {
			return (RoleType) objects.get(0);
		}
		throw new IllegalStateException("Expected to find a single role with name '"+roleName+"' but found "+objects.size()+" users instead");
	}

	private Collection<RoleType> listRequestableRoles() throws SAXException, IOException, FaultMessage, JAXBException {
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:requestable</path>" +
				  "<value>true</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}

	private void deleteUser(String oid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(oid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        ModelExecuteOptionsType executeOptionsType = new ModelExecuteOptionsType();
        executeOptionsType.setRaw(true);
        modelPort.executeChanges(deltaListType, executeOptionsType);
	}

    private void deleteTask(String oid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(TaskType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(oid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        ModelExecuteOptionsType executeOptionsType = new ModelExecuteOptionsType();
        executeOptionsType.setRaw(true);
        modelPort.executeChanges(deltaListType, executeOptionsType);
    }
	
	public ModelPortType createModelPort(String[] args) {
		String endpointUrl = DEFAULT_ENDPOINT_URL;
		
		if (args.length > 0) {
			endpointUrl = args[0];
		}

		System.out.println("Endpoint URL: "+endpointUrl);

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
		ModelService modelService = new ModelService();
		ModelPortType modelPort = modelService.getModelPort();
		BindingProvider bp = (BindingProvider)modelPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
		
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
		
		Map<String,Object> outProps = new HashMap<String,Object>();
		
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, ADM_USERNAME);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);
        // enable the following to get client-side logging of outgoing requests and incoming responses
        //cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        //cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());

		return modelPort;
	}

    public static ItemPathType createNonDefaultItemPathType(String stringPath) {
        ItemPathType itemPathType = new ItemPathType();
        itemPathType.setValue(stringPath);
        return itemPathType;
    }


}
