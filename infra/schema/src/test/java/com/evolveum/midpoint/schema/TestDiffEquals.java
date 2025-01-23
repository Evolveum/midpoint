/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy.DATA;
import static com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy.LITERAL;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@SuppressWarnings("SimplifiableAssertion")
public class TestDiffEquals extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/diff");
    private static final File ROLE_COMPARE_FILE = new File(TEST_DIR, "role-compare.xml");

    private static final File ROLE_1 = new File(TEST_DIR, "role-1.xml");
    private static final File ROLE_2 = new File(TEST_DIR, "role-2.xml");

    private static final String NS_TEST_RI = "http://midpoint.evolveum.com/xml/ns/test/ri-1";

    @Test
    public void testUserSimplePropertyDiff() throws SchemaException {
        UserType userType1 = new UserType();
        userType1.setName(PrismTestUtil.createPolyStringType("test name"));
        UserType userType2 = new UserType();
        userType2.setName(PrismTestUtil.createPolyStringType("test name"));
        PrismTestUtil.getPrismContext().adopt(userType1);
        PrismTestUtil.getPrismContext().adopt(userType2);

        ObjectDelta<?> delta = userType1.asPrismObject().diff(userType2.asPrismObject());
        assertNotNull(delta);
        assertEquals(0, delta.getModifications().size());

        userType2.setDescription(null);

        delta = userType1.asPrismObject().diff(userType2.asPrismObject());
        assertNotNull(delta);
        assertEquals("Delta should be empty, nothing changed.", 0, delta.getModifications().size());
    }

    @Test
    public void testUserListSimpleDiff() throws SchemaException {
        UserType u1 = new UserType();
        u1.setName(PrismTestUtil.createPolyStringType("test name"));
        UserType u2 = new UserType();
        u2.setName(PrismTestUtil.createPolyStringType("test name"));
        PrismTestUtil.getPrismContext().adopt(u1);
        PrismTestUtil.getPrismContext().adopt(u2);

        ObjectDelta<UserType> delta = u1.asPrismObject().diff(u2.asPrismObject());
        assertNotNull(delta);
        assertEquals(0, delta.getModifications().size());

        u2.getAdditionalName();

        delta = u1.asPrismObject().diff(u2.asPrismObject());
        assertNotNull(delta);
        assertEquals("Delta should be empty, nothing changed.", 0, delta.getModifications().size());
    }

    @Test
    public void testAssignmentEquals1() {
        when();
        AssignmentType a1a = new AssignmentType().description("descr1");
        AssignmentType a1b = new AssignmentType().description("descr1");
        AssignmentType a1e = new AssignmentType().description("descr1")
                .activation(new ActivationType().effectiveStatus(ActivationStatusType.ENABLED));
        AssignmentType a1m = new AssignmentType().description("descr1")
                .metadata(new MetadataType().createTimestamp(
                        XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis())));

        AssignmentType a2 = new AssignmentType().description("descr2");

        then("None of a1 equals to a2 (and vice versa)");
        assertThat(a1a).isNotEqualTo(a2);
        assertThat(a1b).isNotEqualTo(a2);
        assertThat(a1m).isNotEqualTo(a2);
        assertThat(a1e).isNotEqualTo(a2);
        assertThat(a2).isNotEqualTo(a1a);
        assertThat(a2).isNotEqualTo(a1b);
        assertThat(a2).isNotEqualTo(a1m);
        assertThat(a2).isNotEqualTo(a1e);

        and("each object is equal to itself");
        assertThat(a1a).isEqualTo(a1a);
        assertThat(a1b).isEqualTo(a1b);
        assertThat(a1m).isEqualTo(a1m);
        assertThat(a1e).isEqualTo(a1e);
        assertThat(a2).isEqualTo(a2);

        and("each a1 object is equal to all other a1 objects (default equals strategy ignores metadata and activation)");
        assertThat(a1a).isEqualTo(a1b);
        assertThat(a1a).isEqualTo(a1e);
        assertThat(a1a).isEqualTo(a1m);
        assertThat(a1b).isEqualTo(a1a);
        assertThat(a1b).isEqualTo(a1e);
        assertThat(a1b).isEqualTo(a1m);
        assertThat(a1e).isEqualTo(a1a);
        assertThat(a1e).isEqualTo(a1b);
        assertThat(a1e).isEqualTo(a1m);
        assertThat(a1m).isEqualTo(a1a);
        assertThat(a1m).isEqualTo(a1b);
        assertThat(a1m).isEqualTo(a1e);
    }

    @Test(enabled = false) // MID-3966
    public void testAssignmentEquals2() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when("a1 and a2 are with the same role and a3 is with different");
        PrismObject<RoleType> roleCompare = prismContext.parseObject(ROLE_COMPARE_FILE);
        PrismContainer<AssignmentType> inducementContainer = roleCompare.findContainer(RoleType.F_INDUCEMENT);
        AssignmentType a1 = inducementContainer.findValue(1L).asContainerable();
        AssignmentType a2 = inducementContainer.findValue(2L).asContainerable();
        AssignmentType a3 = inducementContainer.findValue(3L).asContainerable();

        then("assignments a1 and a2 are equal, but a3 is not equal with them");
        assertThat(a1).isNotEqualTo(a3);
        assertThat(a2).isNotEqualTo(a3);

        assertThat(a1).isEqualTo(a1);
        assertThat(a1).isEqualTo(a2);
        assertThat(a2).isEqualTo(a1);
        assertThat(a2).isEqualTo(a2);
        assertThat(a3).isEqualTo(a3);
    }

    @Test
    public void testAssignmentEquivalent() {
        AssignmentType a1 = new AssignmentType();
        ActivationType a1a = new ActivationType();
        a1a.setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        a1a.setEffectiveStatus(ActivationStatusType.ENABLED);
        a1.setActivation(a1a);

        AssignmentType a2 = new AssignmentType();
        ActivationType a2a = new ActivationType();
        a2a.setEffectiveStatus(ActivationStatusType.ENABLED);
        a2.setActivation(a2a);

        // WHEN
        assertFalse(a1.equals(a2));
        //noinspection unchecked
        assertFalse(a1.asPrismContainerValue().equivalent(a2.asPrismContainerValue()));            // a bit redundant

        assertFalse(a2.equals(a1));
        //noinspection unchecked
        assertFalse(a2.asPrismContainerValue().equivalent(a1.asPrismContainerValue()));            // a bit redundant
    }

    // Disabled, prismContext is now always present
    @Test(enabled = false)
    public void testContextlessAssignmentEquals() throws Exception {
        AssignmentType a1 = new AssignmentType();            // no prismContext here
        a1.setDescription("descr1");

        AssignmentType a2 = new AssignmentType();            // no prismContext here
        a2.setDescription("descr2");

        AssignmentType a3 = new AssignmentType();            // no prismContext here
        a3.setDescription("descr1");

        assertFalse(a1.equals(a2));                          // this should work even without prismContext
        assertTrue(a1.equals(a3));                           // this should work even without prismContext

        PrismContext prismContext = PrismTestUtil.getPrismContext();
        prismContext.adopt(a1);
        prismContext.adopt(a2);
        prismContext.adopt(a3);
        assertFalse(a1.equals(a2));                         // this should work as well
        assertTrue(a1.equals(a3));
    }

    @Test(enabled = false)
    public void testContextlessAssignmentEquals2() {
        // (1) user without prismContext - the functionality is reduced

        UserType user = new UserType();

        AssignmentType a1 = new AssignmentType();            // no prismContext here
        a1.setDescription("descr1");
        user.getAssignment().add(a1);
        AssignmentType a2 = new AssignmentType();            // no prismContext here
        a2.setDescription("descr2");
        user.getAssignment().add(a2);

        AssignmentType a2identical = new AssignmentType();
        a2identical.setDescription("descr2");
        assertTrue(user.getAssignment().contains(a2identical));

        ObjectDelta<UserType> delta1 = user.asPrismObject().createDelta(ChangeType.DELETE);       // delta1 is without prismContext

        // (2) user with prismContext

        UserType userWithContext = new UserType();

        AssignmentType b1 = new AssignmentType();            // no prismContext here
        b1.setDescription("descr1");
        userWithContext.getAssignment().add(b1);
        AssignmentType b2 = new AssignmentType();            // no prismContext here
        b2.setDescription("descr2");
        userWithContext.getAssignment().add(b2);

        AssignmentType b2identical = new AssignmentType();
        b2identical.setDescription("descr2");
        assertTrue(user.getAssignment().contains(b2identical));

        // b1 and b2 obtain context when they are added to the container
        assertFalse(b1.equals(b2));

        ObjectDelta<UserType> delta2 = userWithContext.asPrismObject().createDelta(ChangeType.DELETE);
    }

    @Test
    public void testAssignmentHashcode() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        AssignmentType a1a = new AssignmentType();
        prismContext.adopt(a1a);
        a1a.setDescription("descr1");

        AssignmentType a2 = new AssignmentType();
        prismContext.adopt(a2);
        a2.setDescription("descr2");

        AssignmentType a1b = new AssignmentType();
        prismContext.adopt(a1b);
        a1b.setDescription("descr1");

        AssignmentType a1m = new AssignmentType();
        prismContext.adopt(a1m);
        a1m.setDescription("descr1");
        MetadataType metadata1m = new MetadataType();
        metadata1m.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        a1m.setMetadata(metadata1m);

        AssignmentType a1e = new AssignmentType();
        prismContext.adopt(a1e);
        a1e.setDescription("descr1");
        ActivationType activation1e = new ActivationType();
        activation1e.setEffectiveStatus(ActivationStatusType.ENABLED);
        a1e.setActivation(activation1e);

        // WHEN
        assertFalse(a1a.hashCode() == a2.hashCode());
        assertFalse(a1b.hashCode() == a2.hashCode());
        assertFalse(a1m.hashCode() == a2.hashCode());
        assertFalse(a1e.hashCode() == a2.hashCode());
        assertFalse(a2.hashCode() == a1a.hashCode());
        assertFalse(a2.hashCode() == a1b.hashCode());
        assertFalse(a2.hashCode() == a1m.hashCode());
        assertFalse(a2.hashCode() == a1e.hashCode());

        assertTrue(a1a.hashCode() == a1a.hashCode());
        assertTrue(a1b.hashCode() == a1b.hashCode());
        assertTrue(a1m.hashCode() == a1m.hashCode());
        assertTrue(a1e.hashCode() == a1e.hashCode());
        assertTrue(a2.hashCode() == a2.hashCode());

        assertTrue(a1a.hashCode() == a1b.hashCode());
        assertTrue(a1b.hashCode() == a1a.hashCode());
        assertTrue(a1a.hashCode() == a1m.hashCode());
        assertTrue(a1b.hashCode() == a1m.hashCode());
        assertTrue(a1m.hashCode() == a1a.hashCode());
        assertTrue(a1m.hashCode() == a1b.hashCode());
        assertTrue(a1m.hashCode() == a1e.hashCode());
        assertTrue(a1a.hashCode() == a1e.hashCode());
        assertTrue(a1b.hashCode() == a1e.hashCode());
        assertTrue(a1e.hashCode() == a1a.hashCode());
        assertTrue(a1e.hashCode() == a1b.hashCode());
        assertTrue(a1e.hashCode() == a1m.hashCode());
    }

    // MID-4251
    @Test
    public void testAssignmentHashcode2() {
        AssignmentType a1a = new AssignmentType().id(6L)
                .beginMetadata()
                .createApprovalComment("hi")
                .<AssignmentType>end()
                .targetRef(new ObjectReferenceType().oid("target").type(OrgType.COMPLEX_TYPE).relation(SchemaConstants.ORG_DEFAULT))
                .beginActivation()
                .effectiveStatus(ActivationStatusType.ENABLED)
                .validTo("2018-01-01T00:00:00.000+01:00")
                .end();
        AssignmentType a1b = new AssignmentType()
                .targetRef(new ObjectReferenceType().oid("target").type(OrgType.COMPLEX_TYPE))
                .beginActivation()
                .validTo("2018-01-01T00:00:00.000+01:00")
                .end();

        // WHEN
        assertEquals("Wrong hashCode", a1a.hashCode(), a1b.hashCode());
    }

    // MID-4251
    @Test
    public void testAssignmentHashcode3() {
        AssignmentType a1a = new AssignmentType()
                .beginActivation()
                .validTo("2018-01-01T00:00:00.000+01:00")
                .end();
        AssignmentType a1b = a1a.clone();

        // use unqualified item name for validTo
        a1b.getActivation().asPrismContainerValue()
                .findItem(ActivationType.F_VALID_TO)
                .setElementName(new QName("validTo"));

        System.out.println("a1a = " + a1a.asPrismContainerValue().debugDump());
        System.out.println("a1b = " + a1b.asPrismContainerValue().debugDump());

        // WHEN
        assertEquals("Wrong hashCode", a1a.hashCode(), a1b.hashCode());
    }

    @Test
    public void testDiffShadow() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<ShadowType> shadow1 = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ShadowType.class).instantiate();
        ShadowType shadow1Type = shadow1.asObjectable();
        shadow1Type.setName(new PolyStringType("Whatever"));
        shadow1Type.getAuxiliaryObjectClass().add(new QName(NS_TEST_RI, "foo"));

        ShadowType shadow2Type = new ShadowType();
        PrismObject<ShadowType> shadow2 = shadow2Type.asPrismObject();
        prismContext.adopt(shadow2Type);
        shadow2Type.setName(new PolyStringType("Whatever"));
        shadow2Type.getAuxiliaryObjectClass().add(new QName(NS_TEST_RI, "foo"));
        shadow2Type.getAuxiliaryObjectClass().add(new QName(NS_TEST_RI, "bar"));
        PrismContainer<Containerable> shadow2Attrs = shadow2.findOrCreateContainer(ShadowType.F_ATTRIBUTES);

        PrismProperty<String> attrEntryUuid = prismContext.itemFactory().createProperty(new QName(NS_TEST_RI, "entryUuid"));
        PrismPropertyDefinition<String> attrEntryUuidDef = prismContext.definitionFactory().newPropertyDefinition(new QName(NS_TEST_RI, "entryUuid"),
                DOMUtil.XSD_STRING);
        attrEntryUuid.setDefinition(attrEntryUuidDef);
        shadow2Attrs.add(attrEntryUuid);
        attrEntryUuid.addRealValue("1234-5678-8765-4321");

        PrismProperty<String> attrDn = prismContext.itemFactory().createProperty(new QName(NS_TEST_RI, "dn"));
        PrismPropertyDefinition<String> attrDnDef = prismContext.definitionFactory().newPropertyDefinition(new QName(NS_TEST_RI, "dn"),
                DOMUtil.XSD_STRING);
        attrDn.setDefinition(attrDnDef);
        shadow2Attrs.add(attrDn);
        attrDn.addRealValue("uid=foo,o=bar");

        System.out.println("Shadow 1");
        System.out.println(shadow1.debugDump(1));
        System.out.println("Shadow 2");
        System.out.println(shadow2.debugDump(1));

        // WHEN
        ObjectDelta<ShadowType> delta = shadow1.diff(shadow2);

        // THEN
        assertNotNull("No delta", delta);
        System.out.println("Delta");
        System.out.println(delta.debugDump(1));

        PrismAsserts.assertIsModify(delta);
        PrismAsserts.assertPropertyAdd(delta, ShadowType.F_AUXILIARY_OBJECT_CLASS, new QName(NS_TEST_RI, "bar"));
        //noinspection unchecked
        PrismAsserts.assertContainerAdd(delta, ShadowType.F_ATTRIBUTES, shadow2Attrs.getValue().clone());
        PrismAsserts.assertModifications(delta, 2);
    }

    @Test
    public void testTriggerCollectionsEqual() {
        EvaluatedPolicyRuleTriggerType trigger1 = new EvaluatedPolicyRuleTriggerType()
                .triggerId(100)
                .ruleName("rule100");
        EvaluatedPolicyRuleTriggerType trigger2 = new EvaluatedPolicyRuleTriggerType()
                .triggerId(200)
                .ruleName("rule200");
        EvaluatedPolicyRuleType sourceRule1 = new EvaluatedPolicyRuleType()
                .trigger(trigger1);
        EvaluatedPolicyRuleType sourceRule2 = new EvaluatedPolicyRuleType()
                .trigger(trigger2);
        List<EvaluatedPolicyRuleTriggerType> triggerListA = Arrays.asList(
                new EvaluatedSituationTriggerType()
                        .triggerId(1)
                        .ruleName("rule1")
                        .sourceRule(sourceRule1)
                        .sourceRule(sourceRule2),
                trigger1,
                trigger2);
        List<EvaluatedPolicyRuleTriggerType> triggerListB = Arrays.asList(
                trigger1,
                trigger2,
                new EvaluatedSituationTriggerType()
                        .triggerId(1)
                        .ruleName("rule1")
                        .sourceRule(sourceRule2)
                        .sourceRule(sourceRule1)
        );
        List<EvaluatedPolicyRuleTriggerType> triggerListC = Arrays.asList(
                trigger1,
                trigger2,
                new EvaluatedSituationTriggerType()
                        .triggerId(1)
                        .ruleName("rule123")
                        .sourceRule(sourceRule2)
                        .sourceRule(sourceRule1)
        );

        assertEquals("Wrong comparison A-A", true, PolicyRuleTypeUtil.triggerCollectionsEqual(triggerListA, triggerListA));
        assertEquals("Wrong comparison A-B", true, PolicyRuleTypeUtil.triggerCollectionsEqual(triggerListA, triggerListB));
        assertEquals("Wrong comparison B-A", true, PolicyRuleTypeUtil.triggerCollectionsEqual(triggerListB, triggerListA));
        assertEquals("Wrong comparison A-C", false, PolicyRuleTypeUtil.triggerCollectionsEqual(triggerListA, triggerListC));
        assertEquals("Wrong comparison B-C", false, PolicyRuleTypeUtil.triggerCollectionsEqual(triggerListB, triggerListC));
    }

    @Test
    public void diffRoles() throws Exception {
        PrismObject<RoleType> role1 = PrismTestUtil.parseObject(ROLE_1);
        PrismObject<RoleType> role2 = PrismTestUtil.parseObject(ROLE_2);

        ObjectDelta<RoleType> delta = role1.diff(role2, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        assertFalse(delta.isEmpty());
    }

    @Test      // MID-4688
    public void testDiffWithMetadata() {
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue("abc");

        PrismObject<UserType> user1 = new UserType()
                .beginCredentials()
                .beginPassword()
                .value(value.clone())
                .beginMetadata()
                .requestorComment("hi")
                .<PasswordType>end()
                .<CredentialsType>end()
                .<UserType>end()
                .asPrismObject();
        PrismObject<UserType> user2 = new UserType()
                .beginCredentials()
                .beginPassword()
                .value(value.clone())
                .beginMetadata()
                .<PasswordType>end()
                .<CredentialsType>end()
                .<UserType>end()
                .asPrismObject();

        ObjectDelta<UserType> diffIgnoreMetadataNotLiteral = user1.diff(user2, EquivalenceStrategy.IGNORE_METADATA);
        ObjectDelta<UserType> diffWithMetadataAndLiteral = user1.diff(user2, EquivalenceStrategy.LITERAL);
        ObjectDelta<UserType> diffWithMetadataNotLiteral = user1.diff(user2, EquivalenceStrategy.DATA);

        assertTrue("Diff ignoring metadata is not empty:\n" + diffIgnoreMetadataNotLiteral.debugDump(),
                diffIgnoreMetadataNotLiteral.isEmpty());
        assertFalse("Diff not ignoring metadata (literal) is empty:\n" + diffWithMetadataAndLiteral.debugDump(),
                diffWithMetadataAndLiteral.isEmpty());
        assertFalse("Diff not ignoring metadata (non-literal) is empty:\n" + diffWithMetadataNotLiteral.debugDump(),
                diffWithMetadataNotLiteral.isEmpty());
    }

    // MID-5851
    @Test
    public void testRawValuesHashCode() throws SchemaException {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        QName extensionPropertyName = new QName(NS_TEST_RI, "extensionProperty");

        PrismPropertyDefinition<String> extensionPropertyDef = prismContext.definitionFactory()
                .newPropertyDefinition(extensionPropertyName, DOMUtil.XSD_STRING);
        extensionPropertyDef.mutator().setRuntimeSchema(true);

        PrismProperty<String> propertyParsed = extensionPropertyDef.instantiate();
        PrismProperty<String> propertyRaw = extensionPropertyDef.instantiate();
        propertyParsed.setRealValue("value");
        propertyRaw.setValue(prismContext.itemFactory().createPropertyValue(new PrimitiveXNodeImpl<>("value")));

        PrismObject<UserType> userParsed = new UserType()
                .name("user")
                .asPrismObject();
        userParsed.getOrCreateExtension().add(propertyParsed);

        PrismObject<UserType> userRaw = new UserType()
                .name("user")
                .asPrismObject();
        userRaw.getOrCreateExtension().add(propertyRaw);

        assertHashAndEquals(userParsed, userRaw, null);
        assertHashAndEquals(userParsed, userRaw, LITERAL);
        assertHashAndEquals(userParsed, userRaw, DATA);
    }

    private void assertHashAndEquals(PrismObject<UserType> user1, PrismObject<UserType> user2,
            ParameterizedEquivalenceStrategy strategy) {
        int hash1, hash2;
        if (strategy == null) {
            if (!user1.equals(user2)) {
                fail("Objects are not equal. Diff:\n" + DebugUtil.debugDump(user1.diff(user2)));
            }
            hash1 = user1.hashCode();
            hash2 = user2.hashCode();
        } else {
            if (!user1.equals(user2, strategy)) {
                fail("Objects are not equal under " + strategy + ". Diff:\n" + DebugUtil.debugDump(user1.diff(user2, strategy)));
            }
            hash1 = user1.hashCode(strategy);
            hash2 = user2.hashCode(strategy);
        }
        assertEquals("Hash codes are not equal (strategy=" + strategy + ")", hash1, hash2);
    }

    @Test
    public void testNaturalKeysDiff() {
        GuiObjectDetailsPageType details1 = new GuiObjectDetailsPageType();
        VirtualContainersSpecificationType c3 = createContainer(3L, "id1", "description1");
        details1.getContainer().add(c3);
        details1.getContainer().add(createContainer(4L, "id2", "description2"));

        GuiObjectDetailsPageType details2 = new GuiObjectDetailsPageType();
        details2.getContainer().add(createContainer(5L, "id1", "description11111111"));

        PrismContainer item1 = details1.asPrismContainerValue().findContainer(GuiObjectDetailsPageType.F_CONTAINER);
        PrismContainer item2 = details2.asPrismContainerValue().findContainer(GuiObjectDetailsPageType.F_CONTAINER);

        ContainerDelta<?> delta = item1.diff(item2);
        AssertJUnit.assertNotNull(delta);

        // PCV ID and natural key exists
        // delta will contain only two item deltas, replace description and delete one container value
        List<ItemDelta> deltas = item1.diffModifications(item2, ParameterizedEquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);
        AssertJUnit.assertEquals(2, deltas.size());

        // no PCV ID, natural key will not be used
        // delta will contain only one container delta with add/delete of whole values
        c3.setId(null);
        deltas = item1.diffModifications(item2, ParameterizedEquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);
        AssertJUnit.assertEquals(1, deltas.size());
    }

    private VirtualContainersSpecificationType createContainer(Long id, String identifier, String description) {
        return new VirtualContainersSpecificationType()
                .id(id)
                .identifier(identifier)
                .description(description);
    }

    @Test
    public void testPanelNaturalKeyDiff() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<SystemConfigurationType> s1 = prismContext.parseObject(new File(TEST_DIR, "system-configuration-1.xml"));
        PrismObject<SystemConfigurationType> s2 = prismContext.parseObject(new File(TEST_DIR, "system-configuration-2.xml"));

        ObjectDelta<SystemConfigurationType> delta = s1.diff(s2, ParameterizedEquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);
        System.out.println(delta.debugDump());

        assertEquals(1, delta.getModifications().size());
        ItemDelta<?, ?> itemDelta = delta.getModifications().iterator().next();
        ItemPath path = itemDelta.getPath();

        assertTrue(
                ItemPath.create(
                        SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                        AdminGuiConfigurationType.F_OBJECT_DETAILS,
                        GuiObjectDetailsSetType.F_OBJECT_DETAILS_PAGE,
                        83L,
                        GuiObjectDetailsPageType.F_PANEL,
                        86L,
                        ContainerPanelConfigurationType.F_CONTAINER,
                        92L,
                        VirtualContainersSpecificationType.F_ITEM
                ).equivalent(path));
    }

    /**
     * MID-10297
     */
    @Test(enabled = false)
    public void testTwoResources() throws Exception {
        PrismObject<ResourceType> r1 = PrismTestUtil.parseObject(new File(TEST_DIR, "res-1.xml"));
        PrismObject<ResourceType> r2 = PrismTestUtil.parseObject(new File(TEST_DIR, "res-2.xml"));
        ObjectDelta<ResourceType> delta = r1.diff(r2, ParameterizedEquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        PrismObject<ResourceType> o1 = r1.clone();
        PrismObject<ResourceType> o2 = r1.clone();

        delta.applyTo(o2);

        PrismContext ctx = PrismTestUtil.getPrismContext();

        String leftContent = ctx.xmlSerializer().serializeAnyData(o1);
        // very simple check if the XML is OK - connector ref shouldn't contain xmlns:tns definition and probably not even type attribute
        Assertions.assertThat(leftContent).doesNotContain("xmlns:tns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"");
    }
}
