/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OptionObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RetrieveOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
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
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
 * Common functionality for connector-related tests.
 *
 * @author mederly
 */
public class AbstractTestForExchangeConnector {
	
	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	public static final String DEFAULT_ENDPOINT_URL = "http://localhost.:8080/midpoint/model/model-3";
	
	// Object OIDs

    // Other
    public static final String NS_RI = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
    public static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";

    public static final QName OC_ACCOUNT = new QName(NS_RI, "AccountObjectClass");
    public static final QName OC_ACCEPTED_DOMAIN = new QName(NS_RI, "CustomAcceptedDomainObjectClass");
    public static final QName OC_GLOBAL_ADDRESS_LIST = new QName(NS_RI, "CustomGlobalAddressListObjectClass");
    public static final QName OC_ADDRESS_LIST = new QName(NS_RI, "CustomAddressListObjectClass");
    public static final QName OC_OFFLINE_ADDRESS_BOOK = new QName(NS_RI, "CustomOfflineAddressBookObjectClass");
    public static final QName OC_ADDRESS_BOOK_POLICY = new QName(NS_RI, "CustomAddressBookPolicyObjectClass");
    public static final QName OC_DISTRIBUTION_GROUP = new QName(NS_RI, "CustomDistributionGroupObjectClass");
    public static final QName OC_EMAIL_ADDRESS_POLICY = new QName(NS_RI, "CustomEmailAddressPolicyObjectClass");

    // objects created (to be cleaned up at the end)
    protected List<String> acceptedDomains = new ArrayList<>();
    protected List<String> globalAddressLists = new ArrayList<>();
    protected List<String> addressLists = new ArrayList<>();
    protected List<String> offlineAddressBooks = new ArrayList<>();
    protected List<String> addressBookPolicies = new ArrayList<>();
    protected List<String> distributionGroups = new ArrayList<>();
    protected List<String> emailAddressPolicies = new ArrayList<>();
    protected List<OrgType> orgs = new ArrayList<>();

    protected ModelPortType modelPort;
    protected ObjectDeltaOperationType lastOdo;     // a hack to see last result

    //    private String dn(String givenName, String sn) {
//        return "CN=" + givenName + " " + sn + "," + getContainer();
//    }
//
//    private String mail(String givenName, String sn) {
//        return sn.toLowerCase() + "@" + getMailDomain();
//    }
//
//    private String getContainer() {
//        return System.getProperty("container");
//    }
//
    protected String getResourceOid() {
        return System.getProperty("resourceOid");
    }
//
//    public String getMailDomain() {
//        return System.getProperty("mailDomain");
//    }

    @BeforeClass
	public void initialize() throws Exception {
        modelPort = createModelPort(new String[0]);
    }


    // =============== AcceptedDomain ===============

    protected String createAndCheckAcceptedDomain(String name, String domainName, String domainType) throws Exception {
        System.out.println("Creating accepted domain " + name);
        String oid = createAcceptedDomain(name, domainName, domainType);
        System.out.println("Done; OID = " + oid);
        acceptedDomains.add(oid);
        return checkAcceptedDomain(name, domainName, domainType).getOid();
    }

    protected String createAcceptedDomain(String name, String domainName, String domainType) throws FaultMessage {

        Document doc = ModelClientUtil.getDocumnent();

        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ACCEPTED_DOMAIN);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("custom-accepted-domain");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "DomainName"), domainName, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "DomainType"), domainType, doc));

        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    protected ShadowType checkAcceptedDomain(String name, String domainName, String domainType) throws Exception {
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


    protected String createAndCheckGlobalAddressList(String name, String valueToExpect) throws Exception {
        System.out.println("Creating GAL " + name);
        String oid = createGlobalAddressList(name, valueToExpect);
        System.out.println("Done; OID = " + oid);
        globalAddressLists.add(oid);
        return checkGlobalAddressList(name, galFilter(valueToExpect)).getOid();
    }

    protected String galFilter(String valueToExpect) {
        return "((Alias -ne $null) -and (CustomAttribute1 -eq '" + valueToExpect + "'))";
    }

    protected String createGlobalAddressList(String name, String valueToExpect) throws FaultMessage {

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

    protected ShadowType checkGlobalAddressList(String name, String recipientFilter) throws Exception {
        System.out.println("Retrieving GAL " + name);
        ShadowType shadowType = getShadowByName(getResourceOid(), OC_GLOBAL_ADDRESS_LIST, name);
        AssertJUnit.assertNotNull("GAL " + name + " was not found", shadowType);
        System.out.println("Done; shadow OID = " + shadowType.getOid());
        dumpAttributes(shadowType);
        Map<String,Object> attrs = getAttributesAsMap(shadowType);
        assertAttributeExists(attrs, "uid");
        assertAttributeEquals(attrs, "name", name);
        if (recipientFilter != null) {
            assertAttributeEquals(attrs, "RecipientFilter", recipientFilter);
        }
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

    protected String createAndCheckAddressList(String name, String valueToExpect, AddressListType type) throws Exception {
        System.out.println("Creating AddressList " + name);
        String oid = createAddressList(name, valueToExpect, type);
        System.out.println("Done; OID = " + oid);
        addressLists.add(oid);
        return checkAddressList(name, type.createFilter(valueToExpect)).getOid();
    }

    protected String createAddressList(String name, String valueToExpect, AddressListType type) throws FaultMessage {

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

    protected ShadowType checkAddressList(String name, String recipientFilter) throws Exception {
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

    protected String createAndCheckOfflineAddressBook(String name, String addressList, String tenantName) throws Exception {
        System.out.println("Creating OAB " + name);
        String oid = createOfflineAddressBook(name, addressList, tenantName);
        System.out.println("Done; OID = " + oid);
        offlineAddressBooks.add(oid);
        return checkOfflineAddressBook(name, addressList).getOid();
    }

    protected String createOfflineAddressBook(String name, String addressList, String tenantName) throws FaultMessage {

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
        //attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "_TenantName"), tenantName, doc));
        shadow.setAttributes(attributes);

        return createShadow(shadow);
    }

    protected ShadowType checkOfflineAddressBook(String name, String addressList) throws Exception {
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

    protected String createAndCheckAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws Exception {
        System.out.println("Creating ABP " + name);
        String oid = createAddressBookPolicy(name, addressLists, gal, oab, rooms);
        System.out.println("Done; OID = " + oid);
        addressBookPolicies.add(oid);
        return checkAddressBookPolicy(name, addressLists, gal, oab, rooms).getOid();
    }

    protected String createAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws FaultMessage {

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

    protected ShadowType checkAddressBookPolicy(String name, Collection<String> addressLists, String gal, String oab, String rooms) throws Exception {
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

    protected String distributionGroupName(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    protected String distributionGroupPrimaryAddress(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    protected Collection<String> distributionGroupMembers(String name) {
        //return Arrays.asList("Mail-" + name + "@AllUsers");
        return new ArrayList<>();
    }

    protected String distributionGroupDisplayName(String name) {
        return "Mail-" + name + "@MailSecurity";
    }

    protected String createAndCheckDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws Exception {
        System.out.println("Creating DG " + name);
        String oid = createDistributionGroup(name, primaryAddress, members, ou, displayName, valueForFilter);
        System.out.println("Done; OID = " + oid);
        distributionGroups.add(oid);
        return checkDistributionGroup(name, primaryAddress, members, ou, displayName, valueForFilter).getOid();
    }

    protected String createDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws FaultMessage {

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

    protected ShadowType checkDistributionGroup(String name, String primaryAddress, Collection<String> members, String ou, String displayName, String valueForFilter) throws Exception {
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

    protected void cleanup() throws Exception {
        deleteShadows(distributionGroups, true);
        deleteShadows(emailAddressPolicies, true);
        deleteShadows(addressBookPolicies, true);
        deleteShadows(offlineAddressBooks, true);
        deleteShadows(addressLists, true);
        deleteShadows(globalAddressLists, true);
        deleteShadows(acceptedDomains, true);
        deleteObjects(OrgType.class, orgs, true);
    }

    protected void deleteShadows(List<String> oids, boolean ignoreFailures) throws FaultMessage {
        deleteObjectsByOids(ShadowType.class, oids, ignoreFailures);
    }

    protected void deleteShadow(String oid, boolean ignoreFailures) throws FaultMessage {
        deleteObject(ShadowType.class, oid, ignoreFailures);
    }

    protected void deleteShadowRaw(String oid, boolean ignoreFailures) throws FaultMessage {
        deleteObject(ShadowType.class, oid, ignoreFailures, createRaw());
    }

    protected ModelExecuteOptionsType createRaw() {
        ModelExecuteOptionsType options = new ModelExecuteOptionsType();
        options.setRaw(true);
        return options;
    }

    protected void deleteObjectsByOids(Class objectClass, List<String> oids, boolean ignoreFailures) throws FaultMessage {
        for (String oid : oids) {
            deleteObject(objectClass, oid, ignoreFailures);
        }
    }

    protected void deleteObjects(Class objectClass, List<? extends ObjectType> objects, boolean ignoreFailures) throws FaultMessage {
        for (ObjectType objectType : objects) {
            deleteObject(objectClass, objectType.getOid(), ignoreFailures);
        }
    }

    protected void deleteObject(Class objectClass, String oid, boolean ignoreFailures) throws FaultMessage {
        deleteObject(objectClass, oid, ignoreFailures, new ModelExecuteOptionsType());
    }

    protected void deleteObject(Class objectClass, String oid, boolean ignoreFailures, ModelExecuteOptionsType executeOptionsType) throws FaultMessage {
        System.out.println("Deleting " + objectClass.getSimpleName() + " " + oid);
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(objectClass));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(oid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        if (!ignoreFailures) {
            modelPort.executeChanges(deltaListType, executeOptionsType);
        } else {
            try {
                modelPort.executeChanges(deltaListType, executeOptionsType);
            } catch (Exception e) {
                System.err.println("Cannot remove " + oid + ": " + e.getMessage());
            }
        }
    }

    protected void modifyShadow(String oid, String path, ModificationTypeType modType, Object value) throws Exception {
        modifyObject(ShadowType.class, oid, path, modType, value);
    }

    protected void modifyObject(Class objectType, String oid, String path, ModificationTypeType modType, Object value) throws Exception {
        modifyObject(objectType, oid, path, modType, value, null, true);
    }

    protected ObjectDeltaOperationType modifyObject(Class objectType, String oid, String path, ModificationTypeType modType, Object value, ModelExecuteOptionsType optionsType, boolean assertSuccess) throws Exception {
        System.out.println("Modifying " + objectType.getSimpleName() + " " + oid + " (path: " + path + ")");

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(objectType));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);

        if (path != null) {
            ItemDeltaType itemDelta = new ItemDeltaType();
            itemDelta.setModificationType(modType);
            itemDelta.setPath(createNonDefaultItemPathType(path));
            if (!(value instanceof Collection)) {
                itemDelta.getValue().add(value);
            } else {
                itemDelta.getValue().addAll((Collection) value);
            }
            deltaType.getItemDelta().add(itemDelta);
        }

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType odolist = modelPort.executeChanges(deltaListType, optionsType);
        return assertExecuteChangesSuccess(odolist, deltaType, assertSuccess);
    }

    // values: path -> value or collection of values
    protected ObjectDeltaOperationType modifyObject(Class objectType, String oid, ModificationTypeType modType, Map<String,Object> values, ModelExecuteOptionsType optionsType, boolean assertSuccess) throws Exception {
        System.out.println("Modifying " + objectType.getSimpleName() + " " + oid + " (values: " + values + ")");

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(objectType));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);

        for (Map.Entry<String,Object> entry : values.entrySet()) {
            ItemDeltaType itemDelta = new ItemDeltaType();
            itemDelta.setModificationType(modType);
            itemDelta.setPath(createNonDefaultItemPathType(entry.getKey()));
            if (!(entry.getValue() instanceof Collection)) {
                itemDelta.getValue().add(entry.getValue());
            } else {
                itemDelta.getValue().addAll((Collection) (entry.getValue()));
            }
            deltaType.getItemDelta().add(itemDelta);
        }

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType odolist = modelPort.executeChanges(deltaListType, optionsType);
        return assertExecuteChangesSuccess(odolist, deltaType, assertSuccess);
    }

    protected ObjectDeltaOperationType assertExecuteChangesSuccess(ObjectDeltaOperationListType odolist, ObjectDeltaType deltaType, boolean assertSuccess) {
        ObjectDeltaOperationType found = null;
        for (ObjectDeltaOperationType odo : odolist.getDeltaOperation()) {
            if (deltaType == null || deltaType.getOid().equals(odo.getObjectDelta().getOid())) {
                AssertJUnit.assertNotNull("Operation result is null", odo.getExecutionResult());
                found = odo;
                if (odo.getExecutionResult().getStatus() != OperationResultStatusType.SUCCESS) {
                    System.out.println("!!! Operation result is " + odo.getExecutionResult().getStatus());
                    System.out.println("!!! Message: " + odo.getExecutionResult().getMessage());
                    System.out.println("!!! Details:\n" + odo.getExecutionResult());
                    if (assertSuccess) {
                        AssertJUnit.assertEquals("Unexpected operation result status", OperationResultStatusType.SUCCESS, odo.getExecutionResult().getStatus());
                    }
                }
            }
        }
        AssertJUnit.assertNotNull("ObjectDelta was not found in ObjectDeltaOperationList", found);
        return found;
    }

    protected void assertAttributeExists(Map<String, Object> attrs, String name) {
        AssertJUnit.assertTrue("Attribute " + name + " is missing", attrs.containsKey(name));
    }

    protected ShadowType checkAccount(String givenName, String sn, String dn, String container) throws Exception {
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
        assertAttributeEquals(attrs, "ad_container", container);
        assertAttributeEquals(attrs, "objectClass", new HashSet(Arrays.asList("top", "person", "organizationalPerson", "user")));
        return shadowType;
	}

    protected void assertAttributeEquals(Map<String, Object> attrs, String name, Object expectedValue) {
        Object realValue = attrs.get(name);
        AssertJUnit.assertEquals("Unexpected value of attribute " + name, expectedValue, realValue);
    }

    protected void assertAttributeContains(Map<String, Object> attrs, String name, Object expectedValue) {
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

    protected void dumpAttributes(ShadowType shadowType) {
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

    protected Map<String,Object> getAttributesAsMap(ShadowType shadowType) {
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

    protected void put(Map<String, Object> map, String name, Object value) {
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
    public static String getOrig(PolyStringType polyStringType) {
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

    public static void dump(Collection<? extends ObjectType> objects) {
        System.out.println("Objects returned: " + objects.size());
        for (ObjectType objectType : objects) {
            System.out.println(" - " + getOrig(objectType.getName()) + ": " + objectType);
        }
    }

    protected SystemConfigurationType getConfiguration() throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		
		modelPort.getObject(ModelClientUtil.getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
                objectHolder, resultHolder);
		
		return (SystemConfigurationType) objectHolder.value;
	}

    protected Collection<ResourceType> listResources() throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

		modelPort.searchObjects(ModelClientUtil.getTypeQName(ResourceType.class), null, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}

    protected ResourceType getResource(String oid) throws SAXException, IOException, FaultMessage {
        return getObject(ResourceType.class, oid);
    }

    protected RoleType getRole(String oid) throws SAXException, IOException, FaultMessage {
        return getObject(RoleType.class, oid);
    }

    protected <T extends ObjectType> T getObject(Class<T> clazz, String oid) throws SAXException, IOException, FaultMessage {
        return getObject(clazz, oid, new SelectorQualifiedGetOptionsType());
    }

    protected <T extends ObjectType> T getObjectNoFetch(Class<T> clazz, String oid) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        SelectorQualifiedGetOptionType option = new SelectorQualifiedGetOptionType();
        GetOperationOptionsType getOptions = new GetOperationOptionsType();
        getOptions.setNoFetch(true);
        option.setOptions(getOptions);
        options.getOption().add(option);
        return getObject(clazz, oid, options);
    }

    protected <T extends ObjectType> T getObject(Class<T> clazz, String oid, SelectorQualifiedGetOptionsType options) throws SAXException, IOException, FaultMessage {
        Holder<ObjectType> objectHolder = new Holder<>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.getObject(ModelClientUtil.getTypeQName(clazz), oid, options, objectHolder, resultHolder);

        return (T) objectHolder.value;
    }

    protected Collection<UserType> listUsers() throws SAXException, IOException, FaultMessage {
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

    protected Collection<TaskType> listTasks() throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType operationOptions = new SelectorQualifiedGetOptionsType();

        // Let's say we want to retrieve tasks' next scheduled time (because this may be a costly operation if
        // JDBC based quartz scheduler is used, the fetching of this attribute has to be explicitly requested)
        SelectorQualifiedGetOptionType getNextScheduledTimeOption = new SelectorQualifiedGetOptionType();

        // prepare a selector (described by path) + options (saying to retrieve that attribute)
        OptionObjectSelectorType selector = new OptionObjectSelectorType();
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

    protected String createUserGuybrush(RoleType role) throws FaultMessage {
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

    protected String createOrg(OrgType orgType) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(OrgType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(orgType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
        return ModelClientUtil.getOidFromDeltaOperationList(operationListType, deltaType);
    }


    protected String createAccount(String givenName, String sn, String name, String recipientType, String overrideMail, String samAccountName, String upn, Map<String,Object> additionalAttributes, boolean assertSuccess) throws FaultMessage {
        ShadowType user = prepareShadowType(givenName, sn, name, recipientType, overrideMail, samAccountName, upn, additionalAttributes);
        return createObject(ShadowType.class, user, null, assertSuccess);
    }

    protected String createAccount(String givenName, String sn, String name, String recipientType, String overrideMail, String samAccountName, String upn, boolean assertSuccess) throws FaultMessage {
        ShadowType user = prepareShadowType(givenName, sn, name, recipientType, overrideMail, samAccountName, upn, null);
        return createObject(ShadowType.class, user, null, assertSuccess);
    }

    protected String createAccount(String givenName, String sn, String name, String recipientType, String overrideMail) throws FaultMessage {
        ShadowType user = prepareShadowType(givenName, sn, name, recipientType, overrideMail, null, null, null);
        return createShadow(user);
    }

    protected ObjectDeltaOperationType createAccountOdo(String givenName, String sn, String name, String recipientType, String overrideMail) throws FaultMessage {
        ShadowType shadowType = prepareShadowType(givenName, sn, name, recipientType, overrideMail, null, null, null);

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(ShadowType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(shadowType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
        return ModelClientUtil.findInDeltaOperationList(operationListType, deltaType);
    }

    // additionalAttributes: e.g. "OfflineAddressBook" -> "OAB123"
    private ShadowType prepareShadowType(String givenName, String sn, String name, String recipientType, String overrideMail, String samAccountName,
                                         String upn, Map<String, Object> additionalAttributes) {
        if (samAccountName == null) {
            samAccountName = sn.toLowerCase();
        }
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
        if (overrideMail != null) {
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "EmailAddressPolicyEnabled"), "true", doc));
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "EmailAddresses"), overrideMail, doc));
        }
        if (upn != null) {
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, "userPrincipalName"), upn, doc));
        }
        if (additionalAttributes != null) {
            for (Map.Entry<String,Object> entry : additionalAttributes.entrySet()) {
                if (entry.getValue() instanceof Collection) {
                    for (Object value : (Collection) entry.getValue()) {
                        addAttribute(attributes, entry.getKey(), value, doc);
                    }
                } else {
                    addAttribute(attributes, entry.getKey(), entry.getValue(), doc);
                }
            }
        }
        user.setAttributes(attributes);
        return user;
    }

    private void addAttribute(ShadowAttributesType attributes, String name, Object value, Document doc) {
        if (value != null) {
            attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_RI, name), value.toString(), doc));
        }
    }


    protected ShadowType getShadowByName(String resourceOid, QName objectClass, String name) throws JAXBException, SAXException, IOException, FaultMessage {
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
    public static ObjectReferenceType createObjectReferenceType(Class<? extends ObjectType> typeClass, String oid) {
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(oid);
        ort.setType(ModelClientUtil.getTypeQName(typeClass));
        return ort;
    }

    protected String createUserFromSystemResource(String resourcePath) throws FileNotFoundException, JAXBException, FaultMessage {
		UserType user = unmarshallResource(resourcePath);
		
		return createUser(user);
	}

    protected static <T> T unmarshallFile(File file) throws JAXBException, FileNotFoundException {
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

    protected static <T> T unmarshallResource(String path) throws JAXBException, FileNotFoundException {
		JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
		Unmarshaller unmarshaller = jc.createUnmarshaller(); 
		 
		InputStream is = null;
		JAXBElement<T> element = null;
		try {
			is = AbstractTestForExchangeConnector.class.getClassLoader().getResourceAsStream(path);
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

    protected String createUser(UserType userType) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(userType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
		ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
		String oid = ModelClientUtil.getOidFromDeltaOperationList(operationListType, deltaType);
        return oid;
	}

    protected String createShadow(ShadowType shadowType) throws FaultMessage {
        return createShadow(shadowType, null);
    }

    protected String createShadow(ShadowType shadowType, ModelExecuteOptionsType options) throws FaultMessage {
        return createObject(ShadowType.class, shadowType, options);
    }

    protected String createObject(Class clazz, ObjectType objectType, ModelExecuteOptionsType options) throws FaultMessage {
        return createObject(clazz, objectType, options, true);
    }

    protected String createObject(Class clazz, ObjectType objectType, ModelExecuteOptionsType options, boolean assertSuccess) throws FaultMessage {

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(clazz));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(objectType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, options);
        lastOdo = assertExecuteChangesSuccess(operationListType, null, assertSuccess);
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

    protected void changeUserPassword(String oid, String newPassword) throws FaultMessage {
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

    protected void changeUserGivenName(String oid, String newValue) throws FaultMessage {
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

    protected boolean assignRoles(Class clazz, String oid, String... roleOids) throws FaultMessage {
		return modifyRoleAssignment(clazz, oid, true, roleOids);
	}

    protected void unassignRoles(Class clazz, String oid, String... roleOids) throws FaultMessage {
		modifyRoleAssignment(clazz, oid, false, roleOids);
	}

    protected boolean modifyRoleAssignment(Class clazz, String oid, boolean isAdd, String... roleOids) throws FaultMessage {
		ItemDeltaType assignmentDelta = new ItemDeltaType();
		if (isAdd) {
			assignmentDelta.setModificationType(ModificationTypeType.ADD);
		} else {
			assignmentDelta.setModificationType(ModificationTypeType.DELETE);
		}
        assignmentDelta.setPath(ModelClientUtil.createItemPathType("assignment"));
		for (String roleOid: roleOids) {
            AssertJUnit.assertNotNull("role OID is null", roleOid);
			assignmentDelta.getValue().add(createRoleAssignment(roleOid));
		}

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(clazz));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);
        deltaType.getItemDelta().add(assignmentDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(deltaListType, null);
        Boolean success = null;
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (oid.equals(objectDeltaOperation.getObjectDelta().getOid())) {
                if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                    System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
                    success = false;
                } else {
                    if (success == null) {
                        success = true;
                    }
                }
            }
        }
        return Boolean.TRUE.equals(success);
	}

    protected AssignmentType createRoleAssignment(String roleOid) {
		AssignmentType roleAssignment = new AssignmentType();
		ObjectReferenceType roleRef = new ObjectReferenceType();
		roleRef.setOid(roleOid);
		roleRef.setType(ModelClientUtil.getTypeQName(RoleType.class));
		roleAssignment.setTargetRef(roleRef);
		return roleAssignment;
	}

    protected UserType searchUserByName(String username) throws SAXException, IOException, FaultMessage, JAXBException {
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

    protected RoleType searchRoleByName(String roleName) throws SAXException, IOException, FaultMessage, JAXBException {
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

    protected Collection<RoleType> listRequestableRoles() throws SAXException, IOException, FaultMessage, JAXBException {
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

    protected void deleteUser(String oid) throws FaultMessage {
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

    protected void deleteTask(String oid) throws FaultMessage {
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
//        ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
		ModelService modelService = new ModelService();
		ModelPortType modelPort = modelService.getModelPort();
		BindingProvider bp = (BindingProvider)modelPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);

        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setReceiveTimeout(300000L);
        http.setClient(httpClientPolicy);

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

    protected String getDebugName(ObjectType objectType) {
        return objectType.getClass().getSimpleName() + " " + getOrig(objectType.getName()) + " (" + objectType.getOid() + ")";
    }

}
