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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 * @author mederly
 *
 */
public class TestParsePasswordPolicy {

	public static final File FILE = new File("src/test/resources/common/password-policy.xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}


	@Test
	public void testParsePasswordPolicyFile() throws Exception {
		System.out.println("===[ testParsePasswordPolicyFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<ValuePolicyType> policy = prismContext.parserFor(FILE).xml().parse();

		// THEN
		System.out.println("Parsed policy:");
		System.out.println(policy.debugDump());

		assertPolicy(policy);
	}

    @Test
    public void testParsePolicyRoundtrip() throws Exception {
        System.out.println("===[ testParsePolicyRoundtrip ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<ValuePolicyType> policy = prismContext.parseObject(FILE);

        System.out.println("Parsed policy:");
        System.out.println(policy.debugDump());

        assertPolicy(policy);

        // SERIALIZE

        String serializedPolicy = prismContext.serializeObjectToString(policy, PrismContext.LANG_XML);

        System.out.println("serialized policy:");
        System.out.println(serializedPolicy);

        // RE-PARSE

        PrismObject<ValuePolicyType> reparsedPolicy = prismContext.parseObject(serializedPolicy);

        System.out.println("Re-parsed policy:");
        System.out.println(reparsedPolicy.debugDump());

        // Cannot assert here. It will cause parsing of some of the raw values and diff will fail
        assertPolicy(reparsedPolicy);

        ObjectDelta<ValuePolicyType> objectDelta = policy.diff(reparsedPolicy);
        System.out.println("Delta:");
        System.out.println(objectDelta.debugDump());
        assertTrue("Delta is not empty", objectDelta.isEmpty());

        PrismAsserts.assertEquivalent("Policy re-parsed equivalence", policy, reparsedPolicy);
    }

    private void assertPolicy(PrismObject<ValuePolicyType> policy) {

		policy.checkConsistence();

		assertEquals("Wrong oid", "00000000-0000-0000-0000-000000000003", policy.getOid());
		PrismObjectDefinition<ValuePolicyType> usedDefinition = policy.getDefinition();
		assertNotNull("No definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "valuePolicy"),
				ValuePolicyType.COMPLEX_TYPE, ValuePolicyType.class);
		assertEquals("Wrong class in task", ValuePolicyType.class, policy.getCompileTimeClass());
		ValuePolicyType policyType = policy.asObjectable();
		assertNotNull("asObjectable resulted in null", policyType);

		assertPropertyValue(policy, "name", PrismTestUtil.createPolyString("Testing Complex Password Policy"));
		assertPropertyDefinition(policy, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        // TODO...
	}

	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

}
