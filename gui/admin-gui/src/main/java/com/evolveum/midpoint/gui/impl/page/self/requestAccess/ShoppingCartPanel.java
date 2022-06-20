/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartPanel extends WizardStepPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;
    public static final String STEP_ID = "shoppingCart";

    private enum State {
        SUMMARY, CONFLICTS;
    }

    private static final String ID_CONFLICT_SOLVER = "conflictSolver";
    private static final String ID_CART_SUMMARY = "cartSummary";

    private IModel<State> state = Model.of(State.SUMMARY);

    public ShoppingCartPanel(IModel<RequestAccess> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        getModelObject().computeConflicts(getPageBase());
    }

    // todo this doesn't work properly first time loading conflict numbers - model is evaluated before computeConflicts...
    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return () -> {
            List<Badge> badges = new ArrayList<>();

            int warnings = getModelObject().getWarningCount();
            if (warnings > 0) {
                String key = warnings == 1 ? "ShoppingCartPanel.badge.oneWarning" : "ShoppingCartPanel.badge.multipleWarnings";
                badges.add(new Badge("badge badge-warning", getString(key, warnings)));
            }

            int errors = getModelObject().getErrorCount();
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
        return () -> state.getObject() == State.SUMMARY ? getString("ShoppingCartPanel.title") : getString("ShoppingCartPanel.conflict");
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
        CartSummaryPanel cartSummary = new CartSummaryPanel(ID_CART_SUMMARY, getWizard(), getModel()) {

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

        ConflictSolverPanel conflictSolver = new ConflictSolverPanel(ID_CONFLICT_SOLVER);
        conflictSolver.add(new VisibleBehaviour(() -> state.getObject() == State.CONFLICTS));
        add(conflictSolver);
    }

    protected void openConflictPerformed(AjaxRequestTarget target) {
        state.setObject(State.CONFLICTS);

        target.add(getWizard().getPanel());
    }

    protected void submitPerformed(AjaxRequestTarget target) {
        // todo implement
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
