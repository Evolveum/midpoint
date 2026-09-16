/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.getCaseObjectRef;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;

/**
 * Tests pending-object references and preview context resolved for Cases and Work-item object columns.
 *
 * For an ADD that is pending approval or has been rejected, the column uses
 * the object stored in the approval delta so its name can still be displayed,
 * and supplies case-based navigation context. Approved ADDs and ordinary cases
 * keep the normal displayed reference and ordinary navigation.
 */
public class ColumnUtilsCaseObjectColumnTest extends AbstractGuiUnitTest {

    private static final String DELTA_ROLE_OID = "delta-role-oid";
    private static final String CASE_OBJECT_OID = "case-object-oid";
    private static final String CASE_OID = "case-oid";
    private static final String NON_APPROVAL_OUTCOME = "http://example.com/test-outcome";

    @Test
    public void testPendingAddUsesDeltaObjectAndSuppliesPreviewContext() throws Exception {
        CaseType caseType = createCaseWithAddDelta(null);

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertEquals(caseObjectRef.sourceCaseOid(), CASE_OID);

        PrismObject<RoleType> pendingObject =
                WebComponentUtil.getPendingObjectFromAddCase(caseType, RoleType.class, DELTA_ROLE_OID);

        assertNotNull(pendingObject);
        assertEquals(pendingObject.getOid(), DELTA_ROLE_OID);
    }

    @Test
    public void testRejectedAddUsesDeltaObjectAndSuppliesPreviewContext() throws Exception {
        CaseType caseType = createCaseWithAddDelta(ApprovalUtils.toUri(WorkItemOutcomeType.REJECT));

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertEquals(caseObjectRef.sourceCaseOid(), CASE_OID);
    }

    @Test
    public void testNonApprovalOutcomeAddUsesDeltaObjectAndSuppliesPreviewContext() throws Exception {
        CaseType caseType = createCaseWithAddDelta(NON_APPROVAL_OUTCOME);

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertFalse(ApprovalUtils.isExplicitlyApprovedOutcome(caseType.getOutcome()));
        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertEquals(caseObjectRef.sourceCaseOid(), CASE_OID);
    }

    @Test
    public void testApprovedAddUsesCaseObjectRefWithoutPreviewContext() throws Exception {
        CaseType caseType = createCaseWithAddDelta(ApprovalUtils.toUri(WorkItemOutcomeType.APPROVE));

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);

        assertMatchesCaseObjectRef(caseObjectRef.ref(), caseType);
        assertNull(caseObjectRef.sourceCaseOid());
    }

    @Test
    public void testOrdinaryCaseUsesCaseObjectRefWithoutPreviewContext() {
        CaseType caseType = new CaseType()
                .oid(CASE_OID)
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE));

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);

        assertMatchesCaseObjectRef(caseObjectRef.ref(), caseType);
        assertNull(caseObjectRef.sourceCaseOid());
    }

    @Test
    public void testCaseWithoutOidDoesNotSupplyPreviewContext() {
        CaseType caseType = new CaseType()
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE));

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);

        assertMatchesCaseObjectRef(caseObjectRef.ref(), caseType);
        assertNull(caseObjectRef.sourceCaseOid());
    }

    @Test
    public void testRootOperationRequestReferenceOnlyDoesNotSupplyPreviewContext() {
        ObjectReferenceType objectRef = new ObjectReferenceType()
                .oid(DELTA_ROLE_OID)
                .type(RoleType.COMPLEX_TYPE);
        CaseType caseType = createOperationRequestCase(objectRef);

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertNull(caseObjectRef.sourceCaseOid());
    }

    @Test
    public void testCaseObjectRefWithoutOidDoesNotSupplyPreviewContext() {
        CaseType caseType = new CaseType()
                .oid(CASE_OID)
                .objectRef(new ObjectReferenceType()
                        .type(RoleType.COMPLEX_TYPE));

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertNull(ref.getOid());
        assertNull(caseObjectRef.sourceCaseOid());
    }

    @Test
    public void testRootOperationRequestWithEmbeddedObjectSuppliesPreviewContext() {
        RoleType role = new RoleType(getPrismContext())
                .oid(DELTA_ROLE_OID)
                .name("Embedded role");

        ObjectReferenceType objectRef = createReferenceWithEmbeddedObject(role);
        CaseType caseType = createOperationRequestCase(objectRef);

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertEquals(caseObjectRef.sourceCaseOid(), CASE_OID);
    }

    @Test
    public void testRootOperationRequestWithEmbeddedObjectWithoutOidSuppliesPreviewContext() {
        RoleType role = new RoleType(getPrismContext())
                .name("Embedded role");

        ObjectReferenceType objectRef = createReferenceWithEmbeddedObject(role);
        CaseType caseType = createOperationRequestCase(objectRef);

        ColumnUtils.CaseObjectRef caseObjectRef = getCaseObjectRef(caseType);
        ObjectReferenceType ref = caseObjectRef.ref();

        assertNull(ref.getOid());
        assertEquals(caseObjectRef.sourceCaseOid(), CASE_OID);
    }

    @Test
    public void testRootOperationRequestEmbeddedObjectIsExtracted() {
        RoleType role = new RoleType(getPrismContext())
                .oid(DELTA_ROLE_OID)
                .name("Embedded role");

        ObjectReferenceType objectRef = createReferenceWithEmbeddedObject(role);
        CaseType caseType = createOperationRequestCase(objectRef);

        PrismObject<RoleType> pendingObject =
                WebComponentUtil.getPendingObjectFromAddCase(
                        caseType, RoleType.class, DELTA_ROLE_OID);

        assertNotNull(pendingObject);
        assertEquals(pendingObject.getOid(), DELTA_ROLE_OID);
    }

    @Test
    public void testPendingObjectExtractionRejectsTypeMismatch() throws Exception {
        CaseType caseType = createCaseWithAddDelta(null);

        assertNull(WebComponentUtil.getPendingObjectFromAddCase(caseType, UserType.class, DELTA_ROLE_OID));
    }

    @Test
    public void testPreviewParametersContainExpectedContext() {
        ObjectReferenceType objectRef = new ObjectReferenceType()
                .oid(DELTA_ROLE_OID)
                .type(RoleType.COMPLEX_TYPE);

        Url encoded = new OnePageParameterEncoder()
                .encodePageParameters(DetailsPageUtil.createPendingObjectPreviewParameters(objectRef, CASE_OID));

        assertEquals(encoded.getSegments().size(), 1);
        assertEquals(encoded.getSegments().get(0), DELTA_ROLE_OID);
        assertTrue(encoded.getQueryParameters().stream()
                .anyMatch(parameter -> DetailsPageUtil.PARAM_PENDING_OBJECT_PREVIEW.equals(parameter.getName())
                        && "true".equals(parameter.getValue())));
        assertTrue(encoded.getQueryParameters().stream()
                .anyMatch(parameter -> DetailsPageUtil.PARAM_PENDING_OBJECT_CASE_OID.equals(parameter.getName())
                        && CASE_OID.equals(parameter.getValue())));
        assertTrue(encoded.getQueryParameters().stream()
                .anyMatch(parameter -> DetailsPageUtil.PARAM_PENDING_OBJECT_TYPE.equals(parameter.getName())
                        && RoleType.COMPLEX_TYPE.getLocalPart().equals(parameter.getValue())));
    }

    @Test
    public void testWorkItemObjectColumnDoesNotSupplyPreviewContextForOrdinaryCase() {
        CaseType caseType = new CaseType()
                .oid(CASE_OID)
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE));
        CaseWorkItemType workItem = new CaseWorkItemType();
        caseType.getWorkItem().add(workItem);

        ObjectReferenceColumn<PrismContainerValueWrapper<CaseWorkItemType>> column = getWorkItemObjectColumn();
        IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = createWorkItemRowModel(workItem);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertEquals(ref.getOid(), CASE_OBJECT_OID);
        assertNull(column.getPendingObjectPreviewCaseOid(ref, rowModel));
    }

    @Test
    public void testWorkItemObjectColumnSuppliesPreviewContextForPendingAdd() throws Exception {
        CaseType caseType = createCaseWithAddDelta(null);
        CaseWorkItemType workItem = new CaseWorkItemType();
        caseType.getWorkItem().add(workItem);

        ObjectReferenceColumn<PrismContainerValueWrapper<CaseWorkItemType>> column = getWorkItemObjectColumn();
        IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = createWorkItemRowModel(workItem);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertEquals(column.getPendingObjectPreviewCaseOid(ref, rowModel), CASE_OID);
    }

    @SuppressWarnings("unchecked")
    private ObjectReferenceColumn<PrismContainerValueWrapper<CaseWorkItemType>> getWorkItemObjectColumn() {
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns =
                ColumnUtils.getDefaultWorkItemColumns(null, true, false);
        return (ObjectReferenceColumn<PrismContainerValueWrapper<CaseWorkItemType>>)
                columns.get(2);
    }

    private IModel<PrismContainerValueWrapper<CaseWorkItemType>> createWorkItemRowModel(
            CaseWorkItemType workItem) {
        return Model.of(
                new PrismContainerValueWrapperImpl<>(
                        null,
                        workItem.asPrismContainerValue(),
                        ValueStatus.NOT_CHANGED));
    }

    private <T> ObjectReferenceType getSingleRef(
            ObjectReferenceColumn<T> column,
            IModel<T> rowModel) {
        List<ObjectReferenceType> refs = column.extractDataModel(rowModel).getObject();
        assertEquals(refs.size(), 1);
        return refs.get(0);
    }

    private void assertMatchesCaseObjectRef(ObjectReferenceType ref, CaseType caseType) {
        assertEquals(ref.getOid(), caseType.getObjectRef().getOid());
        assertEquals(ref.getType(), caseType.getObjectRef().getType());
    }

    private CaseType createCaseWithAddDelta(String outcome) throws SchemaException {
        RoleType role = new RoleType(getPrismContext())
                .oid(DELTA_ROLE_OID)
                .name("Delta role");

        return new CaseType()
                .oid(CASE_OID)
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE))
                .approvalContext(new ApprovalContextType()
                        .deltasToApprove(ObjectTreeDeltas.toObjectTreeDeltasType(
                                new ObjectTreeDeltas<>(DeltaFactory.Object.createAddDelta(role.asPrismObject())))))
                .outcome(outcome);
    }

    private CaseType createOperationRequestCase(ObjectReferenceType objectRef) {
        CaseType caseType = new CaseType()
                .oid(CASE_OID)
                .objectRef(objectRef);

        caseType.getArchetypeRef().add(new ObjectReferenceType()
                .oid(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value()));

        return caseType;
    }

    private ObjectReferenceType createReferenceWithEmbeddedObject(RoleType role) {
        ObjectReferenceType objectRef = new ObjectReferenceType()
                .oid(role.getOid())
                .type(RoleType.COMPLEX_TYPE);
        objectRef.asReferenceValue().setObject(role.asPrismObject());

        return objectRef;
    }
}
