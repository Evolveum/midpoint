/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.CheckedFunction;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public class ShadowAsserter<RA> extends PrismObjectAsserter<ShadowType, RA> {

    /** May or may not be present. Sometimes we want to check only incomplete shadows represented by simple {@link ShadowType}. */
    private final AbstractShadow abstractShadow;

    public ShadowAsserter(AbstractShadow abstractShadow) {
        super(abstractShadow.getPrismObject());
        this.abstractShadow = abstractShadow;
    }

    public ShadowAsserter(AbstractShadow abstractShadow, RA returnAsserter, String details) {
        super(abstractShadow.getPrismObject(), returnAsserter, details);
        this.abstractShadow = abstractShadow;
    }

    public ShadowAsserter(PrismObject<ShadowType> shadow) {
        super(shadow);
        abstractShadow = null;
    }

    public ShadowAsserter(PrismObject<ShadowType> shadow, String details) {
        super(shadow, details);
        abstractShadow = null;
    }

    public ShadowAsserter(PrismObject<ShadowType> shadow, RA returnAsserter, String details) {
        super(shadow, returnAsserter, details);
        abstractShadow = null;
    }

    public static ShadowAsserter<Void> forAbstractShadow(ShadowType bean) {
        return new ShadowAsserter<>(AbstractShadow.of(bean));
    }

    public static ShadowAsserter<Void> forAbstractShadow(@NotNull AbstractShadow shadow) {
        return new ShadowAsserter<>(shadow);
    }

    public static ShadowAsserter<Void> forAbstractShadow(PrismObject<ShadowType> shadow) {
        return forAbstractShadow(shadow.asObjectable());
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            PrismObject<ShadowType> shadow, Collection<? extends QName> cachedAttributes) {
        return new RepoShadowAsserter<>(shadow.asObjectable(), cachedAttributes, null);
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            ShadowType shadow, Collection<? extends QName> cachedAttributes) {
        return new RepoShadowAsserter<>(shadow, cachedAttributes, null);
    }

    public static ShadowAsserter<Void> forShadow(PrismObject<ShadowType> shadow) {
        return new ShadowAsserter<>(shadow);
    }

    public static ShadowAsserter<Void> forShadow(AbstractShadow shadow) {
        return forAbstractShadow(shadow);
    }

    public static ShadowAsserter<Void> forShadow(PrismObject<ShadowType> shadow, String details) {
        return new ShadowAsserter<>(shadow, details);
    }

    @Override
    public ShadowAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertNoOid() {
        super.assertNoOid();
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public ShadowAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public ShadowAsserter<RA> assertPurpose(ShadowPurposeType expected) {
        assertThat(getObjectable().getPurpose()).as("purpose").isEqualTo(expected);
        return this;
    }

    public ShadowAsserter<RA> assertObjectClass() {
        assertNotNull("No objectClass in " + desc(), getObject().asObjectable().getObjectClass());
        return this;
    }

    public ShadowAsserter<RA> assertObjectClass(QName expected) {
        PrismAsserts.assertMatchesQName("Wrong objectClass in " + desc(), expected, getObject().asObjectable().getObjectClass());
        return this;
    }

    /** Let's be strict here wrt namespaces. */
    public ShadowAsserter<RA> assertAuxiliaryObjectClasses(QName... expected) {
        assertThat(getObjectable().getAuxiliaryObjectClass())
                .as("auxiliary object classes")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    public ShadowAsserter<RA> assertKind() {
        assertNotNull("No kind in " + desc(), getObject().asObjectable().getKind());
        return this;
    }

    public ShadowAsserter<RA> assertKind(ShadowKindType expected) {
        assertEquals("Wrong kind in " + desc(), expected, getObject().asObjectable().getKind());
        return this;
    }

    public ShadowAsserter<RA> assertIntent(String expected) {
        assertEquals("Wrong intent in " + desc(), expected, getObject().asObjectable().getIntent());
        return this;
    }

    public ShadowAsserter<RA> assertTag(String expected) {
        assertEquals("Wrong tag in " + desc(), expected, getObject().asObjectable().getTag());
        return this;
    }

    public ShadowAsserter<RA> assertTagIsOid() {
        assertEquals("Wrong tag in " + desc(), getObject().getOid(), getObject().asObjectable().getTag());
        return this;
    }

    public ShadowAsserter<RA> assertIndexedPrimaryIdentifierValue(String expected) {
        assertEquals("Wrong primaryIdentifierValue in " + desc(), expected, getObject().asObjectable().getPrimaryIdentifierValue());
        return this;
    }

    public ShadowAsserter<RA> assertHasIndexedPrimaryIdentifierValue() {
        assertThat(getObjectable().getPrimaryIdentifierValue())
                .as("primary identifier value (indexed)")
                .isNotNull();
        return this;
    }

    /** Only for abstract shadows (now). */
    public ShadowAsserter<RA> assertHasPrimaryIdentifierAttribute() {
        checkAbstractShadowPresent();
        assertThat(abstractShadow.getPrimaryIdentifierAttribute())
                .as("primary identifier attribute")
                .isNotNull();
        return this;
    }

    public ShadowAsserter<RA> assertNoPrimaryIdentifierValue() {
        assertNull("Unexpected primaryIdentifierValue in " + desc(), getObject().asObjectable().getPrimaryIdentifierValue());
        return this;
    }

    public ShadowAsserter<RA> assertIteration(Integer expected) {
        assertEquals("Wrong iteration in " + desc(), expected, getObject().asObjectable().getIteration());
        return this;
    }

    public ShadowAsserter<RA> assertIterationToken(String expected) {
        assertEquals("Wrong iteration token in " + desc(),
                expected, getObject().asObjectable().getIterationToken());
        return this;
    }

    public ShadowAsserter<RA> assertSynchronizationSituation(SynchronizationSituationType expected) {
        assertEquals("Wrong synchronization situation in " + desc(),
                expected, getObject().asObjectable().getSynchronizationSituation());
        return this;
    }

    public ShadowAsserter<RA> assertSynchronizationSituationDescriptionUpdatedButNotFull() {
        return assertSynchronizationSituationDescriptionUpdated(false);
    }

    private ShadowAsserter<RA> assertSynchronizationSituationDescriptionUpdated(boolean full) {
        ShadowType shadow = getObjectable();
        SynchronizationSituationDescriptionType desc = ShadowUtil.getLastSyncSituationDescription(shadow);
        assertThat(desc).as("synchronization situation description").isNotNull();
        assertThat(desc.getSituation())
                .withFailMessage("situation in description differs from the one in shadow")
                .isEqualTo(shadow.getSynchronizationSituation());
        assertThat(desc.getTimestamp())
                .withFailMessage("timestamp in description differs from the one in shadow")
                .isEqualTo(full ? shadow.getFullSynchronizationTimestamp() : shadow.getSynchronizationTimestamp());
        assertThat(desc.isFull())
                .as("'full' flag in situation description")
                .isEqualTo(full);
        return this;
    }

    public ShadowAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
        ActivationType activation = getActivation();
        if (activation == null) {
            if (expected == null) {
                return this;
            } else {
                fail("No activation in " + desc());
            }
        }
        assertEquals("Wrong activation administrativeStatus in " + desc(),
                expected, activation.getAdministrativeStatus());
        return this;
    }

    public ShadowAsserter<RA> assertDisableReason(String reason) {
        ActivationType activation = getActivation();
        if (activation == null) {
            if (reason == null) {
                return this;
            } else {
                fail("No activation in " + desc());
            }
        }
        assertEquals("Wrong disableReason in " + desc(),
                reason, activation.getDisableReason());
        return this;
    }

    public ShadowAsserter<RA> assertDisableTimestamp(long start, long end) {
        TestUtil.assertBetween(
                "disable timestamp", start, end, getDisableTimestamp());
        return this;
    }

    private Long getDisableTimestamp() {
        var activation = getObjectable().getActivation();
        return activation != null ?
                XmlTypeConverter.toMillisNullable(activation.getDisableTimestamp()) :
                null;
    }

    public ShadowAsserter<RA> assertResource(String expectedResourceOid) {
        ObjectReferenceType resourceRef = getObject().asObjectable().getResourceRef();
        if (resourceRef == null) {
            fail("No resourceRef in " + desc());
        }
        assertEquals("Wrong resourceRef OID in " + desc(), expectedResourceOid, resourceRef.getOid());
        return this;
    }

    private ActivationType getActivation() {
        return getObject().asObjectable().getActivation();
    }

    public ShadowAsserter<RA> assertBasicRepoProperties() {
        assertOid();
        assertName();
        assertObjectClass();
        attributes().assertAny();
        return this;
    }

    public ShadowAsserter<RA> assertDead() {
        assertIsDead(true);
        return this;
    }

    public ShadowAsserter<RA> assertNotDead() {
        Boolean isDead = getObject().asObjectable().isDead();
        if (isDead != null && isDead) {
            fail("Wrong isDead in " + desc() + ", expected null or false, but was true");
        }
        return this;
    }

    public ShadowAsserter<RA> assertIsDead(Boolean expected) {
        assertEquals("Wrong isDead in " + desc(), expected, getObject().asObjectable().isDead());
        assertNoPrimaryIdentifierValue();
        return this;
    }

    public ShadowAsserter<RA> assertIsExists() {
        Boolean isExists = getObject().asObjectable().isExists();
        if (isExists != null && !isExists) {
            fail("Wrong isExists in " + desc() + ", expected null or true, but was false");
        }
        return this;
    }

    public ShadowAsserter<RA> assertIsNotExists() {
        assertIsExists(false);
        return this;
    }

    public ShadowAsserter<RA> assertIsExists(Boolean expected) {
        assertEquals("Wrong isExists in " + desc(), expected, getObject().asObjectable().isExists());
        return this;
    }

    public ShadowAsserter<RA> assertConception() {
        assertNotDead();
        assertIsNotExists();
        return this;
    }

    // We cannot really distinguish gestation and life now. But maybe later.
    public ShadowAsserter<RA> assertGestation() {
        assertNotDead();
        assertIsExists();
        return this;
    }

    public ShadowAsserter<RA> assertLive() {
        assertNotDead();
        assertIsExists();
        return this;
    }

    public ShadowAsserter<RA> assertTombstone() {
        assertDead();
        assertIsNotExists();
        return this;
    }

    // We cannot really distinguish corpse and tombstone now. But maybe later.
    public ShadowAsserter<RA> assertCorpse() {
        assertDead();
        assertIsNotExists();
        return this;
    }

    public PendingOperationsAsserter<RA> pendingOperations() {
        PendingOperationsAsserter<RA> asserter = new PendingOperationsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAsserter<RA> hasUnfinishedPendingOperations() {
        pendingOperations()
                .assertUnfinishedOperation();
        return this;
    }

    public ShadowAttributesAsserter<RA> attributes() {
        ShadowAttributesAsserter<RA> asserter = new ShadowAttributesAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAsserter<RA> assertNoAttributes() {
        assertNull("Unexpected attributes in " + desc(), getObject().findContainer(ShadowType.F_ATTRIBUTES));
        return this;
    }

    public ShadowAssociationsAsserter<RA> associations() {
        ShadowAssociationsAsserter<RA> asserter = new ShadowAssociationsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAsserter<RA> assertNoLegacyConsistency() {
        // Nothing to do. Those are gone in midPoint 4.0.
        return this;
    }

    public ShadowAsserter<RA> display() {
        super.display();
        return this;
    }

    public ShadowAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    public ShadowAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    public ShadowAsserter<RA> assertNoPassword() {
        PrismProperty<PolyStringType> passValProp = getPasswordValueProperty();
        assertNull("Unexpected password value property in " + desc() + ": " + passValProp, passValProp);
        return this;
    }

    // To be used after MID-10050 is resolved.
    public ShadowAsserter<RA> assertNoPasswordIf(boolean condition) {
        if (condition) {
            PrismProperty<PolyStringType> passValProp = getPasswordValueProperty();
            assertNull("Unexpected password value property in " + desc() + ": " + passValProp, passValProp);
        }
        return this;
    }

    private PrismProperty<PolyStringType> getPasswordValueProperty() {
        return getObject().findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
    }

    @Override
    public ShadowAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    public ShadowAsserter<RA> assertMatchReferenceId(String id) throws SchemaException {
        assertThat(getIdMatchCorrelatorStateRequired().getReferenceId())
                .as("referenceId")
                .isEqualTo(id);
        return this;
    }

    public ShadowAsserter<RA> assertHasMatchReferenceId() throws SchemaException {
        assertThat(getIdMatchCorrelatorStateRequired().getReferenceId())
                .as("referenceId")
                .isNotNull();
        return this;
    }

    public ShadowAsserter<RA> assertMatchRequestId(String id) throws SchemaException {
        assertThat(getIdMatchCorrelatorStateRequired().getMatchRequestId())
                .as("matchRequestId")
                .isEqualTo(id);
        return this;
    }

    public ShadowAsserter<RA> assertHasMatchRequestId() throws SchemaException {
        assertThat(getIdMatchCorrelatorStateRequired().getMatchRequestId())
                .as("matchRequestId")
                .isNotNull();
        return this;
    }

    private @NotNull ShadowCorrelationStateType getCorrelationStateRequired() {
        return Objects.requireNonNull(
                getObjectable().getCorrelation(), () -> "No correlation state in " + desc());
    }

    private @NotNull AbstractCorrelatorStateType getCorrelatorStateRequired() {
        return Objects.requireNonNull(
                getCorrelationStateRequired().getCorrelatorState(), () -> "No correlator state in " + desc());
    }

    private @NotNull IdMatchCorrelatorStateType getIdMatchCorrelatorStateRequired() throws SchemaException {
        return MiscUtil.castSafely(
                getCorrelatorStateRequired(), IdMatchCorrelatorStateType.class);
    }

    /**
     * Temporary: until correlation state asserter is implemented.
     */
    public ShadowAsserter<RA> assertCorrelationSituation(CorrelationSituationType expected) {
        assertThat(getCorrelationSituation()).as("correlation situation").isEqualTo(expected);
        return this;
    }

    private CorrelationSituationType getCorrelationSituation() {
        ShadowCorrelationStateType correlation = getObjectable().getCorrelation();
        return correlation != null ? correlation.getSituation() : null;
    }

    public ShadowAsserter<RA> assertPotentialOwnerOptions(int expected) {
        assertThat(getPotentialOwnerOptions())
                .as("potential owner options")
                .hasSize(expected);
        return this;
    }

    private List<ResourceObjectOwnerOptionType> getPotentialOwnerOptions() {
        ShadowCorrelationStateType state = getObjectable().getCorrelation();
        if (state == null || state.getOwnerOptions() == null) {
            return List.of();
        } else {
            return state.getOwnerOptions().getOption();
        }
    }

    public ShadowAsserter<RA> assertCandidateOwners(String... expectedOids) {
        assertThat(getCandidateOwnerOids())
                .as("candidate owners OIDs")
                .containsExactlyInAnyOrder(expectedOids);
        return this;
    }

    private Set<String> getCandidateOwnerOids() {
        ShadowCorrelationStateType state = getObjectable().getCorrelation();
        if (state == null || state.getOwnerOptions() == null) {
            return Set.of();
        } else {
            return state.getOwnerOptions().getOption().stream()
                    .map(o -> Referencable.getOid(o.getCandidateOwnerRef()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
    }

    public ShadowAsserter<RA> assertResultingOwner(String expectedOid) {
        assertThat(getResultingOwnerOid())
                .as("resulting owner OID")
                .isEqualTo(expectedOid);
        return this;
    }

    private @Nullable String getResultingOwnerOid() {
        ShadowCorrelationStateType state = getObjectable().getCorrelation();
        return state != null ? Referencable.getOid(state.getResultingOwner()) : null;
    }

    public ShadowAsserter<RA> assertCorrelationCaseOpenTimestampBetween(long start, long end) {
        TestUtil.assertBetween(
                "correlation case open timestamp", start, end, getCorrelationCaseOpenTimestamp());
        return this;
    }

    public ShadowAsserter<RA> assertCorrelationCaseCloseTimestampBetween(long start, long end) {
        TestUtil.assertBetween(
                "correlation case close timestamp", start, end, getCorrelationCaseCloseTimestamp());
        return this;
    }

    public ShadowAsserter<RA> assertCorrelationStartTimestampBetween(long start, long end) {
        TestUtil.assertBetween(
                "correlation start timestamp", start, end, getCorrelationStartTimestamp());
        return this;
    }

    public ShadowAsserter<RA> assertCorrelationEndTimestampBetween(long start, long end) {
        TestUtil.assertBetween(
                "correlation end timestamp", start, end, getCorrelationEndTimestamp());
        return this;
    }

    public ShadowAsserter<RA> assertNoCorrelationCaseCloseTimestamp() {
        assertThat(getCorrelationCaseCloseTimestamp()).as("correlation case close timestamp").isNull();
        return this;
    }

    public ShadowAsserter<RA> assertNoCorrelationEndTimestamp() {
        assertThat(getCorrelationEndTimestamp()).as("correlation case end timestamp").isNull();
        return this;
    }

    private Long getCorrelationCaseOpenTimestamp() {
        ShadowCorrelationStateType correlationState = getObjectable().getCorrelation();
        return correlationState != null ?
                XmlTypeConverter.toMillisNullable(correlationState.getCorrelationCaseOpenTimestamp()) :
                null;
    }

    private Long getCorrelationCaseCloseTimestamp() {
        ShadowCorrelationStateType correlationState = getObjectable().getCorrelation();
        return correlationState != null ?
                XmlTypeConverter.toMillisNullable(correlationState.getCorrelationCaseCloseTimestamp()) :
                null;
    }

    private Long getCorrelationStartTimestamp() {
        ShadowCorrelationStateType correlationState = getObjectable().getCorrelation();
        return correlationState != null ?
                XmlTypeConverter.toMillisNullable(correlationState.getCorrelationStartTimestamp()) :
                null;
    }

    private Long getCorrelationEndTimestamp() {
        ShadowCorrelationStateType correlationState = getObjectable().getCorrelation();
        return correlationState != null ?
                XmlTypeConverter.toMillisNullable(correlationState.getCorrelationEndTimestamp()) :
                null;
    }

    public ShadowAsserter<RA> assertCorrelationPerformers(String... oids) {
        assertThat(getCorrelationPerformers())
                .as("correlation performers OIDs")
                .containsExactlyInAnyOrder(oids);
        return this;
    }

    private Collection<String> getCorrelationPerformers() {
        ShadowCorrelationStateType correlation = getObjectable().getCorrelation();
        return correlation != null ?
                ObjectTypeUtil.getOidsFromRefs(correlation.getPerformerRef()) :
                Set.of();
    }

    public ShadowAsserter<RA> assertCorrelationComments(String... comments) {
        assertThat(getCorrelationComments())
                .as("correlation-related comments")
                .containsExactlyInAnyOrder(comments);
        return this;
    }

    private Collection<String> getCorrelationComments() {
        ShadowCorrelationStateType correlation = getObjectable().getCorrelation();
        return correlation != null ?
                correlation.getPerformerComment() :
                Set.of();
    }

    @SafeVarargs
    public final <T> ShadowAsserter<RA> assertOrigValues(String attrName, T... expectedValues) {
        return assertOrigValues(
                new ItemName(MidPointConstants.NS_RI, attrName),
                expectedValues);
    }

    @SafeVarargs
    public final <T> ShadowAsserter<RA> assertOrigValues(QName attrName, T... expectedValues) {
        return assertAttributeOrigOrNormValues(attrName, ShadowSimpleAttribute::getOrigValues, expectedValues);
    }

    public <T> T getOrigValue(QName attrName) {
        checkAbstractShadowPresent();
        ShadowSimpleAttribute<?> attribute = abstractShadow.findAttribute(attrName);
        if (attribute != null) {
            //noinspection unchecked
            return (T) MiscUtil.extractSingleton(attribute.getOrigValues()); // TODO implement more nicely
        } else {
            return null;
        }
    }

    @SafeVarargs
    public final <T> ShadowAsserter<RA> assertNormValues(String attrName, T... expectedValues) {
        return assertNormValues(
                new ItemName(MidPointConstants.NS_RI, attrName),
                expectedValues);
    }

    @SafeVarargs
    public final <T> ShadowAsserter<RA> assertNormValues(QName attrName, T... expectedValues) {
        return assertAttributeOrigOrNormValues(attrName, ShadowSimpleAttribute::getNormValues, expectedValues);
    }

    @SafeVarargs
    private <T> ShadowAsserter<RA> assertAttributeOrigOrNormValues(
            QName attrName, CheckedFunction<ShadowSimpleAttribute<?>, Collection<?>> extractor, T... expectedValues) {
        checkAbstractShadowPresent();
        ShadowSimpleAttribute<?> attribute = abstractShadow.findAttribute(attrName);
        Collection<?> actualValues;
        try {
            actualValues = attribute != null ? extractor.apply(attribute) : List.of();
        } catch (CommonException e) {
            throw new AssertionError(e);
        }
        //noinspection unchecked
        assertThat((Collection<T>) actualValues)
                .as("values of " + attrName + " in " + abstractShadow)
                .containsExactlyInAnyOrder(expectedValues);
        return this;
    }

    /** Requires {@link #abstractShadow} to be present. */
    public ShadowAsserter<RA> assertAttributes(int expectedNumber) {
        assertThat(ShadowUtil.getAttributesTolerant(getObjectable()))
                .as("attributes")
                .hasSize(expectedNumber);
        return this;
    }

    /** Requires {@link #abstractShadow} to be present. */
    public ShadowAsserter<RA> assertAttributesAtLeast(int expectedNumber) {
        assertThat(ShadowUtil.getAttributesTolerant(getObjectable()))
                .as("attributes")
                .hasSizeGreaterThanOrEqualTo(expectedNumber);
        return this;
    }

    private void checkAbstractShadowPresent() {
        stateCheck(abstractShadow != null, "This assertion is available only when abstractShadow is present.");
    }

    public String getIndexedPrimaryIdentifierValue() {
        return getObjectable().getPrimaryIdentifierValue();
    }

    public String getIndexedPrimaryIdentifierValueRequired() {
        return MiscUtil.stateNonNull(
                getObjectable().getPrimaryIdentifierValue(),
                () -> "No primary identifier value in " + desc());
    }

    public AbstractShadow getAbstractShadow() {
        checkAbstractShadowPresent();
        return abstractShadow;
    }

    public ShadowAsserter<RA> assertFromResourceComplete() {
        assertThat(getObjectable().getContentDescription())
                .as("content description")
                .isEqualTo(ShadowContentDescriptionType.FROM_RESOURCE_COMPLETE);
        return this;
    }

    public ShadowAsserter<RA> assertEffectiveOperationsDeeply() {
        assertEffectiveOperationsDeeply(getObjectable(), desc());
        return this;
    }

    // See ShadowsUtil.checkReturnedShadowValidityDeeply method
    private static void assertEffectiveOperationsDeeply(@NotNull ShadowType shadow, String context) {
        assertThat(shadow.getEffectiveOperationPolicy())
                .as("effective operations in " + context)
                .isNotNull();
        for (var refAttr : ShadowUtil.getReferenceAttributes(shadow)) {
            for (var refAttrVal : refAttr.getReferenceValues()) {
                assertEffectiveOperationsDeeply(
                        refAttrVal.getShadowBean(),
                        refAttr.getElementName() + " value " + refAttrVal + " in " + context);
            }
        }
        for (var assoc : ShadowUtil.getAssociations(shadow)) {
            for (var assocVal : assoc.getAssociationValues()) {
                var assocContext = assoc.getElementName() + " value " + assocVal + " in " + context;
                for (var ref : assocVal.getObjectReferences()) {
                    for (var refAttrVal : ref.getReferenceValues()) {
                        assertEffectiveOperationsDeeply(
                                refAttrVal.getShadowBean(),
                                ref.getElementName() + " value " + refAttrVal + " in " + assocContext);
                    }
                }
                if (assoc.getDefinitionRequired().isComplex()) {
                    assertEffectiveOperationsDeeply(
                            assocVal.getAssociationDataObject().getBean(),
                            "object in " + assocContext);
                }
            }
        }
    }

    public ShadowAsserter<RA> assertProtected() {
        assertThat(getObjectable().isProtectedObject())
                .as("protected flag")
                .isTrue();
        return this;
    }

    public ShadowAsserter<RA> assertNotProtected() {
        assertThat(getObjectable().isProtectedObject())
                .as("protected flag")
                .isNotEqualTo(Boolean.TRUE);
        return this;
    }
}
