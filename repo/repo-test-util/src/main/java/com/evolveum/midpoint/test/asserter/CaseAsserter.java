/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.PrismObject.asObjectableList;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CASE_STATE_CLOSED_QNAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CASE_STATE_CLOSING_QNAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PolyStringAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 */
public class CaseAsserter<RA> extends PrismObjectAsserter<CaseType,RA> {

    public CaseAsserter(PrismObject<CaseType> focus) {
        super(focus);
    }

    public CaseAsserter(PrismObject<CaseType> focus, String details) {
        super(focus, details);
    }

    public CaseAsserter(PrismObject<CaseType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object) {
        return new CaseAsserter<>(object);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object, String details) {
        return new CaseAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public CaseAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertNameOrig(String expectedOrig) {
        return (CaseAsserter<RA>) super.assertNameOrig(expectedOrig);
    }

    @Override
    public CaseAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public CaseAsserter<RA> display() {
        super.display();
        return this;
    }

    public CaseAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public CaseAsserter<RA> displayXml() throws SchemaException {
        super.displayXml();
        return this;
    }

    @Override
    public CaseAsserter<RA> displayXml(String message) throws SchemaException {
        super.displayXml(message);
        return this;
    }

    @Override
    public PolyStringAsserter<CaseAsserter<RA>> name() {
        //noinspection unchecked
        return (PolyStringAsserter<CaseAsserter<RA>>)super.name();
    }

    @Override
    public CaseAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        return (CaseAsserter<RA>) super.assertArchetypeRef(expectedArchetypeOid);
    }

    @Override
    public CaseAsserter<RA> assertNoArchetypeRef() {
        return (CaseAsserter<RA>) super.assertNoArchetypeRef();
    }

    public CaseAsserter<RA> assertOperationRequestArchetype() {
        return assertArchetypeRef(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
    }

    public CaseAsserter<RA> assertApprovalCaseArchetype() {
        return assertArchetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    public CaseAsserter<RA> assertClosed() {
        return assertState(CASE_STATE_CLOSED_QNAME);
    }

    public CaseAsserter<RA> assertOpen() {
        return assertState(SchemaConstants.CASE_STATE_OPEN_QNAME);
    }

    public CaseAsserter<RA> assertClosingOrClosed() {
        String stateUri = getState();
        QName stateQName = QNameUtil.uriToQName(stateUri, true);
        if (!QNameUtil.matchAny(stateQName, List.of(CASE_STATE_CLOSING_QNAME, CASE_STATE_CLOSED_QNAME))) {
            fail("State is neither closing or closed: " + stateUri);
        }
        return this;
    }

    private CaseAsserter<RA> assertState(QName expected) {
        MidPointAsserts.assertUriMatches(getState(), "state", expected);
        return this;
    }

    private String getState() {
        return getObjectable().getState();
    }

    public CaseAsserter<RA> assertApproved() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
    }

    public CaseAsserter<RA> assertRejected() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
    }

    private CaseAsserter<RA> assertOutcome(String expected) {
        MidPointAsserts.assertUriMatches(getObjectable().getOutcome(), "outcome", expected);
        return this;
    }

    /** Uses approximate comparison, see {@link #normalize(ObjectDelta)}. */
    public CaseAsserter<RA> assertDeltasToApprove(ObjectDelta<?>... expected) throws SchemaException {
        Collection<ObjectDelta<?>> real = getRealDeltasToApprove();
        assertThat(normalize(real))
                .as("deltas to approve in " + desc())
                .containsExactlyInAnyOrderElementsOf(
                        normalize(Arrays.asList(expected)));
        return this;
    }

    private List<ObjectDelta<?>> normalize(Collection<ObjectDelta<?>> deltas) {
        List<ObjectDelta<?>> normalized = new ArrayList<>();
        for (ObjectDelta<?> delta : deltas) {
            normalized.add(normalize(delta));
        }
        return normalized;
    }

    /** Delta comparison is hard. Fix this method if needed. */
    private ObjectDelta<?> normalize(ObjectDelta<?> delta) {
        if (!delta.isModify()) {
            return delta;
        }
        var deltaClone = delta.clone();
        deltaClone.getModifications().clear(); // ugly hack
        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            ItemDelta<?, ?> modificationClone = modification.clone();
            modificationClone.setEstimatedOldValues(null);
            deltaClone.addModification(modificationClone);
        }
        return deltaClone;
    }

    private Collection<ObjectDelta<?>> getRealDeltasToApprove() throws SchemaException {
        List<ObjectDelta<?>> deltas = new ArrayList<>();
        ApprovalContextType ctx = getObjectable().getApprovalContext();
        if (ctx == null) {
            return deltas;
        }
        ObjectTreeDeltasType bean = ctx.getDeltasToApprove();
        if (bean == null) {
            return deltas;
        }
        ObjectDeltaType focusDeltaBean = bean.getFocusPrimaryDelta();
        if (focusDeltaBean != null) {
            deltas.add(DeltaConvertor.createObjectDelta(focusDeltaBean));
        }
        for (ProjectionObjectDeltaType projectionContextDeltaBean : bean.getProjectionPrimaryDelta()) {
            ObjectDeltaType projectionDeltaBean = projectionContextDeltaBean.getPrimaryDelta();
            if (projectionDeltaBean != null) {
                deltas.add(DeltaConvertor.createObjectDelta(projectionDeltaBean));
            }
        }
        return deltas;
    }

    public @NotNull ApprovalContextType getApprovalContextRequired() {
        ApprovalContextType ctx = getObjectable().getApprovalContext();
        assertThat(ctx).as("approval context in " + desc()).isNotNull();
        return ctx;
    }

    public CaseAsserter<RA> assertOpenApproval(@NotNull String expectedName) {
        assertNoFetchResult();
        assertThat(getObjectable().getName().getOrig())
                .as("case name in " + desc())
                .isEqualTo(expectedName);

        ObjectReferenceType targetRef = getTargetRef();
        assertThat(getStartTimestamp())
                .as("start timestamp in " + desc())
                .isNotNull();

        assertThat(getCloseTimestamp())
                .as("close timestamp in " + desc())
                .isNull();

        assertThat(getOutcome())
                .as("outcome in " + desc())
                .isNull();

        return this;
    }

    private ObjectReferenceType getTargetRef() {
        return getObjectable().getTargetRef();
    }

    private String getOutcome() {
        return getObjectable().getOutcome();
    }

    private XMLGregorianCalendar getCloseTimestamp() {
        return getObjectable().getCloseTimestamp();
    }

    private XMLGregorianCalendar getStartTimestamp() {
        return CaseTypeUtil.getStartTimestamp(getObjectable());
    }

    public SubcasesAsserter<RA> subcases() {
        OperationResult result = new OperationResult(CaseAsserter.class.getName() + ".subcases");
        ObjectQuery query = getPrismContext().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(getOid())
                .build();
        SearchResultList<PrismObject<CaseType>> subcases;
        try {
            subcases = getRepositoryService().searchObjects(CaseType.class, query, null, result);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        SubcasesAsserter<RA> asserter =
                new SubcasesAsserter<>(this, asObjectableList(subcases), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseAsserter<RA> assertObjectRef(@NotNull ObjectReferenceType ref) {
        return assertObjectRef(ref.getOid(), ref.getType());
    }

    public CaseAsserter<RA> assertObjectRef(String oid, QName typeName) {
        return assertReference(getObjectable().getObjectRef(), "objectRef", oid, typeName);
    }

    public CaseAsserter<RA> assertTargetRef(@NotNull ObjectReferenceType ref) {
        return assertTargetRef(ref.getOid(), ref.getType());
    }

    public CaseAsserter<RA> assertTargetRef(String oid, QName typeName) {
        return assertReference(getTargetRef(), "targetRef", oid, typeName);
    }

    public CaseAsserter<RA> assertNoTargetRef() {
        assertThat(getTargetRef())
                .withFailMessage("targetRef present in case even if it shouldn't be: " + desc())
                .isNull();
        return this;
    }

    private CaseAsserter<RA> assertReference(ObjectReferenceType ref, String desc, String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(ref, desc, oid, typeName);
        return this;
    }

    public CaseAsserter<RA> assertStageNumber(int expected) {
        assertThat(getObjectable().getStageNumber())
                .as("stage number")
                .isEqualTo(expected);
        return this;
    }

    public CaseEventsAsserter<RA> events() {
        CaseEventsAsserter<RA> asserter = new CaseEventsAsserter<>(this, getObjectable().getEvent(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseWorkItemsAsserter<RA> workItems() {
        CaseWorkItemsAsserter<RA> asserter = new CaseWorkItemsAsserter<>(this, getObjectable().getWorkItem(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public TriggersAsserter<CaseType, ? extends CaseAsserter<RA>, RA> triggers() {
        TriggersAsserter<CaseType, ? extends CaseAsserter<RA>, RA> asserter =
                new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
