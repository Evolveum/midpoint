/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @see TestEquals
 * @author semancik
 */
public abstract class TestCompare extends AbstractPrismTest {

    private static final QName REF_QNAME = new QName(NS_FOO, "ref");
    private static final QName REF_TYPE_QNAME = new QName(NS_FOO, "RefType");

    protected abstract String getSubdirName();

    protected abstract String getFilenameSuffix();

    protected File getCommonSubdir() {
        return new File(COMMON_DIR_PATH, getSubdirName());
    }

    protected File getFile(String baseName) {
        return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
    }

    /**
     * Parse the same files twice, compare the results.
     */
    @Test
    public void testCompareJack() throws SchemaException, SAXException, IOException {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> user1 = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
        PrismObject<UserType> user2 = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

        // WHEN, THEN

        assertEquals("Users not the same (PrismObject)(1)", user2, user1);
        assertEquals("Users not the same (PrismObject)(2)", user1, user2);

        // Following line won't work here. We don't have proper generated classes here.
        // It is tested in the "schema" project that has a proper code generation
        // assertTrue("Users not the same (Objectable)", user1.asObjectable().equals(user2.asObjectable()));

        assertTrue("Users not equivalent (1)", user1.equivalent(user2));
        assertTrue("Users not equivalent (2)", user2.equivalent(user1));
    }

    /**
     * Parse original jack and modified Jack. Diff and assert if the resulting
     * delta is OK.
     */
    @Test
    public void testDiffJack() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> jackOriginal = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

        PrismObject<UserType> jackModified = prismContext.parseObject(getFile(USER_JACK_MODIFIED_FILE_BASENAME));

        // WHEN
        ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified);

        // THEN
        System.out.println("Jack delta:");
        System.out.println(jackDelta.debugDump());

        jackDelta.assertDefinitions();
        jackDelta.checkConsistence(true, true, true);

        assertEquals("Wrong delta type", ChangeType.MODIFY, jackDelta.getChangeType());
        assertEquals("Wrong delta OID", USER_JACK_OID, jackDelta.getOid());
        assertEquals("Wrong number of modificaitions", 8, jackDelta.getModifications().size());

        PrismAsserts.assertPropertyReplace(jackDelta, USER_FULLNAME_QNAME, "Jack Sparrow");

        PrismAsserts.assertPropertyDelete(jackDelta, ItemPath.create(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "dva");
        PrismAsserts.assertPropertyAdd(jackDelta, ItemPath.create(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "osem");
        // TODO: assert BAR

        PrismAsserts.assertPropertyDelete(jackDelta, USER_ADDITIONALNAMES_QNAME, "Captain");

        PrismAsserts.assertPropertyAdd(jackDelta, USER_LOCALITY_QNAME, "World's End");

        // There won't be any activation deltas. Activation is operational.
        PrismAsserts.assertNoItemDelta(jackDelta, USER_ENABLED_PATH);
        PrismAsserts.assertNoItemDelta(jackDelta, USER_VALID_FROM_PATH);

        PrismAsserts.assertPropertyReplace(jackDelta,
                ItemPath.create(USER_ASSIGNMENT_QNAME, USER_ASSIGNMENT_2_ID, USER_DESCRIPTION_QNAME),
                "Assignment II");

        ContainerDelta<?> assignment3Delta = PrismAsserts.assertContainerAddGetContainerDelta(jackDelta, USER_ASSIGNMENT_QNAME);
        PrismContainerValue<?> assignment3DeltaAddValue = assignment3Delta.getValuesToAdd().iterator().next();
        assertEquals("Assignment 3 wrong ID", USER_ASSIGNMENT_3_ID, assignment3DeltaAddValue.getId());

        // TODO assert assignment[i1112]/accountConstruction
    }


    /**
     * Parse original jack and modified Jack. Diff and assert if the resulting
     * delta is OK.
     * This is literal diff. All the changes should be part of the resulting delta.
     */
    @Test
    public void testDiffJackLiteral() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> jackOriginal = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

        PrismObject<UserType> jackModified = prismContext.parseObject(getFile(USER_JACK_MODIFIED_FILE_BASENAME));

        // WHEN
        ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified, ParameterizedEquivalenceStrategy.DATA);

        // THEN
        System.out.println("Jack delta:");
        System.out.println(jackDelta.debugDump());

        jackDelta.assertDefinitions();
        jackDelta.checkConsistence(true, true, true);

        assertEquals("Wrong delta type", ChangeType.MODIFY, jackDelta.getChangeType());
        assertEquals("Wrong delta OID", USER_JACK_OID, jackDelta.getOid());
        assertEquals("Wrong number of modificaitions", 10, jackDelta.getModifications().size());

        PrismAsserts.assertPropertyReplace(jackDelta, USER_FULLNAME_QNAME, "Jack Sparrow");

        PrismAsserts.assertPropertyDelete(jackDelta, ItemPath.create(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "dva");
        PrismAsserts.assertPropertyAdd(jackDelta, ItemPath.create(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "osem");
        // TODO: assert BAR

        PrismAsserts.assertPropertyDelete(jackDelta, USER_ADDITIONALNAMES_QNAME, "Captain");

        PrismAsserts.assertPropertyAdd(jackDelta, USER_LOCALITY_QNAME, "World's End");

        PrismAsserts.assertPropertyReplace(jackDelta, USER_ENABLED_PATH, false);
        PrismAsserts.assertPropertyDelete(jackDelta, USER_VALID_FROM_PATH, USER_JACK_VALID_FROM);

        PrismAsserts.assertPropertyReplace(jackDelta,
                ItemPath.create(USER_ASSIGNMENT_QNAME, USER_ASSIGNMENT_2_ID, USER_DESCRIPTION_QNAME),
                "Assignment II");

        ContainerDelta<?> assignment3Delta = PrismAsserts.assertContainerAddGetContainerDelta(jackDelta, USER_ASSIGNMENT_QNAME);
        PrismContainerValue<?> assignment3DeltaAddValue = assignment3Delta.getValuesToAdd().iterator().next();
        assertEquals("Assignment 3 wrong ID", USER_ASSIGNMENT_3_ID, assignment3DeltaAddValue.getId());

        // TODO assert assignment[i1112]/accountConstruction
    }

    @Test
    public void testDiffPatchRoundTrip() throws SchemaException, SAXException, IOException {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> jackOriginal = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

        PrismObject<UserType> jackModified = prismContext.parseObject(getFile(USER_JACK_MODIFIED_FILE_BASENAME));

        ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified);

        jackDelta.assertDefinitions();
        jackDelta.checkConsistence(true, true, true);

        // WHEN
        jackDelta.applyTo(jackOriginal);

        // THEN
        assertTrue("Roundtrip failed", jackOriginal.equivalent(jackModified));
    }

    @Test
    public void testEqualsReferenceValues() throws Exception {
        PrismContext prismContext = constructInitializedPrismContext();

        PrismReferenceValue val11 = new PrismReferenceValueImpl("oid1");
        val11.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val12 = new PrismReferenceValueImpl("oid1");
        val12.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val13 = new PrismReferenceValueImpl("oid1");
        // No type

        PrismReferenceValue val21 = new PrismReferenceValueImpl();
        val21.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val22 = new PrismReferenceValueImpl();
        val22.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val23 = new PrismReferenceValueImpl();
        // No type

        PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

        PrismReferenceValue val31 = new PrismReferenceValueImpl();
        val31.setObject(user);

        PrismReferenceValue val32 = new PrismReferenceValueImpl();
        val32.setObject(user.clone());

        PrismReferenceValue val33 = new PrismReferenceValueImpl();
        // No type, no object

        PrismReferenceValue val34 = new PrismReferenceValueImpl();
        PrismObject<UserType> differentUser = user.clone();
        differentUser.setOid(null);
        differentUser.setPropertyRealValue(UserType.F_FULL_NAME, "Jack Different");
        val34.setObject(differentUser);

        PrismReferenceValue val35 = new PrismReferenceValueImpl();
        PrismObject<UserType> yetAnotherDifferentUser = user.clone();
        yetAnotherDifferentUser.setOid(null);
        yetAnotherDifferentUser.setPropertyRealValue(UserType.F_FULL_NAME, "John J Random");
        val35.setObject(yetAnotherDifferentUser);

        assertTrue("val11 - val11", val11.equals(val11));
        assertTrue("val11 - val12", val11.equals(val12));
        assertTrue("val12 - val11", val12.equals(val11));
        assertFalse("val11 - val13", val11.equals(val13));
        assertFalse("val13 - val11", val13.equals(val11));

        assertTrue("val21 - val21", val21.equals(val21));
        assertTrue("val21 - val22", val21.equals(val22));
        assertTrue("val22 - val21", val22.equals(val21));
        assertFalse("val21 - val23", val21.equals(val23));
        assertFalse("val23 - val21", val23.equals(val21));

        assertTrue("val31 - val31", val31.equals(val31));
        assertTrue("val31 - val32", val31.equals(val32));
        assertTrue("val32 - val31", val32.equals(val31));
        assertFalse("val31 - val33", val31.equals(val33));
        assertFalse("val33 - val31", val33.equals(val31));
        assertFalse("val31 - val34", val31.equals(val34));
        assertFalse("val34 - val31", val34.equals(val31));
        assertFalse("val31 - val35", val31.equals(val35));
        assertFalse("val35 - val31", val35.equals(val31));
        assertFalse("val34 - val35", val34.equals(val35));
        assertFalse("val35 - val34", val35.equals(val34));

    }

    @Test
    public void testEqualsReferenceValuesSchema() throws Exception {
        PrismContext prismContext = constructInitializedPrismContext();

        MutablePrismReferenceDefinition ref1Def = prismContext.definitionFactory().createReferenceDefinition(REF_QNAME, REF_TYPE_QNAME);
        ref1Def.setTargetTypeName(ACCOUNT_TYPE_QNAME);

        PrismReference ref1a = prismContext.itemFactory().createReference(REF_QNAME, ref1Def);
        PrismReferenceValue val11 = new PrismReferenceValueImpl("oid1");
        val11.setTargetType(ACCOUNT_TYPE_QNAME);
        assertTrue(ref1a.add(val11));

        PrismReferenceValue val12 = new PrismReferenceValueImpl("oid1");
        val12.setTargetType(ACCOUNT_TYPE_QNAME);
        assertFalse(ref1a.add(val12));

        PrismReference ref1b = prismContext.itemFactory().createReference(REF_QNAME, ref1Def);
        PrismReferenceValue val13 = new PrismReferenceValueImpl("oid1");
        // No type
        assertTrue(ref1b.add(val13));

        PrismReferenceValue val14 = new PrismReferenceValueImpl("oid1");
        // No type
        assertFalse(ref1b.add(val14));

        PrismReferenceDefinition ref2Def = prismContext.definitionFactory().createReferenceDefinition(REF_QNAME, REF_TYPE_QNAME);
        // no target type def

        PrismReference ref2a = prismContext.itemFactory().createReference(REF_QNAME, ref2Def);
        PrismReferenceValue val21 = new PrismReferenceValueImpl("oid1");
        val21.setTargetType(ACCOUNT_TYPE_QNAME);
        assertTrue(ref2a.add(val21));

        PrismReferenceValue val22 = new PrismReferenceValueImpl("oid1");
        val22.setTargetType(ACCOUNT_TYPE_QNAME);
        assertFalse(ref2a.add(val22));

        PrismReference ref2b = prismContext.itemFactory().createReference(REF_QNAME, ref2Def);
        PrismReferenceValue val23 = new PrismReferenceValueImpl("oid1");
        // No type
        assertTrue(ref2b.add(val23));

        // No def in val4x

        PrismReferenceValue val41 = new PrismReferenceValueImpl("oid1");
        val41.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val42 = new PrismReferenceValueImpl("oid1");
        val42.setTargetType(ACCOUNT_TYPE_QNAME);

        PrismReferenceValue val43 = new PrismReferenceValueImpl("oid1");
        // No type


        assertTrue("val11 - val11", val11.equals(val11));
        assertTrue("val11 - val12", val11.equals(val12));
        assertTrue("val12 - val11", val12.equals(val11));
        assertTrue("val11 - val13", val11.equals(val13));
        assertTrue("val13 - val11", val13.equals(val11));
        assertFalse("val13 - val14", val13.equals(val14));      // val14 has no type because it is not in ref1b

        assertTrue("val21 - val21", val21.equals(val21));
        assertTrue("val21 - val22", val21.equals(val22));
        assertTrue("val22 - val21", val22.equals(val21));
        assertFalse("val21 - val23", val21.equals(val23));
        assertFalse("val23 - val21", val23.equals(val21));

        assertTrue("val41 - val41", val41.equals(val41));
        assertTrue("val41 - val42", val41.equals(val42));
        assertTrue("val42 - val41", val42.equals(val41));
        assertFalse("val41 - val43", val41.equals(val43));
        assertFalse("val43 - val41", val43.equals(val41));

        assertTrue("val11 - val21", val11.equals(val21));
        assertTrue("val11 - val41", val11.equals(val41));

        assertTrue("val41 - val11", val41.equals(val11));
        assertTrue("val41 - val21", val41.equals(val12));

        assertTrue("val13 - val21", val13.equals(val21));
        assertTrue("val13 - val41", val13.equals(val41));

        assertFalse("val43 - val11", val43.equals(val11));
        assertFalse("val43 - val12", val43.equals(val12));
        assertFalse("val43 - val13", val43.equals(val13));

        assertFalse("val43 - val21", val43.equals(val21));
        assertFalse("val43 - val22", val43.equals(val22));
        assertTrue("val43 - val23", val43.equals(val23));
    }

    @Test
    public void testDiffReferences() throws Exception {
        if (!"xml".equals(getFilenameSuffix())) {
            return;
        }

        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> refWithFilter = prismContext.parserFor(getFile(REF_WITH_FILTER_BASENAME)).parse();
        PrismObject<UserType> refWithFilter2 = prismContext.parserFor(getFile(REF_WITH_FILTER_BASENAME)).parse();
        PrismObject<UserType> refWithFilterDifferentPath = prismContext.parserFor(getFile(REF_WITH_FILTER_DIFFERENT_PATH_BASENAME)).parse();
        PrismObject<UserType> refWithFilterNoOid = prismContext.parserFor(getFile(REF_WITH_FILTER_NO_OID_BASENAME)).parse();
        PrismObject<UserType> refWithFilterNoOid2 = prismContext.parserFor(getFile(REF_WITH_FILTER_NO_OID_BASENAME)).parse();
        PrismObject<UserType> refWithoutFilter = prismContext.parserFor(getFile(REF_WITHOUT_FILTER_BASENAME)).parse();

        // WHEN/THEN

        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, null);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, EquivalenceStrategy.LITERAL);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, EquivalenceStrategy.DATA);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, EquivalenceStrategy.IGNORE_METADATA);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, EquivalenceStrategy.REAL_VALUE);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilter2, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);

        assertInequality("refWithFilter - refWithFilterDifferentPath", refWithFilter, refWithFilterDifferentPath, null);
        assertInequality("refWithFilter - refWithFilterDifferentPath", refWithFilter, refWithFilterDifferentPath, EquivalenceStrategy.LITERAL);
        // if the filter was parsed this should be an equality
        assertInequality("refWithFilter - refWithFilterDifferentPath", refWithFilter, refWithFilterDifferentPath, EquivalenceStrategy.DATA);
        assertInequality("refWithFilter - refWithFilterDifferentPath", refWithFilter, refWithFilterDifferentPath, EquivalenceStrategy.IGNORE_METADATA);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilterDifferentPath, EquivalenceStrategy.REAL_VALUE);
        assertEquality("refWithFilter - refWithFilter2", refWithFilter, refWithFilterDifferentPath, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);

        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, null);
        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, EquivalenceStrategy.LITERAL);
        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, EquivalenceStrategy.DATA);
        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, EquivalenceStrategy.IGNORE_METADATA);
        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, EquivalenceStrategy.REAL_VALUE);
        assertInequality("refWithFilter - refWithFilterNoOid", refWithFilter, refWithFilterNoOid, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);

        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, null);
        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, EquivalenceStrategy.LITERAL);
        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, EquivalenceStrategy.DATA);
        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, EquivalenceStrategy.IGNORE_METADATA);
        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, EquivalenceStrategy.REAL_VALUE);
        assertEquality("refWithFilterNoOid2 - refWithFilterNoOid", refWithFilterNoOid2, refWithFilterNoOid, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);

        assertInequality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, null);
        assertInequality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, EquivalenceStrategy.LITERAL);
        assertInequality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, EquivalenceStrategy.DATA);
        assertInequality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, EquivalenceStrategy.IGNORE_METADATA);
        assertEquality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, EquivalenceStrategy.REAL_VALUE);
        assertEquality("refWithFilter - refWithoutFilter", refWithFilter, refWithoutFilter, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    protected void assertEquality(String message, PrismObject<?> o1, PrismObject<?> o2, ParameterizedEquivalenceStrategy strategy) {
        boolean equals = strategy != null ? o1.equals(o2, strategy) : o1.equals(o2);
        assertTrue(message + " under '" + strategy + "' should be equivalent but it is not", equals);
    }
    protected void assertInequality(String message, PrismObject<?> o1, PrismObject<?> o2, ParameterizedEquivalenceStrategy strategy) {
        boolean equals = strategy != null ? o1.equals(o2, strategy) : o1.equals(o2);
        assertFalse(message + " under '" + strategy + "' should not be equivalent but it is", equals);
    }

}
