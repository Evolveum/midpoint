/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class AdUtils {
	
	public static final String ATTRIBUTE_OBJECT_GUID_NAME = "objectGUID";
	public static final String ATTRIBUTE_SAM_ACCOUNT_NAME_NAME = "sAMAccountName";
	public static final String ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME = "userAccountControl";
	public static final QName ATTRIBUTE_USER_ACCOUNT_CONTROL_QNAME = new QName(MidPointConstants.NS_RI, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME);
	public static final String ATTRIBUTE_UNICODE_PWD_NAME = "unicodePwd";
	public static final String ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME = "msExchHideFromAddressLists";
	public static final QName ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_QNAME = new QName(MidPointConstants.NS_RI, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME);
	
	public static final QName OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME = new QName(MidPointConstants.NS_RI, "msExchBaseClass");
	
	/**
	 * Returns dashed GUID notation formatted from simple hex-encoded binary.
	 * 
	 * E.g. "2f01c06bb1d0414e9a69dd3841a13506" -> "6bc0012f-d0b1-4e41-9a69-dd3841a13506"
	 */
	public static String formatGuidToDashedNotation(String hexValue) {
		if (hexValue == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(hexValue.substring(6, 8));
		sb.append(hexValue.substring(4, 6));
		sb.append(hexValue.substring(2, 4));
		sb.append(hexValue.substring(0, 2));
		sb.append('-');
		sb.append(hexValue.substring(10, 12));
		sb.append(hexValue.substring(8, 10));
		sb.append('-');
		sb.append(hexValue.substring(14, 16));
		sb.append(hexValue.substring(12, 14));
		sb.append('-');
		sb.append(hexValue.substring(16, 20));
		sb.append('-');
		sb.append(hexValue.substring(20, 32));
		return sb.toString();
	}

	public static ObjectClassComplexTypeDefinition assertAdSchema(PrismObject<ResourceType> resource, QName accountObjectClass, PrismContext prismContext) throws SchemaException {
		
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        display("Resource schema", resourceSchema);
        
        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
        display("Refined schema", refinedSchema);
        ObjectClassComplexTypeDefinition accountObjectClassDefinition = refinedSchema.findObjectClassDefinition(accountObjectClass);
        assertNotNull("No definition for object class "+accountObjectClass, accountObjectClassDefinition);
        display("Account object class def", accountObjectClassDefinition);
        
        ResourceAttributeDefinition<String> cnDef = accountObjectClassDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, new QName(MidPointConstants.NS_RI, "cn"), DOMUtil.XSD_STRING, 0, 1);
        assertTrue("cn read", cnDef.canRead());
        assertTrue("cn modify", cnDef.canModify());
        assertTrue("cn add", cnDef.canAdd());

        ResourceAttributeDefinition<String> samAccountNameDef = accountObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_SAM_ACCOUNT_NAME_NAME);
        PrismAsserts.assertDefinition(samAccountNameDef, 
        		new QName(MidPointConstants.NS_RI, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME), DOMUtil.XSD_STRING, 0, 1);
        assertTrue("samAccountNameDef read", samAccountNameDef.canRead());
        assertTrue("samAccountNameDef modify", samAccountNameDef.canModify());
        assertTrue("samAccountNameDef add", samAccountNameDef.canAdd());
        
        ResourceAttributeDefinition<String> oDef = accountObjectClassDefinition.findAttributeDefinition("o");
        PrismAsserts.assertDefinition(oDef, new QName(MidPointConstants.NS_RI, "o"), DOMUtil.XSD_STRING, 0, -1);
        assertTrue("o read", oDef.canRead());
        assertTrue("o modify", oDef.canModify());
        assertTrue("o add", oDef.canAdd());
        
        ResourceAttributeDefinition<Long> createTimestampDef = accountObjectClassDefinition.findAttributeDefinition("createTimeStamp");
        PrismAsserts.assertDefinition(createTimestampDef, new QName(MidPointConstants.NS_RI, "createTimeStamp"),
        		DOMUtil.XSD_LONG, 0, 1);
        assertTrue("createTimeStampDef read", createTimestampDef.canRead());
        assertFalse("createTimeStampDef modify", createTimestampDef.canModify());
        assertFalse("createTimeStampDef add", createTimestampDef.canAdd());
        
        ResourceAttributeDefinition<Long> isCriticalSystemObjectDef = accountObjectClassDefinition.findAttributeDefinition("isCriticalSystemObject");
        PrismAsserts.assertDefinition(isCriticalSystemObjectDef, new QName(MidPointConstants.NS_RI, "isCriticalSystemObject"),
        		DOMUtil.XSD_BOOLEAN, 0, 1);
        assertTrue("isCriticalSystemObject read", isCriticalSystemObjectDef.canRead());
        assertTrue("isCriticalSystemObject modify", isCriticalSystemObjectDef.canModify());
        assertTrue("isCriticalSystemObject add", isCriticalSystemObjectDef.canAdd());
        
        ResourceAttributeDefinition<Long> nTSecurityDescriptorDef = accountObjectClassDefinition.findAttributeDefinition("nTSecurityDescriptor");
        PrismAsserts.assertDefinition(nTSecurityDescriptorDef, new QName(MidPointConstants.NS_RI, "nTSecurityDescriptor"),
        		DOMUtil.XSD_BASE64BINARY, 0, 1);
        assertTrue("nTSecurityDescriptor read", nTSecurityDescriptorDef.canRead());
        assertTrue("nTSecurityDescriptor modify", nTSecurityDescriptorDef.canModify());
        assertTrue("nTSecurityDescriptor add", nTSecurityDescriptorDef.canAdd());
        
        ResourceAttributeDefinition<Long> lastLogonDef = accountObjectClassDefinition.findAttributeDefinition("lastLogon");
        PrismAsserts.assertDefinition(lastLogonDef, new QName(MidPointConstants.NS_RI, "lastLogon"),
        		DOMUtil.XSD_LONG, 0, 1);
        assertTrue("lastLogonDef read", lastLogonDef.canRead());
        assertTrue("lastLogonDef modify", lastLogonDef.canModify());
        assertTrue("lastLogonDef add", lastLogonDef.canAdd());
        
        return accountObjectClassDefinition;
	}
	
	public static void assertExchangeSchema(PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException {
		
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        
        ObjectClassComplexTypeDefinition msExchBaseClassObjectClassDefinition = resourceSchema.findObjectClassDefinition(OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME);
        assertNotNull("No definition for object class "+OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME, msExchBaseClassObjectClassDefinition);
        display("Object class "+OBJECT_CLASS_MS_EXCH_BASE_CLASS_QNAME+" def", msExchBaseClassObjectClassDefinition);
        
        ResourceAttributeDefinition<String> msExchHideFromAddressListsDef = msExchBaseClassObjectClassDefinition.findAttributeDefinition(ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME);
        PrismAsserts.assertDefinition(msExchHideFromAddressListsDef, new QName(MidPointConstants.NS_RI, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME), DOMUtil.XSD_BOOLEAN, 0, 1);
        assertTrue("msExchHideFromAddressLists read", msExchHideFromAddressListsDef.canRead());
        assertTrue("msExchHideFromAddressLists modify", msExchHideFromAddressListsDef.canModify());
        assertTrue("msExchHideFromAddressLists add", msExchHideFromAddressListsDef.canAdd());

	}

}
