/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.common.mapping.MappingTestEvaluator.TEST_DIR;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Tests how well mapping evaluation works with the value metadata.
 */
public class TestMappingMetadata extends AbstractModelCommonTest {

    private static final String MAPPING_SCRIPT_FULLNAME_METADATA_XML = "mapping-script-fullname-metadata.xml";
    private MappingTestEvaluator evaluator;

    private static final TestResource<ObjectTemplateType> TEMPLATE_PROVENANCE = new TestResource<>(TEST_DIR, "template-provenance.xml", "0ca5cef4-6df3-42c3-82b6-daae691e960d");

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.initWithMetadata();

        TEMPLATE_PROVENANCE.read(evaluator.getPrismContext());
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     *    (see user-jack-metadata.xml)
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
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping = evaluate(MAPPING_SCRIPT_FULLNAME_METADATA_XML, DeltaFactory.Object.createAddDelta(user));

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
     *    (see user-jack-metadata.xml)
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
     *    (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("hr").
     *
     * Expected result: user, rest, hr (in zero set, because no real value is being added).
     */
    @Test
    public void testAddSparrowHr() throws Exception {
        PrismPropertyValue<PolyString> sparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                    .beginYield()
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
     *
     *    (see user-jack-metadata.xml)
     *
     * Delta: add familyName Sparrow ("user").
     *
     * Expected result: user, rest (in zero set, because no real value is being added).
     */
    @Test
    public void testAddSparrowUser() throws Exception {
        PrismPropertyValue<PolyString> sparrowUserWithOriginRef =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUserWithOriginRef.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                    .beginYield()
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
     *
     *    (see user-jack-metadata.xml)
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
                    .beginYield()
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

        List<ProvenanceAcquisitionType> acquisitions = ((ValueMetadataType) jackSparrow.getValueMetadata().getValue().asContainerable()).getProvenance().getYield().get(0).getAcquisition();
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
     *    (see user-jack-metadata.xml)
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
                    .beginYield()
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
     *
     *    (see user-jack-metadata.xml)
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
                    .beginYield()
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
     *    (see user-jack-metadata.xml)
     *
     * Delta: delete familyName Sparrow (user)
     *
     * Expected result: Jack Sparrow (rest, user) in delete set.
     */
    @Test
    public void testDeleteSparrowUser() throws Exception {
        PrismPropertyValue<PolyString> sparrowUser =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) sparrowUser.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                    .beginYield()
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

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     *    (see user-jack-metadata.xml)
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

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     *    (see user-jack-metadata.xml)
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

        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     *    (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jackie ("hr")
     *
     * Expected result:
     *  - Jackie Sparrow: hr, user (plus set),
     *  - Jack Sparrow: user, rest (minus set).
     */
    @Test
    public void testReplaceJackieHr() throws Exception {
        PrismPropertyValue<PolyString> jackieHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginYield()
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
        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    /**
     * Jack has acquisition origins: user, rest
     * Sparrow has acquisition origins: user
     *
     *    (see user-jack-metadata.xml)
     *
     * Delta: replace giveName Jackie ("hr"), replace familyName Sparrow ("hr")
     *
     * Expected result:
     *  - Jackie Sparrow: hr (plus set),
     *  - Jack Sparrow: user, rest (minus set).
     */
    @Test
    public void testReplaceJackieHrReplaceSparrowHr() throws Exception {
        PrismPropertyValue<PolyString> jackieHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Jackie"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginYield()
                .beginAcquisition()
                .originRef("hr", ServiceType.COMPLEX_TYPE);

        PrismPropertyValue<PolyString> sparrowHr =
                evaluator.getPrismContext().itemFactory().createPropertyValue(new PolyString("Sparrow"));
        //noinspection unchecked
        ((PrismContainer<ValueMetadataType>) (PrismContainer) jackieHr.getValueMetadata()).createNewValue().asContainerable()
                .beginProvenance()
                .beginYield()
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
        assertOrigins(MiscUtil.extractSingleton(outputTriple.getMinusSet()), "user", "rest");
    }

    public MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> evaluate(String filename, ObjectDelta<UserType> delta) throws Exception {
        MappingImpl<PrismPropertyValue<PolyString>, PrismPropertyDefinition<PolyString>> mapping =
                evaluator.createMapping(filename, getTestNameShort(), "fullName", delta);

        Task task = createTask();
        OperationResult result = createOperationResult();

        ModelContext<UserType> lensContext = createFakeLensContext();

        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(lensContext, null, task, result));

        // WHEN
        mapping.evaluate(task, result);

        // THEN
        return mapping;
    }

    private void assertOrigins(PrismValue value, String... origins) {
        ValueMetadata metadataContainer = value.getValueMetadata();
        assertThat(metadataContainer.size()).isEqualTo(1);
        ValueMetadataType metadata = (ValueMetadataType) metadataContainer.getValue().asContainerable();
        assertThat(metadata.getProvenance()).isNotNull();
        assertThat(metadata.getProvenance().getYield().size()).isEqualTo(1);
        assertThat(metadata.getProvenance().getYield().get(0).getAcquisition().size()).isEqualTo(origins.length);
        Set<String> realOrigins = metadata.getProvenance().getYield().get(0).getAcquisition().stream()
                .map(acq -> acq.getOriginRef().getOid())
                .collect(Collectors.toSet());
        assertThat(realOrigins).as("real origins").containsExactlyInAnyOrder(origins);
    }

    private ModelContext<UserType> createFakeLensContext() {
        return new ModelContext<UserType>() {
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
            public Collection<? extends ModelProjectionContext> getProjectionContexts() {
                return null;
            }

            @Override
            public ModelProjectionContext findProjectionContext(
                    ResourceShadowDiscriminator rat) {
                return null;
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
            public DeltaSetTriple<? extends EvaluatedAssignment<?>> getEvaluatedAssignmentTriple() {
                return null;
            }

            @Override
            public @NotNull Stream<? extends EvaluatedAssignment<?>> getEvaluatedAssignmentsStream() {
                return Stream.empty();
            }

            @Override
            public @NotNull Collection<? extends EvaluatedAssignment<?>> getNonNegativeEvaluatedAssignments() {
                return Collections.emptyList();
            }

            @Override
            public @NotNull Collection<? extends EvaluatedAssignment<?>> getAllEvaluatedAssignments() {
                return Collections.emptyList();
            }

            @Override
            public PrismContext getPrismContext() {
                return evaluator.getPrismContext();
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
            public String dumpFocusPolicyRules(int indent, boolean alsoMessages) {
                return null;
            }

            @Override
            public Map<String, Collection<Containerable>> getHookPreviewResultsMap() {
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
            public boolean isPreview() {
                return false;
            }

            @Override
            public @NotNull ObjectTreeDeltas<UserType> getTreeDeltas() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<ResourceShadowDiscriminator> getHistoricResourceObjects() {
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
        };
    }

}
