/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartPanel extends WizardStepPanel<RequestAccess> implements AccessRequestStep {

    private static final long serialVersionUID = 1L;
    public static final String STEP_ID = "cart";

    private enum State {
        SUMMARY, CONFLICTS;
    }

    private static final String ID_CONFLICT_SOLVER = "conflictSolver";
    private static final String ID_CART_SUMMARY = "cartSummary";

    private PageBase page;

    private IModel<State> state = Model.of(State.SUMMARY);

    public ShoppingCartPanel(IModel<RequestAccess> model, PageBase page) {
        super(model);

        this.page = page;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        getModelObject().computeConflicts(page);
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return () -> {
            getModelObject().computeConflicts(page);

            List<Badge> badges = new ArrayList<>();

            long warnings = getModelObject().getWarningCount();
            if (warnings > 0) {
                String key = warnings == 1 ? "ShoppingCartPanel.badge.oneWarning" : "ShoppingCartPanel.badge.multipleWarnings";
                badges.add(new Badge("badge badge-warning", getString(key, warnings)));
            }

            long errors = getModelObject().getErrorCount();
            if (errors > 0) {
                String key = errors == 1 ? "ShoppingCartPanel.badge.oneConflict" : "ShoppingCartPanel.badge.multipleConflicts";
                badges.add(new Badge("badge badge-danger", "fa fa-exclamation-triangle", getString(key, errors)));
            }

            return badges;
        };
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    @Override
    public IModel<String> getTitle() {
        return () -> state.getObject() == State.SUMMARY ? page.getString("ShoppingCartPanel.title") : page.getString("ShoppingCartPanel.conflict");
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    private void initLayout() {
        CartSummaryPanel cartSummary = new CartSummaryPanel(ID_CART_SUMMARY, getWizard(), getModel(), page) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void openConflictPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.openConflictPerformed(target);
            }

            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.submitPerformed(target);
            }
        };
        cartSummary.add(new VisibleBehaviour(() -> state.getObject() == State.SUMMARY));
        add(cartSummary);

        ConflictSolverPanel conflictSolver = new ConflictSolverPanel(ID_CONFLICT_SOLVER, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void backToSummaryPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.onBackPerformed(target);
            }
        };
        conflictSolver.add(new VisibleBehaviour(() -> state.getObject() == State.CONFLICTS));
        add(conflictSolver);
    }

    protected void openConflictPerformed(AjaxRequestTarget target) {
        state.setObject(State.CONFLICTS);

        target.add(getWizard().getPanel());
    }

    protected void submitPerformed(AjaxRequestTarget target) {
        RequestAccess requestAccess = getModelObject();
        OperationResult result = requestAccess.submitRequest(page);

        if (result == null) {
            page.warn(getString("ShoppingCartPanel.nothingToSubmit"));
            target.add(page.getFeedbackPanel());
            return;
        }

        page.showResult(result);

        if (hasBackgroundTaskOperation(result)) {
            result.setMessage(getString("ShoppingCartPanel.requestInProgress"));
            requestAccess.clearCart();

            setResponsePage(page.getMidpointApplication().getHomePage());
            return;
        }

        if (WebComponentUtil.isSuccessOrHandledError(result)
                || OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
            result.setMessage(getString("ShoppingCartPanel.requestSuccess"));
            requestAccess.clearCart();

            setResponsePage(page.getMidpointApplication().getHomePage());
        } else {
            result.setMessage(getString("ShoppingCartPanel.requestError"));

            target.add(page.getFeedbackPanel());
            target.add(getWizard().getPanel());
        }
    }

    private boolean hasBackgroundTaskOperation(OperationResult result) {
        String caseOid = OperationResult.referenceToCaseOid(result.findAsynchronousOperationReference());
        return StringUtils.isNotEmpty(caseOid);
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        switch (state.getObject()) {
            case CONFLICTS:
                state.setObject(State.SUMMARY);
                target.add(getWizard().getPanel());
                return false;
            case SUMMARY:
            default:
                return true;
        }
    }

    @Override
    public VisibleEnableBehaviour getStepsBehaviour() {
        return new VisibleBehaviour(() -> state.getObject() == State.SUMMARY);
    }
}
