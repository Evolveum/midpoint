/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
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
public class ShoppingCartPanel extends WizardStepPanel<RequestAccess> implements AccessRequestMixin {

    @Serial private static final long serialVersionUID = 1L;
    public static final String STEP_ID = "cart";

    private enum State {
        SUMMARY, CONFLICTS
    }

    private static final String ID_CONFLICT_SOLVER = "conflictSolver";
    private static final String ID_CART_SUMMARY = "cartSummary";
    public static final String ID_PANEL_STATUS = "panelStatus";

    private final PageBase page;

    private final IModel<State> state = Model.of(State.SUMMARY);

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
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        String statusId = get(ID_PANEL_STATUS).getMarkupId();
        String message = state.getObject() == State.CONFLICTS ?
                getString("ShoppingCartPanel.conflictSolver.ariaLabel") : "";
        response.render(OnDomReadyHeaderItem.forScript(
                String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d)", statusId, message, 250)));
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        getModelObject().computeConflicts(page);
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return () -> {
            RequestAccess data = getModelObject();
            data.computeConflicts(page);

            List<Badge> badges = new ArrayList<>();

            long warnings = data.getWarningCount();
            if (warnings > 0) {
                String key = warnings == 1 ? "ShoppingCartPanel.badge.oneWarning" : "ShoppingCartPanel.badge.multipleWarnings";
                badges.add(new Badge("badge badge-warning", getString(key, warnings)));
            }

            long errors = data.getErrorCount();
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
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    private void initLayout() {
        Label panelStatusLabel = new Label(ID_PANEL_STATUS);
        panelStatusLabel.setOutputMarkupId(true);
        add(panelStatusLabel);

        CartSummaryPanel cartSummary = new CartSummaryPanel(ID_CART_SUMMARY, (WizardModelBasic) getWizard(), getModel(), page) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void openConflictPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.openConflictPerformed(target);
                target.appendJavaScript(String.format("MidPointTheme.saveFocus('%s');", panelStatusLabel.getPageRelativePath()));
                target.appendJavaScript("MidPointTheme.restoreFocus();");
            }

            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.submitPerformed(target);
            }
        };
        cartSummary.add(new VisibleBehaviour(() -> state.getObject() == State.SUMMARY));
        add(cartSummary);

        ConflictSolverPanel conflictSolver = new ConflictSolverPanel(ID_CONFLICT_SOLVER, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void backToSummaryPerformed(AjaxRequestTarget target) {
                ShoppingCartPanel.this.onBackPerformed(target);
            }

            @Override
            protected void solveConflictPerformed(AjaxRequestTarget target, IModel<Conflict> conflictModel, IModel<ConflictItem> itemToKeepModel) {
                super.solveConflictPerformed(target, conflictModel, itemToKeepModel);

                target.add(((WizardModelBasic)getWizard()).getHeader());
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
        String caseOid = result.findCaseOid();
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
