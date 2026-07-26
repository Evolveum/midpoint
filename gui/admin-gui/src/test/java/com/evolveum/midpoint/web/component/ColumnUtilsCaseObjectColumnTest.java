/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;

/**
 * Tests object-reference selection and link availability in the Cases object column.
 *
 * For an ADD that is pending approval or has been rejected, the column uses
 * the object stored in the approval delta so its name can still be displayed,
 * but disables navigation because the object has not been created. Approved ADDs
 * and ordinary cases retain the normal case object reference and link behavior.
 */
public class ColumnUtilsCaseObjectColumnTest extends AbstractGuiUnitTest {

    private static final String DELTA_ROLE_OID = "delta-role-oid";
    private static final String CASE_OBJECT_OID = "case-object-oid";

    @Test
    public void testDefaultObjectReferenceColumnLinkEnabled() {
        TestObjectReferenceColumn column = new TestObjectReferenceColumn();

        assertTrue(column.isLinkEnabledForTest(new ObjectReferenceType(), Model.of("row")));
    }

    @Test
    public void testPendingAddUsesDeltaObjectAndDisablesLink() throws Exception {
        CaseType caseType = createCaseWithAddDelta(null);

        ObjectReferenceColumn<SelectableBean<CaseType>> column = getCaseObjectColumn();
        IModel<SelectableBean<CaseType>> rowModel = createRowModel(caseType);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertFalse(isLinkEnabled(column, ref, rowModel));
    }

    @Test
    public void testRejectedAddUsesDeltaObjectAndDisablesLink() throws Exception {
        CaseType caseType = createCaseWithAddDelta(ApprovalUtils.toUri(WorkItemOutcomeType.REJECT));

        ObjectReferenceColumn<SelectableBean<CaseType>> column = getCaseObjectColumn();
        IModel<SelectableBean<CaseType>> rowModel = createRowModel(caseType);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertEquals(ref.getOid(), DELTA_ROLE_OID);
        assertFalse(isLinkEnabled(column, ref, rowModel));
    }

    @Test
    public void testApprovedAddUsesCaseObjectRefAndEnablesLink() throws Exception {
        CaseType caseType = createCaseWithAddDelta(ApprovalUtils.toUri(WorkItemOutcomeType.APPROVE));

        ObjectReferenceColumn<SelectableBean<CaseType>> column = getCaseObjectColumn();
        IModel<SelectableBean<CaseType>> rowModel = createRowModel(caseType);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertMatchesCaseObjectRef(ref, caseType);
        assertTrue(isLinkEnabled(column, ref, rowModel));
    }

    @Test
    public void testOrdinaryCaseUsesCaseObjectRefAndEnablesLink() throws Exception {
        CaseType caseType = new CaseType()
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE));

        ObjectReferenceColumn<SelectableBean<CaseType>> column = getCaseObjectColumn();
        IModel<SelectableBean<CaseType>> rowModel = createRowModel(caseType);
        ObjectReferenceType ref = getSingleRef(column, rowModel);

        assertMatchesCaseObjectRef(ref, caseType);
        assertTrue(isLinkEnabled(column, ref, rowModel));
    }

    @SuppressWarnings("unchecked")
    private ObjectReferenceColumn<SelectableBean<CaseType>> getCaseObjectColumn() {
        List<IColumn<SelectableBean<CaseType>, String>> columns = ColumnUtils.getDefaultCaseColumns(null, false);
        return (ObjectReferenceColumn<SelectableBean<CaseType>>) columns.get(1);
    }

    private IModel<SelectableBean<CaseType>> createRowModel(CaseType caseType) {
        return Model.of(new SelectableBeanImpl<>(Model.of(caseType)));
    }

    private ObjectReferenceType getSingleRef(
            ObjectReferenceColumn<SelectableBean<CaseType>> column,
            IModel<SelectableBean<CaseType>> rowModel) {
        List<ObjectReferenceType> refs = column.extractDataModel(rowModel).getObject();
        assertEquals(refs.size(), 1);
        return refs.get(0);
    }

    private void assertMatchesCaseObjectRef(ObjectReferenceType ref, CaseType caseType) {
        assertEquals(ref.getOid(), caseType.getObjectRef().getOid());
        assertEquals(ref.getType(), caseType.getObjectRef().getType());
    }

    private boolean isLinkEnabled(
            ObjectReferenceColumn<SelectableBean<CaseType>> column,
            ObjectReferenceType ref,
            IModel<SelectableBean<CaseType>> rowModel) throws Exception {
        Method method = column.getClass().getDeclaredMethod("isLinkEnabled", ObjectReferenceType.class, IModel.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(column, ref, rowModel);
    }

    private CaseType createCaseWithAddDelta(String outcome) throws SchemaException {
        RoleType role = new RoleType(getPrismContext())
                .oid(DELTA_ROLE_OID)
                .name("Delta role");

        return new CaseType()
                .objectRef(new ObjectReferenceType()
                        .oid(CASE_OBJECT_OID)
                        .type(RoleType.COMPLEX_TYPE))
                .approvalContext(new ApprovalContextType()
                        .deltasToApprove(ObjectTreeDeltas.toObjectTreeDeltasType(
                                new ObjectTreeDeltas<>(
                                        DeltaFactory.Object.createAddDelta(role.asPrismObject())))))
                .outcome(outcome);
    }

    private static class TestObjectReferenceColumn extends ObjectReferenceColumn<String> {

        private TestObjectReferenceColumn() {
            super(Model.of("test"), "");
        }

        @Override
        public IModel<List<ObjectReferenceType>> extractDataModel(IModel<String> rowModel) {
            return Model.ofList(Collections.<ObjectReferenceType>emptyList());
        }

        private boolean isLinkEnabledForTest(ObjectReferenceType ref, IModel<String> rowModel) {
            return isLinkEnabled(ref, rowModel);
        }
    }
}
