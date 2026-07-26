/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Tests visibility of the default "Go to object" tile after a wizard save.
 *
 * The tile is hidden only when the save result represents an operation submitted
 * for workflow approval. Other results, including unrelated in-progress operations,
 * retain the original behavior.
 */
public class EnumWizardChoicePanelTest {

    private WicketTester tester;

    @BeforeMethod
    public void before() {
        tester = new WicketTester();
    }

    @AfterMethod
    public void after() {
        tester.destroy();
    }

    @Test
    public void testAddDefaultTileForApprovalResult() {
        OperationResult approval = new OperationResult("approval");
        approval.setInProgress();
        approval.setCaseOid("case-oid");

        assertFalse(isDefaultTileAdded(approval));
    }

    @Test
    public void testAddDefaultTileForCompletedResult() {
        OperationResult success = new OperationResult("success");
        success.recordSuccess();

        assertTrue(isDefaultTileAdded(success));
    }

    @Test
    public void testAddDefaultTileForInProgressResultWithoutCaseOid() {
        OperationResult inProgress = new OperationResult("inProgress");
        inProgress.setInProgress();

        assertTrue(isDefaultTileAdded(inProgress));
    }

    @Test
    public void testAddDefaultTileForNullResult() {
        assertTrue(isDefaultTileAdded(null));
    }

    private boolean isDefaultTileAdded(OperationResult result) {
        return new TestEnumWizardChoicePanel(result).isDefaultTileAdded();
    }

    private enum TestTile implements TileEnum {
        VALUE;

        @Override
        public String getIcon() {
            return "fa fa-test";
        }
    }

    private static class TestEnumWizardChoicePanel
            extends EnumWizardChoicePanel<TestTile, AssignmentHolderDetailsModel<RoleType>> {

        private TestEnumWizardChoicePanel(OperationResult result) {
            super("test", null, TestTile.class, result);
        }

        private boolean isDefaultTileAdded() {
            return addDefaultTile();
        }

        @Override
        protected QName getObjectType() {
            return RoleType.COMPLEX_TYPE;
        }

        @Override
        protected void onTileClickPerformed(TestTile value, AjaxRequestTarget target) {
            // Not needed for default-tile visibility tests.
        }

        @Override
        protected IModel<String> getBreadcrumbLabel() {
            return Model.of("test");
        }
    }
}
