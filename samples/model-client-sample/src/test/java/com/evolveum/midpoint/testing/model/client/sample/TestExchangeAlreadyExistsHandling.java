/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertTrue;

/**
 * See documentation for TestExchangeConnectorLow.
 *
 *
 * @author mederly
 *
 */
public class TestExchangeAlreadyExistsHandling extends AbstractTestForExchangeConnector {
	
    private String orvilleOid;
    private String wilburOid;

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

    // =============== Users ===============

    @Test
    public void test110CreateOrvilleWright() throws Exception {
        System.out.println("Creating account for Orville Wright...");
        orvilleOid = createAccount("Orville", "Wright", dn("Orville", "Wright"), "User", null, "wright", "wright@test.uniba.local", true);
        System.out.println("Done; OID = " + orvilleOid);
    }

    @Test
    public void test112GetOrville() throws Exception {
        ShadowType orville = checkAccount("Orville", "Wright", dn("Orville", "Wright"), getContainer());
        Map<String,Object> attrs = getAttributesAsMap(orville);
        assertAttributeEquals(attrs, "RecipientType", "User");
    }

    @Test
    public void test120CreateWilburWrightConflictOnSamAccountName() throws Exception {
        System.out.println("Creating account for Wilbur Wright, should create a conflict on samAccountName...");
        wilburOid = createAccount("Wilbur", "Wright", dn("Wilbur", "Wright"), "User", null, "wright", null, false);
        System.out.println("Done; OID = " + wilburOid);
        OperationResultType result = lastOdo.getExecutionResult();
        String message = result.getMessage();
        assertTrue("Result should not be SUCCESS", result.getStatus() != OperationResultStatusType.SUCCESS);
        assertTrue("Message should contain AlreadyExistsException; it is " + message + " instead",
                message.contains("AlreadyExistsException"));
    }

    @Test
    public void test130CreateOrvilleAgainConflictOnDistinguishedName() throws Exception {
        System.out.println("Creating account for Orville Wright, should create a conflict on distinguishedName...");
        String oid = createAccount("Orville", "Wright", dn("Orville", "Wright"), "User", null, "wright1", null, false);
        System.out.println("Done; OID = " + oid);
        OperationResultType result = lastOdo.getExecutionResult();
        String message = result.getMessage();
        assertTrue("Result should not be SUCCESS", result.getStatus() != OperationResultStatusType.SUCCESS);
        assertTrue("Message should contain AlreadyExistsException; it is " + message + " instead",
                message.contains("AlreadyExistsException"));
    }

    @Test
    public void test140CreateWilburWrightConflictOnUserPrincipalName() throws Exception {
        System.out.println("Creating account for Wilbur Wright, should create a conflict on userPrincipalName...");
        String oid = createAccount("Wilbur", "Wright", dn("Wilbur", "Wright"), "User", null, "wright1", "wright@test.uniba.local", false);
        System.out.println("Done; OID = " + oid);
        OperationResultType result = lastOdo.getExecutionResult();
        String message = result.getMessage();
        assertTrue("Result should not be SUCCESS", result.getStatus() != OperationResultStatusType.SUCCESS);
        assertTrue("Message should contain AlreadyExistsException; it is " + message + " instead",
                message.contains("AlreadyExistsException"));
    }


//    @Test
//    public void test120CreateLeibniz() throws Exception {
//        System.out.println("Creating account for Leibniz...");
//        leibnizOid = createAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, dn(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN), "UserMailbox", null);
//        System.out.println("Done; OID = " + leibnizOid);
//    }
//
//    @Test
//    public void test122GetLeibniz() throws Exception {
//        String mail = mail(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN);
//        ShadowType leibniz = checkAccount(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN, dn(LEIBNIZ_GIVEN_NAME, LEIBNIZ_SN), getContainer());
//        Map<String,Object> attrs = getAttributesAsMap(leibniz);
//        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
//        assertAttributeExists(attrs, "Database");
//        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
//        assertAttributeEquals(attrs, "mail", mail);
//        assertAttributeEquals(attrs, "Alias", LEIBNIZ_SN.toLowerCase());
//        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
//        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
//        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
//        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
//        assertAttributeEquals(attrs, "displayName", LEIBNIZ_GIVEN_NAME + " " + LEIBNIZ_SN);
//    }
//
//    @Test
//    public void test130CreatePascal() throws Exception {
//        System.out.println("Creating account for Pascal...");
//        pascalOid = createAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), "UserMailbox", null);
//        System.out.println("Done; OID = " + pascalOid);
//
//        String mail = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
//        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
//        Map<String,Object> attrs = getAttributesAsMap(pascal);
//        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
//        assertAttributeExists(attrs, "Database");
//        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
//        assertAttributeEquals(attrs, "mail", mail);
//        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
//        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
//        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
//        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
//        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
//        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
//    }
//
//    @Test
//    public void test132AddSecondaryAddressToPascal() throws Exception {
//        String mail1 = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
//        String mail2 = "pascal@clermont-ferrand.fr";
//
//        System.out.println("Setting new secondary address to Pascal...");
//        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.ADD, Arrays.asList("smtp:" + mail1, "smtp:" + mail2));        // first one is actually a duplicate
//        System.out.println("Done");
//
//        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
//        Map<String,Object> attrs = getAttributesAsMap(pascal);
//        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail1);
//        assertAttributeEquals(attrs, "mail", mail1);
//        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
//        assertAttributeContains(attrs, "EmailAddresses", new HashSet<>(Arrays.asList("SMTP:" + mail1, "smtp:" + mail2)));           // FIXME
//        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
//        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
//        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
//        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
//    }
//
//    @Test
//    public void test134SwapAddressesForPascal() throws Exception {
//        String mail1 = mail(PASCAL_GIVEN_NAME, PASCAL_SN);
//        String mail2 = "pascal@clermont-ferrand.fr";
//
//        System.out.println("Disabling email address policy for Pascal...");
//        modifyShadow(pascalOid, "attributes/EmailAddressPolicyEnabled", ModificationTypeType.REPLACE, false);
//        System.out.println("Done");
//        ShadowType pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
//        Map<String,Object> attrs = getAttributesAsMap(pascal);
//        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "false");
//
//        System.out.println("Setting new email addresses for Pascal...");
//        modifyShadow(pascalOid, "attributes/EmailAddresses", ModificationTypeType.REPLACE, new HashSet<>(Arrays.asList("smtp:" + mail1, "SMTP:" + mail2)));
//        System.out.println("Done");
//
//        pascal = checkAccount(PASCAL_GIVEN_NAME, PASCAL_SN, dn(PASCAL_GIVEN_NAME, PASCAL_SN), getContainer());
//        attrs = getAttributesAsMap(pascal);
//        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail2);
//        assertAttributeEquals(attrs, "mail", mail2);
//        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
//        assertAttributeEquals(attrs, "EmailAddresses", new HashSet<>(Arrays.asList("SMTP:"+mail2, "smtp:"+mail1)));
//        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
//        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
//        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
//    }
//
//    @Test
//    public void test140AssignConflictingAddress() throws Exception {
//        String mail = "pascal@clermont-ferrand.fr";
//
//        System.out.println("Disabling email address policy for Leibniz...");
//        modifyShadow(leibnizOid, "attributes/EmailAddressPolicyEnabled", ModificationTypeType.REPLACE, false);
//        System.out.println("Done");
//
//        System.out.println("Adding conflicting email addresses to Leibniz...");
//        ObjectDeltaOperationType result = modifyObject(ShadowType.class, leibnizOid, "attributes/EmailAddresses", ModificationTypeType.ADD, "smtp:" + mail, null, false);
//        System.out.println("Done; result = " + result.getExecutionResult().getStatus() + " / " + result.getExecutionResult().getMessage());
//
//        AssertJUnit.assertEquals("Unexpected operation status when adding conflicting address", OperationResultStatusType.FATAL_ERROR, result.getExecutionResult().getStatus());
//    }
//
//    @Test
//    public void test150CreateHuygensConflicting() throws Exception {
//        String mail = "pascal@clermont-ferrand.fr";
//
//        System.out.println("Creating account for Huygens...");
//        ObjectDeltaOperationType odo = createAccountOdo(HUYGENS_GIVEN_NAME, HUYGENS_SN, dn(HUYGENS_GIVEN_NAME, HUYGENS_SN), "UserMailbox", mail);
//        OperationResultType result = odo.getExecutionResult();
//        System.out.println("Done; status = " + result.getStatus() + ":" + result.getMessage());
//
////        ShadowType huygens = checkAccount(HUYGENS_GIVEN_NAME, HUYGENS_SN, dn(HUYGENS_GIVEN_NAME, HUYGENS_SN), getContainer());
////        Map<String,Object> attrs = getAttributesAsMap(pascal);
////        assertAttributeEquals(attrs, "RecipientType", "UserMailbox");
////        assertAttributeExists(attrs, "homeMDB");
////        assertAttributeEquals(attrs, "PrimarySmtpAddress", mail);
////        assertAttributeEquals(attrs, "mail", mail);
////        assertAttributeEquals(attrs, "Alias", PASCAL_SN.toLowerCase());
////        assertAttributeContains(attrs, "EmailAddresses", "SMTP:" + mail);               // FIXME
////        assertAttributeEquals(attrs, "EmailAddressPolicyEnabled", "true");
////        assertAttributeEquals(attrs, "msExchRecipientDisplayType", "1073741824");
////        assertAttributeEquals(attrs, "msExchRecipientTypeDetails", "1");
////        assertAttributeEquals(attrs, "displayName", PASCAL_GIVEN_NAME + " " + PASCAL_SN);
//    }
//
//    // non-existing objects
//    @Test
//    public void test200FetchingNonexistingPowerShellObject() throws Exception {
//        ShadowType domain = getShadowByName(getResourceOid(), OC_ACCEPTED_DOMAIN, "Non-existing domain");
//        AssertJUnit.assertNull("Non-existing domain was found somehow", domain);
//    }
//
//    @Test
//    public void test201FetchingNonexistingAccount() throws Exception {
//        ShadowType account = getShadowByName(getResourceOid(), OC_ACCOUNT, "Non-existing user account");
//        AssertJUnit.assertNull("Non-existing account was found somehow", account);
//    }
//
//    @Test
//    public void test210ModifyingNonexistingPowerShellObject() throws Exception {
//
//        // create shadow with non-existing GUID
//        System.out.println("Creating shadow with non-existing GUID...");
//        Document doc = ModelClientUtil.getDocumnent();
//
//        String name = "Wrong GUID shadow";
//        ShadowType shadow = new ShadowType();
//        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
//        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
//        shadow.setObjectClass(OC_ACCEPTED_DOMAIN);
//        shadow.setKind(ShadowKindType.GENERIC);
//        shadow.setIntent("custom-accepted-domain");
//
//        ShadowAttributesType attributes = new ShadowAttributesType();
//        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
//        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "uid"), "wrong-GUID", doc));
//        shadow.setAttributes(attributes);
//
//        String oid = createObject(ShadowType.class, shadow, createRaw());
//
//        System.out.println("Done, reading it back...");
//        ShadowType shadowReadAgain = getObjectNoFetch(ShadowType.class, oid);
//        dumpAttributes(shadowReadAgain);
//
//        System.out.println("Now launching modifyObject operation...");
//        //Class objectType, String oid, String path, ModificationTypeType modType, Object value, ModelExecuteOptionsType optionsType, boolean assertSuccess
//        ObjectDeltaOperationType odo = modifyObject(ShadowType.class, oid, "attributes/DomainType", ModificationTypeType.REPLACE, "InternalRelay", null, false);
//        OperationResultType r = odo.getExecutionResult();
//        System.out.println("Done: " + r.getStatus() + ":" + r.getMessage());
//
//        OperationResultType found = findOperationResult(r, new OperationResultMatcher() {
//            @Override
//            public boolean match(OperationResultType r) {
//                return r.getDetails() != null && r.getDetails().contains("UnknownUidException");
//            }
//        });
//        AssertJUnit.assertNotNull("UnknownUidException was not detected", found);
//        System.out.println("======================================================================================================");
//        System.out.println("Details: " + found.getDetails());
//    }
//
//    private OperationResultType findOperationResult(OperationResultType result, OperationResultMatcher matcher) {
//        if (matcher.match(result)) {
//            return result;
//        }
//        for (OperationResultType r : result.getPartialResults()) {
//            OperationResultType found = findOperationResult(r, matcher);
//            if (found != null) {
//                return found;
//            }
//        }
//        return null;
//    }
//
//    @Test
//    public void test220ModifyingNonexistingAccount() throws Exception {
//
//        // create shadow with non-existing GUID
//        System.out.println("Creating shadow with non-existing GUID...");
//        Document doc = ModelClientUtil.getDocumnent();
//
//        String name = "Wrong GUID shadow";
//        ShadowType shadow = new ShadowType();
//        shadow.setName(ModelClientUtil.createPolyStringType(name, doc));
//        shadow.setResourceRef(createObjectReferenceType(ResourceType.class, getResourceOid()));
//        shadow.setObjectClass(OC_ACCOUNT);
//        shadow.setKind(ShadowKindType.ACCOUNT);
//
//        ShadowAttributesType attributes = new ShadowAttributesType();
//        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "name"), name, doc));
//        attributes.getAny().add(ModelClientUtil.createTextElement(new QName(NS_ICFS, "uid"), "CN=wrong-GUID," + getContainer(), doc));
//        shadow.setAttributes(attributes);
//
//        String oid = createObject(ShadowType.class, shadow, createRaw());
//
//        System.out.println("Done, reading it back...");
//        ShadowType shadowReadAgain = getObjectNoFetch(ShadowType.class, oid);
//        dumpAttributes(shadowReadAgain);
//
//        System.out.println("Now launching modifyObject operation...");
//        ObjectDeltaOperationType odo = modifyObject(ShadowType.class, oid, "attributes/sn", ModificationTypeType.REPLACE, "xxxxxx", null, false);
//        OperationResultType r = odo.getExecutionResult();
//        System.out.println("Done: " + r.getStatus() + ":" + r.getMessage());
//
//        OperationResultType found = findOperationResult(r, new OperationResultMatcher() {
//            @Override
//            public boolean match(OperationResultType r) {
//                return r.getDetails() != null && r.getDetails().contains("UnknownUidException");
//            }
//        });
//        AssertJUnit.assertNotNull("UnknownUidException was not detected", found);
//        System.out.println("======================================================================================================");
//        System.out.println("Details: " + found.getDetails());
//    }


    @Test
    public void test900Cleanup() throws Exception {
//        deleteObject(ShadowType.class, orvilleOid, true);
//        cleanup();
    }

    @FunctionalInterface
    private interface OperationResultMatcher {
        boolean match(OperationResultType operationResultType);
    }
}
