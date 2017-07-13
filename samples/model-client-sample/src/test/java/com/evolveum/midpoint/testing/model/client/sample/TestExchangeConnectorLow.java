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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
public class TestExchangeConnectorLow extends AbstractTestForExchangeConnector {
	
    private static final String NEWTON_GIVEN_NAME = "Isaac";
    private static final String NEWTON_SN = "Newton";
    private String newtonOid;

    private static final String LEIBNIZ_GIVEN_NAME = "Gottfried Wilhelm";
    private static final String LEIBNIZ_SN = "Leibniz";
    private String leibnizOid;

    private static final String PASCAL_GIVEN_NAME = "Blaise";
    private static final String PASCAL_SN = "Pascal";
    private String pascalOid;

    private static final String HUYGENS_GIVEN_NAME = "Christiaan";
    private static final String HUYGENS_SN = "Huygens";
    private String huygensOid;

    private String dn(String givenName, String sn) {
        return "CN=" + givenName + " " + sn + "," + getContainer();
    }

    private String mail(String givenName, String sn) {
        return sn.toLowerCase() + "@" + getMailDomain();
    }

    private String getContainer() {
        return System.getProperty("container");
    }
    
    public String getMailDomain() {
        return System.getProperty("mailDomain");
    }

    @Test
    public void test001GetResource() throws Exception {
        System.out.println("Getting Exchange resource...");
        ResourceType exchangeResource = getResource(getResourceOid());
        AssertJUnit.assertNotNull("Exchange resource was not found", exchangeResource);
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
        deleteShadow(oid, false);
        acceptedDomains.remove(oid);
        ShadowType tempDomain = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, "Temporary domain");
        AssertJUnit.assertNull("Temporary domain was not removed", tempDomain);
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
        deleteShadow(oid, false);
        globalAddressLists.remove(oid);
        ShadowType tempGal = getShadowByName(getResourceOid(), OC_GLOBAL_ADDRESS_LIST, "Temporary GAL");
        AssertJUnit.assertNull("Temporary GAL was not removed", tempGal);
    }

    // =============== Address Lists ===============

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
        deleteShadow(oid, false);
        addressLists.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_ADDRESS_LIST, "Temporary Address List");
        AssertJUnit.assertNull("Temporary AddressList was not removed", temp);
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

        deleteShadow(tempGal, false);
        globalAddressLists.remove(tempGal);
    }

    @Test
    public void test045CreateAndDeleteOfflineAddressBook() throws Exception {
        String oid = createAndCheckOfflineAddressBook("Temporary OAB", "Scientists Global Address List Updated", "Scientists");
        deleteShadow(oid, false);
        offlineAddressBooks.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_OFFLINE_ADDRESS_BOOK, "Temporary OAB");
        AssertJUnit.assertNull("Temporary OAB was not removed", temp);
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

        deleteShadow(oid, false);
        addressBookPolicies.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_ADDRESS_BOOK_POLICY, "Temporary Address Book Policy");
        AssertJUnit.assertNull("Temporary Address Book Policy was not removed", temp);
    }

    // =============== DistributionGroup ===============

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

        deleteShadow(oid, false);
        distributionGroups.remove(oid);
        ShadowType temp = getShadowByName(getResourceOid(), OC_DISTRIBUTION_GROUP, distributionGroupName(n));
        AssertJUnit.assertNull("Temporary DistributionGroup was not removed", temp);
    }

    // =============== Users ===============

    @Test
    public void test110CreateNewton() throws Exception {
        System.out.println("Creating account for Newton...");
        newtonOid = createAccount(NEWTON_GIVEN_NAME, NEWTON_SN, dn(NEWTON_GIVEN_NAME, NEWTON_SN), "User", null);
        System.out.println("Done; OID = " + newtonOid);
    }

    @Test
    public void test112GetNewton() throws Exception {
        ShadowType newton = checkAccount(NEWTON_GIVEN_NAME, NEWTON_SN, dn(NEWTON_GIVEN_NAME, NEWTON_SN), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(newton);
        assertAttributeEquals(attrs, "RecipientType", "User");
    }

    @Test
    public void test120CreateLeibniz() throws Exception {
        System.out.println("Creating account for Leibniz...");
        leibnizOid = createAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, dn(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN), "UserMailbox", null);
        System.out.println("Done; OID = " + leibnizOid);
    }

    @Test
    public void test122GetLeibniz() throws Exception {
        String mail = mail(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN);
        ShadowType leibniz = checkAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, dn(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(leibniz);
        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
        assertAttributeExists(attrs, "Database");
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
    public void test124ModifyLeibnizAddOabAndAbp() throws Exception {
        Map<String,Object> values = new HashMap<>();
        values.put("attributes/OfflineAddressBook", "Scientists Offline Address Book Updated");
        values.put("attributes/AddressBookPolicy", "Scientists Address Book Policy Updated");
        modifyObject(ShadowType.class, leibnizOid, ModificationTypeType.REPLACE, values, null, true);
    }

    @Test
    public void test126GetLeibnizAgain() throws Exception {
        String mail = mail(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN);
        ShadowType leibniz = checkAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, dn(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(leibniz);
        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
        assertAttributeExists(attrs, "Database");
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
        assertAttributeEquals(attrs, "mail", mail);
        assertAttributeEquals(attrs, "Alias", LEIBNIZ_SN.toLowerCase());
        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", LEIBNIZ_GIVEN_NAME + " " + LEIBNIZ_SN);
        assertAttributeEquals(attrs, "OfflineAddressBook", "Scientists Offline Address Book Updated");
        assertAttributeEquals(attrs, "AddressBookPolicy", "Scientists Address Book Policy Updated");
    }

    @Test
    public void test130CreatePascal() throws Exception {
        System.out.println("Creating account for Pascal...");
        Map<String,Object> values = new HashMap<>();
        values.put("OfflineAddressBook", "Scientists Offline Address Book Updated");
        values.put("AddressBookPolicy", "Scientists Address Book Policy Updated");
        pascalOid = createAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), "UserMailbox", null, null, null, values, true);
        System.out.println("Done; OID = " + pascalOid);

        String mail = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
        assertAttributeExists(attrs, "Database");
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
        assertAttributeEquals(attrs, "mail", mail);
        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
        assertAttributeEquals(attrs, "OfflineAddressBook", "Scientists Offline Address Book Updated");
        assertAttributeEquals(attrs, "AddressBookPolicy", "Scientists Address Book Policy Updated");
    }

    @Test
    public void test132AddSecondaryAddressToPascal() throws Exception {
        String mail1 = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
        String mail2 = "pascal@clermont-ferrand.fr";

        System.out.println("Setting new secondary address to Pascal...");
        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.ADD, Arrays.asList("smtp:" + mail1, "smtp:" + mail2));        // first one is actually a duplicate
        System.out.println("Done");

        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
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
        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "false");

        System.out.println("Setting new email addresses for Pascal...");
        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.REPLACE, new HashSet<>(Arrays.asList("smtp:" + mail1, "SMTP:" + mail2)));
        System.out.println("Done");

        pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
        attrs = getAttributesAsMap(pascal);
        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail2);
        assertAttributeEquals(attrs, "mail", mail2);
        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
        assertAttributeEquals(attrs, "EmailAddresses", new HashSet<>(Arrays.asList("SMTP:"+mail2, "smtp:"+mail1)));
        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
    }

    @Test
    public void test140AssignConflictingAddress() throws Exception {
        String mail = "pascal@clermont-ferrand.fr";

        System.out.println("Disabling email address policy for Leibniz...");
        modifyShadow(leibnizOid, "attributes/EmailAddressPolicyEnabled", ModificationTypeType.REPLACE, false);
        System.out.println("Done");

        System.out.println("Adding conflicting email addresses to Leibniz...");
        ObjectDeltaOperationType result = modifyObject(ShadowType.class, leibnizOid, "attributes/EmailAddresses", ModificationTypeType.ADD, "smtp:" + mail, null, false);
        System.out.println("Done; result = " + result.getExecutionResult().getStatus() + " / " + result.getExecutionResult().getMessage());

        AssertJUnit.assertEquals("Unexpected operation status when adding conflicting address", OperationResultStatusType.FATAL_ERROR, result.getExecutionResult().getStatus());
    }

    @Test
    public void test150CreateHuygensConflicting() throws Exception {
        String mail = "pascal@clermont-ferrand.fr";

        System.out.println("Creating account for Huygens...");
        ObjectDeltaOperationType odo = createAccountOdo(HUYGENS_GIVEN_NAME, HUYGENS_SN, dn(HUYGENS_GIVEN_NAME, HUYGENS_SN), "UserMailbox", mail);
        OperationResultType result = odo.getExecutionResult();
        System.out.println("Done; status = " + result.getStatus() + ":" + result.getMessage());

//        ShadowType huygens = checkAccount(HUYGENS_GIVEN_NAME, HUYGENS_SN, dn(HUYGENS_GIVEN_NAME, HUYGENS_SN), getContainer());
//        Map<String,Object> attrs = getAttributesAsMap(pascal);
//        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
//        assertAttributeExists(attrs, "homeMDB");
//        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
//        assertAttributeEquals(attrs, "mail", mail);
//        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
//        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
//        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
//        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
//        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
//        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
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

    @Test
    public void test210ModifyingNonexistingPowerShellObject() throws Exception {

        // create shadow with non-existing GUID
        System.out.println("Creating shadow with non-existing GUID...");
        Document doc = ModelClientUtil.getDocumnent();

        String name = "Wrong GUID shadow";
        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ACCEPTED_DOMAIN);
        shadow.setKind(ShadowKindType.GENERIC);
        shadow.setIntent("custom-accepted-domain");

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "uid"), "wrong-GUID", doc));
        shadow.setAttributes(attributes);

        String oid = createObject(ShadowType.class, shadow, createRaw());

        System.out.println("Done, reading it back...");
        ShadowType shadowReadAgain = getObjectNoFetch(ShadowType.class, oid);
        dumpAttributes(shadowReadAgain);

        System.out.println("Now launching modifyObject operation...");
        //Class objectType, String oid, String path, ModificationTypeType modType, Object value, ModelExecuteOptionsType optionsType, boolean assertSuccess
        ObjectDeltaOperationType odo = modifyObject(ShadowType.class, oid, "attributes/DomainType", ModificationTypeType.REPLACE, "InternalRelay", null, false);
        OperationResultType r = odo.getExecutionResult();
        System.out.println("Done: " + r.getStatus() + ":" + r.getMessage());

        OperationResultType found = findOperationResult(r, new OperationResultMatcher() {
            @Override
            public boolean match(OperationResultType r) {
                return r.getDetails() != null && r.getDetails().contains("UnknownUidException");
            }
        });
        AssertJUnit.assertNotNull("UnknownUidException was not detected", found);
        System.out.println("======================================================================================================");
        System.out.println("Details: " + found.getDetails());
    }

    private OperationResultType findOperationResult(OperationResultType result, OperationResultMatcher matcher) {
        if (matcher.match(result)) {
            return result;
        }
        for (OperationResultType r : result.getPartialResults()) {
            OperationResultType found = findOperationResult(r, matcher);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    public void test220ModifyingNonexistingAccount() throws Exception {

        // create shadow with non-existing GUID
        System.out.println("Creating shadow with non-existing GUID...");
        Document doc = ModelClientUtil.getDocumnent();

        String name = "Wrong GUID shadow";
        ShadowType shadow = new ShadowType();
        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
        shadow.setObjectClass(OC_ACCOUNT);
        shadow.setKind(ShadowKindType.ACCOUNT);

        ShadowAttributesType attributes = new ShadowAttributesType();
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "uid"), "CN=wrong-GUID," + getContainer(), doc));
        shadow.setAttributes(attributes);

        String oid = createObject(ShadowType.class, shadow, createRaw());

        System.out.println("Done, reading it back...");
        ShadowType shadowReadAgain = getObjectNoFetch(ShadowType.class, oid);
        dumpAttributes(shadowReadAgain);

        System.out.println("Now launching modifyObject operation...");
        ObjectDeltaOperationType odo = modifyObject(ShadowType.class, oid, "attributes/sn", ModificationTypeType.REPLACE, "xxxxxx", null, false);
        OperationResultType r = odo.getExecutionResult();
        System.out.println("Done: " + r.getStatus() + ":" + r.getMessage());

        OperationResultType found = findOperationResult(r, new OperationResultMatcher() {
            @Override
            public boolean match(OperationResultType r) {
                return r.getDetails() != null && r.getDetails().contains("UnknownUidException");
            }
        });
        AssertJUnit.assertNotNull("UnknownUidException was not detected", found);
        System.out.println("======================================================================================================");
        System.out.println("Details: " + found.getDetails());
    }


    @Test
    public void test900Cleanup() throws Exception {
        deleteObject(ShadowType.class, newtonOid, true);
        deleteObject(ShadowType.class, leibnizOid, true);
        deleteObject(ShadowType.class, pascalOid, true);
        deleteObject(ShadowType.class, huygensOid, true);
        cleanup();
    }

    @FunctionalInterface
    private interface OperationResultMatcher {
        boolean match(OperationResultType operationResultType);
    }
}
