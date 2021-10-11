/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import java.io.File;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 * @author mederly
 */
public class TestParsePasswordPolicy extends AbstractSchemaTest {

    public static final File FILE = new File("src/test/resources/common/password-policy.xml");

    @Test
    public void testParsePasswordPolicyFile() throws Exception {
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
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<ValuePolicyType> policy = prismContext.parseObject(FILE);

        System.out.println("Parsed policy:");
        System.out.println(policy.debugDump());

        assertPolicy(policy);

        // SERIALIZE

        String serializedPolicy = prismContext.xmlSerializer().serialize(policy);

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

    private void assertPropertyDefinition(PrismContainer<?> container,
            String propName, QName xsdType, int minOccurs, int maxOccurs) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }
}
