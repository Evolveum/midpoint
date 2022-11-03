/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_CN;

import static org.testng.AssertJUnit.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Mixin providing common methods/utilities for Active Directory (AD) related tests.
 */
public interface AdTestMixin extends InfraTestMixin {

    String ATTRIBUTE_OBJECT_GUID_NAME = "objectGUID";
    String ATTRIBUTE_OBJECT_SID_NAME = "objectSid";
    String ATTRIBUTE_OBJECT_CATEGORY_NAME = "objectCategory";
    String ATTRIBUTE_SAM_ACCOUNT_NAME_NAME = "sAMAccountName";
    String ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME = "userAccountControl";
    QName ATTRIBUTE_USER_ACCOUNT_CONTROL_QNAME = new QName(MidPointConstants.NS_RI, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME);
    String ATTRIBUTE_UNICODE_PWD_NAME = "unicodePwd";
    String ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME = "msExchHideFromAddressLists";
    String ATTRIBUTE_TITLE_NAME = "title";
    String ATTRIBUTE_TELEPHONE_NUMBER = "telephoneNumber";
    String ATTRIBUTE_PROXY_ADDRESSES_NAME = "proxyAddresses";
    String ATTRIBUTE_USER_PARAMETERS_NAME = "userParameters";
    String ATTRIBUTE_MEMBER_NAME = "member";

    QName OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME = new QName(MidPointConstants.NS_RI, "msExchBaseClass");

    String AD_CONNECTOR_TYPE = "com.evolveum.polygon.connector.ldap.ad.AdLdapConnector";

    /**
     * Returns dashed GUID notation formatted from simple hex-encoded binary.
     * <p>
     * E.g. "2f01c06bb1d0414e9a69dd3841a13506" -> "6bc0012f-d0b1-4e41-9a69-dd3841a13506"
     */
    default String formatGuidToDashedNotation(String hexValue) {
        if (hexValue == null) {
            return null;
        }
        return hexValue.substring(6, 8)
                + hexValue.substring(4, 6)
                + hexValue.substring(2, 4)
                + hexValue.substring(0, 2)
                + '-'
                + hexValue.substring(10, 12)
                + hexValue.substring(8, 10)
                + '-'
                + hexValue.substring(14, 16)
                + hexValue.substring(12, 14)
                + '-'
                + hexValue.substring(16, 20)
                + '-'
                + hexValue.substring(20, 32);
    }

    default ResourceObjectDefinition assertAdResourceSchema(
            PrismObject<ResourceType> resource, QName accountObjectClass)
            throws SchemaException, ConfigurationException {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchemaRequired(resource.asObjectable());
        displayDumpable("Resource schema", resourceSchema);
        resourceSchema.validate();
        return assertAdSchema(resource, accountObjectClass);
    }

    default ResourceObjectDefinition assertAdRefinedSchema(
            PrismObject<ResourceType> resource, QName accountObjectClass) throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        displayDumpable("Refined schema", refinedSchema);
        refinedSchema.validate();
        return assertAdSchema(resource, accountObjectClass);
    }

    // Assumes string timestamp
    default ResourceObjectDefinition assertAdSchema(
            PrismObject<ResourceType> resource, QName accountObjectClass) throws SchemaException, ConfigurationException {
        ResourceObjectDefinition accountObjectClassDefinition = assertAdSchemaBase(resource, accountObjectClass);

        ResourceAttributeDefinition<?> createTimestampDef =
                accountObjectClassDefinition.findAttributeDefinition("createTimeStamp");
        PrismAsserts.assertDefinition(createTimestampDef,
                new QName(MidPointConstants.NS_RI, "createTimeStamp"), DOMUtil.XSD_DATETIME, 0, 1);
        assertTrue("createTimeStampDef read", createTimestampDef.canRead());
        assertFalse("createTimeStampDef modify", createTimestampDef.canModify());
        assertFalse("createTimeStampDef add", createTimestampDef.canAdd());

        ResourceAttributeDefinition<?> whenChangedDef =
                accountObjectClassDefinition.findAttributeDefinition("whenChanged");
        PrismAsserts.assertDefinition(whenChangedDef,
                new QName(MidPointConstants.NS_RI, "createTimeStamp"), DOMUtil.XSD_DATETIME, 0, 1);
        assertTrue("whenChanged read", whenChangedDef.canRead());
        assertFalse("whenChanged modify", whenChangedDef.canModify());
        assertFalse("whenChanged add", whenChangedDef.canAdd());

        return accountObjectClassDefinition;
    }

    private ResourceObjectDefinition assertAdSchemaBase(
            PrismObject<ResourceType> resource, QName accountObjectClass) throws SchemaException, ConfigurationException {

        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        displayDumpable("Refined schema", refinedSchema);
        ResourceObjectDefinition accountObjectClassDefinition = refinedSchema.findDefinitionForObjectClass(accountObjectClass);
        assertNotNull("No definition for object class " + accountObjectClass, accountObjectClassDefinition);
        displayDumpable("Account object class def", accountObjectClassDefinition);

        ResourceAttributeDefinition<?> cnDef = accountObjectClassDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, QNAME_CN, DOMUtil.XSD_STRING, 0, 1);
        assertTrue("cn read", cnDef.canRead());
        assertTrue("cn modify", cnDef.canModify());
        assertTrue("cn add", cnDef.canAdd());

        ResourceAttributeDefinition<?> samAccountNameDef = accountObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_SAM_ACCOUNT_NAME_NAME);
        PrismAsserts.assertDefinition(samAccountNameDef,
                new QName(MidPointConstants.NS_RI, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME), DOMUtil.XSD_STRING, 0, 1);
        assertTrue("samAccountNameDef read", samAccountNameDef.canRead());
        assertTrue("samAccountNameDef modify", samAccountNameDef.canModify());
        assertTrue("samAccountNameDef add", samAccountNameDef.canAdd());

        ResourceAttributeDefinition<?> oDef = accountObjectClassDefinition.findAttributeDefinition("o");
        PrismAsserts.assertDefinition(oDef, new QName(MidPointConstants.NS_RI, "o"), DOMUtil.XSD_STRING, 0, -1);
        assertTrue("o read", oDef.canRead());
        assertTrue("o modify", oDef.canModify());
        assertTrue("o add", oDef.canAdd());

        ResourceAttributeDefinition<?> isCriticalSystemObjectDef = accountObjectClassDefinition.findAttributeDefinition("isCriticalSystemObject");
        PrismAsserts.assertDefinition(isCriticalSystemObjectDef, new QName(MidPointConstants.NS_RI, "isCriticalSystemObject"),
                PrimitiveType.XSD_BOOLEAN, 0, 1);
        assertTrue("isCriticalSystemObject read", isCriticalSystemObjectDef.canRead());
        assertTrue("isCriticalSystemObject modify", isCriticalSystemObjectDef.canModify());
        assertTrue("isCriticalSystemObject add", isCriticalSystemObjectDef.canAdd());

        ResourceAttributeDefinition<?> nTSecurityDescriptorDef = accountObjectClassDefinition.findAttributeDefinition("nTSecurityDescriptor");
        PrismAsserts.assertDefinition(nTSecurityDescriptorDef, new QName(MidPointConstants.NS_RI, "nTSecurityDescriptor"),
                PrimitiveType.XSD_BASE64BINARY, 0, 1);
        assertTrue("nTSecurityDescriptor read", nTSecurityDescriptorDef.canRead());
        assertTrue("nTSecurityDescriptor modify", nTSecurityDescriptorDef.canModify());
        assertTrue("nTSecurityDescriptor add", nTSecurityDescriptorDef.canAdd());

        ResourceAttributeDefinition<?> objectSidDef = accountObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_OBJECT_SID_NAME);
        PrismAsserts.assertDefinition(objectSidDef, new QName(MidPointConstants.NS_RI, ATTRIBUTE_OBJECT_SID_NAME),
                PrimitiveType.XSD_STRING, 0, 1);
        assertTrue("objectSid read", objectSidDef.canRead());
        assertFalse("objectSid modify", objectSidDef.canModify());
        assertFalse("objectSid add", objectSidDef.canAdd());

        ResourceAttributeDefinition<?> lastLogonDef = accountObjectClassDefinition.findAttributeDefinition("lastLogon");
        PrismAsserts.assertDefinition(lastLogonDef, new QName(MidPointConstants.NS_RI, "lastLogon"),
                PrimitiveType.XSD_LONG, 0, 1);
        assertTrue("lastLogonDef read", lastLogonDef.canRead());
        assertTrue("lastLogonDef modify", lastLogonDef.canModify());
        assertTrue("lastLogonDef add", lastLogonDef.canAdd());

        ResourceAttributeDefinition<?> proxyAddressesDef = accountObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_PROXY_ADDRESSES_NAME);
        PrismAsserts.assertDefinition(proxyAddressesDef, new QName(MidPointConstants.NS_RI, ATTRIBUTE_PROXY_ADDRESSES_NAME),
                PrimitiveType.XSD_STRING, 0, -1);
        assertTrue("proxyAddressesDef read", proxyAddressesDef.canRead());
        assertTrue("proxyAddressesDef modify", proxyAddressesDef.canModify());
        assertTrue("proxyAddressesDef add", proxyAddressesDef.canAdd());
        // TODO: proxyAddressesDef.getMatchingRuleQName()

        return accountObjectClassDefinition;
    }

    default void assertExchangeSchema(PrismObject<ResourceType> resource, QName accountObjectClassQName)
            throws SchemaException, ConfigurationException {

        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchemaRequired(resource.asObjectable());
        assertExchangeSchema(resourceSchema, accountObjectClassQName);

        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        assertExchangeSchema(refinedSchema, accountObjectClassQName);
    }

    default void assertExchangeSchema(ResourceSchema resourceSchema, QName accountObjectClassQName) {
        ResourceObjectDefinition msExchBaseClassObjectClassDefinition = resourceSchema.findDefinitionForObjectClass(OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME);
        assertNotNull("No definition for object class " + OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME, msExchBaseClassObjectClassDefinition);
        displayDumpable("Object class " + OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME + " def", msExchBaseClassObjectClassDefinition);

        ResourceAttributeDefinition<?> msExchHideFromAddressListsDef = msExchBaseClassObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME);
        PrismAsserts.assertDefinition(msExchHideFromAddressListsDef, new QName(MidPointConstants.NS_RI, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME), DOMUtil.XSD_BOOLEAN, 0, 1);
        assertTrue("msExchHideFromAddressLists read", msExchHideFromAddressListsDef.canRead());
        assertTrue("msExchHideFromAddressLists modify", msExchHideFromAddressListsDef.canModify());
        assertTrue("msExchHideFromAddressLists add", msExchHideFromAddressListsDef.canAdd());

        ResourceObjectDefinition accountObjectClassDef = resourceSchema.findDefinitionForObjectClass(accountObjectClassQName);
        assertNotNull("No definition for object class " + accountObjectClassQName, accountObjectClassDef);
        displayDumpable("Object class " + accountObjectClassQName + " def", accountObjectClassDef);

        ResourceAttributeDefinition<?> accountMsExchHideFromAddressListsDef = accountObjectClassDef.findAttributeDefinition(ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME);
        PrismAsserts.assertDefinition(accountMsExchHideFromAddressListsDef, new QName(MidPointConstants.NS_RI, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME), DOMUtil.XSD_BOOLEAN, 0, 1);
        assertTrue("msExchHideFromAddressLists read", accountMsExchHideFromAddressListsDef.canRead());
        assertTrue("msExchHideFromAddressLists modify", accountMsExchHideFromAddressListsDef.canModify());
        assertTrue("msExchHideFromAddressLists add", accountMsExchHideFromAddressListsDef.canAdd());
    }

    default long getWin32Filetime(long millis) {
        return (millis + 11644473600000L) * 10000L;
    }

    default void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }
}
