/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.fail;

import static com.evolveum.midpoint.model.common.mapping.MappingTestEvaluator.TEST_DIR;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests how well mapping evaluation works with the value metadata.
 */
public class TestMappingMetadata extends AbstractModelCommonTest {

    private static final String MAPPING_SCRIPT_FULLNAME_METADATA_XML = "mapping-script-fullname-metadata.xml";
    private static final String MAPPING_ASIS_FULLNAME_METADATA_XML = "mapping-asis-fullname-metadata.xml";
    private static final String MAPPING_ASIS_FULLNAME_METADATA_RANGE_XML = "mapping-asis-fullname-metadata-range.xml";
    private static final String MAPPING_ASIS_FULLNAME_METADATA_RANGE_ALL_XML = "mapping-asis-fullname-metadata-range-all.xml";
    private static final String MAPPING_PATH_FULLNAME_METADATA_XML = "mapping-path-fullname-metadata.xml";
    private static final String MAPPING_CONST_FULLNAME_METADATA_XML = "mapping-const-fullname-metadata.xml";
    private static final String MAPPING_GENERATE_FULLNAME_METADATA_XML = "mapping-generate-fullname-metadata.xml";
    private static final String MAPPING_VALUE_FULLNAME_METADATA_XML = "mapping-value-fullname-metadata.xml";
    private MappingTestEvaluator evaluator;

    private static final TestObject<ObjectTemplateType> TEMPLATE_PROVENANCE =
            TestObject.file(TEST_DIR, "template-provenance.xml", "0ca5cef4-6df3-42c3-82b6-daae691e960d");

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.initWithMetadata();
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: ADD object
     *
     * Expected result: user, rest (in plus set)
     *
     * Here we create the user and verify acquisition for computed fullName. It should be in the plus set, because
     * both sources are being added.
     */
    @Test
    public void testUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: none
     *
     * Expected result: user, rest (in zero set)
     */
    @Test
    public void testRecompute() throws Exception {
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, null);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("hr").
     *
     * Expected result:
     * - zero: user, rest, hr (because no real value is being added).
     *
     * See also the following test for deleting existing metadata.
     */
    @Test
    public void testAddSparrowHr() throws Exception {
        PrismPropertyValue<PolyString> sparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(sparrowHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "rest", "hr");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     * target (Jack Sparrow) has origins: user, rest (in the form of m:user+rest).
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("hr").
     *
     * Expected result:
     * - zero: user, rest, hr (because no real value is being added)
     * - minus: user, rest (because mapping recognizes its own previous output, even without range setting)
     */
    @Test
    public void testAddSparrowHrWithExisting() throws Exception {
        PrismPropertyValue<PolyString> sparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(sparrowHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta,
                existingValuesCustomizer(createJackSparrowOriginal()));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "rest", "hr");
        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("user") - phantom add
     *
     * Expected result: user, rest (in zero set, because no real value is being added).
     */
    @Test
    public void testAddSparrowUserPhantom() throws Exception {
        PrismPropertyValue<PolyString> sparrowUserWithOriginRef =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUserWithOriginRef.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(sparrowUserWithOriginRef)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        PrismPropertyValue<PolyString> jackSparrow = MiscUtil.extractSingleton(outputTriple.getZeroSet());
        assertOrigins(jackSparrow, "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     * target (Jack Sparrow) has origins: user, rest (in the form of m:user+rest).
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("user") - phantom add
     *
     * Expected result: user, rest (in zero set, because no real value is being added).
     */
    @Test
    public void testAddSparrowUserPhantomWithExisting() throws Exception {
        PrismPropertyValue<PolyString> sparrowUserWithOriginRef =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUserWithOriginRef.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(sparrowUserWithOriginRef)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta,
                existingValuesCustomizer(createJackSparrowOriginal()));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        PrismPropertyValue<PolyString> jackSparrow = MiscUtil.extractSingleton(outputTriple.getZeroSet());
        assertOrigins(jackSparrow, "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add givenName Jack (rest+timestamp).
     *
     * Expected result: user, rest+timestamp (in zero set, because no real value is being added).
     */
    @Test
    public void testAddJackRestWithTimestamp() throws Exception {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismPropertyValue<PolyString> jackRestWithTs =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackRestWithTs.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("rest", ServiceType.COMPLEX_TYPE)
                .timestamp(now);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add(jackRestWithTs)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        PrismPropertyValue<PolyString> jackSparrow = MiscUtil.extractSingleton(outputTriple.getZeroSet());
        assertOrigins(jackSparrow, "user", "rest");

        List<ProvenanceAcquisitionType> acquisitions = ((ValueMetadataType) jackSparrow.getValueMetadata().getValue().asContainerable()).getProvenance().getAcquisition();
        XMLGregorianCalendar timestamp = acquisitions.stream().filter(acq -> acq.getOriginRef().getOid().equals("rest"))
                .findAny()
                .orElseThrow(() -> new AssertionError("no origin rest value"))
                .getTimestamp();
        assertThat(timestamp).isEqualTo(now);
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     * target (Jack Sparrow) has origins: user, rest (in the form of m:user+rest).
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: add givenName Jack (rest+timestamp).
     *
     * Expected result: user, rest+timestamp (in zero set, because no real value is being added).
     */
    @Test
    public void testAddJackRestWithTimestampWithExisting() throws Exception {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismPropertyValue<PolyString> jackRestWithTs =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackRestWithTs.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("rest", ServiceType.COMPLEX_TYPE)
                .timestamp(now);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add(jackRestWithTs)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta,
                existingValuesCustomizer(createJackSparrowOriginal()));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        PrismPropertyValue<PolyString> jackSparrow = MiscUtil.extractSingleton(outputTriple.getZeroSet());
        assertOrigins(jackSparrow, "user", "rest");

        List<ProvenanceAcquisitionType> acquisitions = ((ValueMetadataType) jackSparrow.getValueMetadata().getValue().asContainerable()).getProvenance().getAcquisition();
        XMLGregorianCalendar timestamp = acquisitions.stream().filter(acq -> acq.getOriginRef().getOid().equals("rest"))
                .findAny()
                .orElseThrow(() -> new AssertionError("no origin rest value"))
                .getTimestamp();
        assertThat(timestamp).isEqualTo(now);
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jack ("hr")
     *
     * Expected result: hr, user (zero set).
     */
    @Test
    public void testReplaceJackHr() throws Exception {
        PrismPropertyValue<PolyString> jackHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "hr");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     * target (Jack Sparrow) has origins: user, rest (in the form of m:user+rest).
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jack ("hr")
     *
     * Expected result:
     * - zero: hr, user
     * - minus: user, rest
     */
    @Test
    public void testReplaceJackHrWithExisting() throws Exception {
        PrismPropertyValue<PolyString> jackHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta,
                existingValuesCustomizer(createJackSparrowOriginal()));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "hr");
        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete givenName Jack ("rest")
     *
     * Expected result: user (zero set, because real value is not being added nor deleted).
     */
    @Test
    public void testDeleteJackRest() throws Exception {
        PrismPropertyValue<PolyString> jackRest =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackRest.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("rest", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).delete(jackRest)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete familyName Sparrow (user)
     *
     * Expected result: Jack Sparrow (no metadata) in delete set.
     * We do not know the metadata because we do not have the original target value.
     */
    @Test
    public void testDeleteSparrowUser() throws Exception {
        PrismPropertyValue<PolyString> sparrowUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).delete(sparrowUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     * target (Jack Sparrow) has origins: user, rest (in the form of m:user+rest).
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete familyName Sparrow (user)
     *
     * Expected result: Jack Sparrow (user, rest) in delete set.
     */
    @Test
    public void testDeleteSparrowUserWithExisting() throws Exception {
        PrismPropertyValue<PolyString> sparrowUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).delete(sparrowUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta,
                existingValuesCustomizer(createJackSparrowOriginal()));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete familyName Sparrow (custom) -- phantom delete
     *
     * Expected result: Jack Sparrow (user, rest) in zero set.
     */
    @Test
    public void testDeleteSparrowCustomPhantom() throws Exception {
        PrismPropertyValue<PolyString> sparrowUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("custom", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).delete(sparrowUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete givenName Jack (no metadata = all values).
     *
     * Expected result: Jack Sparrow (rest, user) in delete set.
     */
    @Test
    public void testDeleteJackAll() throws Exception {
        PrismPropertyValue<PolyString> jack =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).delete(jack)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: delete givenName Jack + delete familyName Sparrow (no metadata = all values).
     *
     * Expected result: Jack Sparrow (rest, user) in delete set.
     */
    @Test
    public void testDeleteJackAndSparrowAll() throws Exception {
        PrismPropertyValue<PolyString> jack =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jack"));
        PrismPropertyValue<PolyString> sparrow =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).delete(jack)
                .item(UserType.F_FAMILY_NAME).delete(sparrow)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jackie ("hr")
     *
     * Expected result:
     * - Jackie Sparrow: hr, user (plus set),
     * - Jack Sparrow: user, rest (minus set).
     */
    @Test
    public void testReplaceJackieHr() throws Exception {
        PrismPropertyValue<PolyString> jackieHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackieHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user", "hr");
        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jackie ("hr"), replace familyName Sparrow ("hr")
     *
     * Expected result:
     * - Jackie Sparrow: hr (plus set),
     * - Jack Sparrow: user, rest (minus set).
     */
    @Test
    public void testReplaceJackieHrReplaceSparrowHr() throws Exception {
        PrismPropertyValue<PolyString> jackieHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        PrismPropertyValue<PolyString> sparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackieHr)
                .item(UserType.F_FAMILY_NAME).replace(sparrowHr)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "hr");
        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Now testing asIs evaluator (givenName -> fullName)
     *
     * Jack has acquisition origins: user, rest
     *
     * Delta: ADD object
     *
     * Expected result: user, rest (in plus set)
     */
    @Test
    public void testAsIsUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_ASIS_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jack"));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace givenName Jackie (user)
     *
     * Expected result:
     * - Jackie (user) in plus set
     * - Jack (no MD) in minus set
     */
    @Test
    public void testAsIsDeleteSparrowUser() throws Exception {
        PrismPropertyValue<PolyString> jackieUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackieUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_ASIS_FULLNAME_METADATA_XML, delta);

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user");
        assertNoMetadata(MiscUtil.extractSingleton(outputTriple.getMinusSet()));
    }

    /**
     * Jack has acquisition origins: user, rest
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace givenName Jackie (user)
     * Existing: Jack Sparrow (m:hr, user)
     *
     * Expected result:
     * - Jackie (user) in plus set
     * - Jack (no MD) in minus set
     * - Jack Sparrow (m:hr) in minus set (because of range)
     */
    @Test
    public void testAsIsDeleteSparrowUserWithRangeAll() throws Exception {

        PrismPropertyValue<PolyString> jackieUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        PrismPropertyValue<PolyString> jackSparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(PrismTestUtil.createPolyString("Jack Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackSparrowHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .mappingSpecification(createMappingSpec())
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE)
                .<ProvenanceMetadataType>end()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE)
                .end();

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackieUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_ASIS_FULLNAME_METADATA_RANGE_ALL_XML, delta,
                        existingValuesCustomizer(jackSparrowHr));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack"), PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user");
        //noinspection ConstantConditions
        assertNoMetadata(outputTriple.getMinusSet().stream().filter(v -> v.getRealValue().getOrig().equals("Jack")).findAny().orElse(null));
        //noinspection ConstantConditions
        assertOrigins(outputTriple.getMinusSet().stream().filter(v -> v.getRealValue().getOrig().equals("Jack Sparrow")).findAny().orElse(null), "hr", "user");
    }

    private MappingSpecificationType createMappingSpec() {
        return new MappingSpecificationType().mappingName("mapping");
    }

    /**
     * Jack has acquisition origins: user, rest
     *
     * (see user-jack-metadata.xml)
     *
     * Delta: replace givenName Jackie (user)
     * Existing: Jack Sparrow (m:hr, user)
     *
     * Expected result:
     * - Jackie (user) in plus set
     * - Jack (no MD) in minus set
     * - Jack Sparrow (m:hr) in minus set (because of range)
     */
    @Test
    public void testAsIsDeleteSparrowUserWithRange() throws Exception {
        PrismPropertyValue<PolyString> jackieUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE);

        PrismPropertyValue<PolyString> jackSparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(PrismTestUtil.createPolyString("Jack Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackSparrowHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .mappingSpecification(createMappingSpec())
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE)
                .<ProvenanceMetadataType>end()
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE)
                .end();

        ObjectDelta<UserType> delta = evaluator.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(jackieUser)
                .asObjectDelta(null);
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_ASIS_FULLNAME_METADATA_RANGE_XML, delta,
                        existingValuesCustomizer(jackSparrowHr));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie"));
        PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack"), PrismTestUtil.createPolyString("Jack Sparrow"));

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user");
        //noinspection ConstantConditions
        assertNoMetadata(outputTriple.getMinusSet().stream().filter(v -> v.getRealValue().getOrig().equals("Jack")).findAny().orElse(null));
        //noinspection ConstantConditions
        assertOrigins(outputTriple.getMinusSet().stream().filter(v -> v.getRealValue().getOrig().equals("Jack Sparrow")).findAny().orElse(null), "hr", "user");
    }

    /**
     * Now testing path evaluator ($user/givenName -> fullName)
     *
     * Jack has acquisition origins: user, rest
     *
     * Delta: ADD object
     *
     * Expected result: user, rest (in plus set)
     */
    @Test
    public void testPathUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_PATH_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jack"));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getPlusSet()), "user", "rest");
    }

    /**
     * Now testing const evaluator (const:foo -> fullName)
     *
     * Delta: ADD object
     *
     * Expected result: (no origins but mapping spec) (in zero set)
     */
    @Test
    public void testConstUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_CONST_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString(ExpressionTestUtil.CONST_FOO_VALUE));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), SystemObjectsType.ORIGIN_INTERNAL.value());
    }

    /**
     * Now testing literal (value) evaluator (Jack Sparrow -> fullName)
     *
     * Delta: ADD object
     *
     * Expected result: (no origins but mapping spec) (in zero set)
     */
    @Test
    public void testValueUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_VALUE_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), SystemObjectsType.ORIGIN_INTERNAL.value());
    }

    /**
     * Now testing generate evaluator (UUID -> fullName)
     *
     * Delta: ADD object
     *
     * Expected result: (no origins but mapping spec) (in zero set)
     */
    @Test
    public void testGenerateUserAdd() throws Exception {
        PrismObject<UserType> user = evaluator.getUserOld();
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluate(MAPPING_GENERATE_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

        PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getZeroSet()), SystemObjectsType.ORIGIN_INTERNAL.value());
    }

    public MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> evaluate(String filename,
            ObjectDelta<UserType> delta) throws Exception {
        return evaluate(filename, delta, null);
    }

    public MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> evaluate(String filename,
            ObjectDelta<UserType> delta, Consumer<MappingBuilder<?, ?>> mappingBuilderCustomizer) throws Exception {

        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, getTestNameShort(), "fullName", delta,
                        addMappingSpec(mappingBuilderCustomizer));

        Task task = createTask();
        OperationResult result = createOperationResult();

        ModelContext<UserType> lensContext = createFakeLensContext();

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment<>(lensContext, null, task, result));

        try {

            // WHEN
            mapping.evaluate(task, result);

        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }

        // THEN
        return mapping;
    }

    private Consumer<MappingBuilder<?, ?>> addMappingSpec(Consumer<MappingBuilder<?, ?>> customizer) {
        return mappingBuilder -> {
            mappingBuilder.mappingSpecification(createMappingSpec());
            if (customizer != null) {
                customizer.accept(mappingBuilder);
            }
        };
    }

    private void assertNoMetadata(PrismValue value) {
        if (value.hasValueMetadata()) {
            fail("Value " + value + " has unexpected metadata:\n" + value.getValueMetadataAsContainer().debugDump());
        }
    }

    private void assertOrigins(PrismValue value, String... origins) {
        ValueMetadata metadataContainer = value.getValueMetadata();
        assertThat(metadataContainer.size()).isEqualTo(1);
        ValueMetadataType metadata = (ValueMetadataType) metadataContainer.getValue().asContainerable();
        assertThat(metadata.getProvenance()).isNotNull();
        assertThat(metadata.getProvenance().getMappingSpecification()).as("mapping spec").isNotNull();
        assertThat(metadata.getProvenance().getMappingSpecification().getMappingName()).as("mapping name").isEqualTo("mapping");
        assertThat(metadata.getProvenance().getAcquisition().size()).isEqualTo(origins.length);
        Set<String> realOrigins = metadata.getProvenance().getAcquisition().stream()
                .map(acq -> acq.getOriginRef().getOid())
                .collect(Collectors.toSet());
        assertThat(realOrigins).as("real origins").containsExactlyInAnyOrder(origins);
    }

    @NotNull
    private PrismPropertyValue<PolyString> createJackSparrowOriginal() {
        PrismPropertyValue<PolyString> jackSparrowOriginal =
                evaluator.getPrismContext().itemFactory().createPropertyValue(PrismTestUtil.createPolyString("Jack Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackSparrowOriginal.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .mappingSpecification(createMappingSpec())
                .beginAcquisition()
                .originRef("user", ServiceType.COMPLEX_TYPE)
                .<ProvenanceMetadataType>end()
                .beginAcquisition()
                .originRef("rest", ServiceType.COMPLEX_TYPE)
                .end();
        return jackSparrowOriginal;
    }

    private Consumer<MappingBuilder> existingValuesCustomizer(PolyString... values) {
        return mappingBuilder -> {
            //noinspection unchecked
            mappingBuilder.originalTargetValues(
                    Arrays.stream(values)
                            .map(value -> evaluator.getPrismContext().itemFactory().createPropertyValue(value))
                            .collect(Collectors.toList()));
        };
    }

    private Consumer<MappingBuilder<?, ?>> existingValuesCustomizer(PrismValue... values) {
        return mappingBuilder -> {
            //noinspection unchecked,rawtypes
            mappingBuilder.originalTargetValues((List) (Arrays.asList(values)));
        };
    }

    private ModelContext<UserType> createFakeLensContext() {
        return new ModelContext<>() {
            @Override
            public String getRequestIdentifier() {
                return null;
            }

            @Override
            public ModelState getState() {
                return null;
            }

            @Override
            public ModelElementContext<UserType> getFocusContext() {
                return null;
            }

            @Override
            public @NotNull ModelElementContext<UserType> getFocusContextRequired() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @NotNull Collection<? extends ModelProjectionContext> getProjectionContexts() {
                return Collections.emptyList();
            }

            @Override
            public ModelProjectionContext findProjectionContextByKeyExact(
                    @NotNull ProjectionContextKey key) {
                return null;
            }

            @Override
            public @NotNull Collection<ModelProjectionContext> findProjectionContexts(@NotNull ProjectionContextFilter filter) {
                return List.of();
            }

            @Override
            public ModelExecuteOptions getOptions() {
                return null;
            }

            @Override
            public @NotNull PartialProcessingOptionsType getPartialProcessingOptions() {
                return new PartialProcessingOptionsType();
            }

            @Override
            public Class<UserType> getFocusClass() {
                return UserType.class;
            }

            @Override
            public void reportProgress(ProgressInformation progress) {
            }

            @Override
            public DeltaSetTriple<? extends EvaluatedAssignment> getEvaluatedAssignmentTriple() {
                return null;
            }

            @Override
            public @NotNull Stream<? extends EvaluatedAssignment> getEvaluatedAssignmentsStream() {
                return Stream.empty();
            }

            @Override
            public @NotNull Collection<? extends EvaluatedAssignment> getNonNegativeEvaluatedAssignments() {
                return Collections.emptyList();
            }

            @Override
            public @NotNull Collection<? extends EvaluatedAssignment> getAllEvaluatedAssignments() {
                return Collections.emptyList();
            }

            @Override
            public ObjectTemplateType getFocusTemplate() {
                return TEMPLATE_PROVENANCE.getObjectable();
            }

            @Override
            public PrismObject<SystemConfigurationType> getSystemConfiguration() {
                return null;
            }

            @Override
            public String getChannel() {
                return null;
            }

            @Override
            public int getAllChanges() {
                return 0;
            }

            @Override
            public String dumpAssignmentPolicyRules(int indent, boolean alsoMessages) {
                return null;
            }

            @Override
            public String dumpObjectPolicyRules(int indent, boolean alsoMessages) {
                return null;
            }

            @Override
            public Map<String, Collection<? extends Containerable>> getHookPreviewResultsMap() {
                return null;
            }

            @Override
            public @NotNull <T> List<T> getHookPreviewResults(@NotNull Class<T> clazz) {
                return Collections.emptyList();
            }

            @Override
            public @Nullable PolicyRuleEnforcerPreviewOutputType getPolicyRuleEnforcerPreviewOutput() {
                return null;
            }

            @Override
            public @NotNull ObjectTreeDeltas<UserType> getTreeDeltas() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<ProjectionContextKey> getHistoricResourceObjects() {
                return null;
            }

            @Override
            public Long getSequenceCounter(String sequenceOid) {
                return null;
            }

            @Override
            public void setSequenceCounter(String sequenceOid, long counter) {

            }

            @Override
            public String debugDump(int indent) {
                return null;
            }

            @Override
            public String getTaskTreeOid(Task task, OperationResult result) {
                return null;
            }

            @Override
            public ExpressionProfile getPrivilegedExpressionProfile() {
                return null;
            }

            @Override
            public @NotNull TaskExecutionMode getTaskExecutionMode() {
                return TaskExecutionMode.PRODUCTION;
            }
        };
    }

}
