/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnforcementPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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

        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        computeConflicts();
    }

    private void computeConflicts() {
        MidPointApplication mp = MidPointApplication.get();
        PageBase page = getPageBase();

        Task task = page.createSimpleTask("computeConflicts");
        OperationResult result = task.getResult();

        RequestAccess requestAccess = getModelObject();

        int warnings = 0;
        int errors = 0;
        Map<String, ConflictDto> conflictsMap = new HashMap<>();
        try {
            PrismObject<UserType> user = WebModelServiceUtils.loadObject(requestAccess.getPersonOfInterest().get(0), page);
            ObjectDelta<UserType> delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);

            handleAssignmentDeltas(delta, requestAccess.getShoppingCartAssignments(), def);

            PartialProcessingOptionsType processing = new PartialProcessingOptionsType();
            processing.setInbound(SKIP);
            processing.setProjection(SKIP);

            ModelExecuteOptions options = ModelExecuteOptions.create().partialProcessing(processing);

            ModelContext<UserType> ctx = mp.getModelInteractionService()
                    .previewChanges(MiscUtil.createCollection(delta), options, task, result);

            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ctx.getEvaluatedAssignmentTriple();

            if (evaluatedAssignmentTriple != null) {
                Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple.getPlusSet();
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                        if (!policyRule.containsEnabledAction()) {
                            continue;
                        }
                        // everything other than 'enforce' is a warning
                        boolean isWarning = !policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
                        if (isWarning) {
                            warnings++;
                        } else {
                            errors++;
                        }
//                        fillInConflictedObjects(evaluatedAssignment, policyRule.getAllTriggers(), isWarning, conflictsMap);
                    }
                }
            } else if (!result.isSuccess() && StringUtils.isNotEmpty(getSubresultWarningMessages(result))) {
                warn(createStringResource("PageAssignmentsList.conflictsWarning").getString() + " " + getSubresultWarningMessages(result));
//                conflictProblemExists = true;
            }
        } catch (Exception e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
//            error("Couldn't get assignments conflicts. Reason: " + e);
            // todo error handling
            e.printStackTrace();
        }
        requestAccess.setWarningCount(warnings);
        requestAccess.setErrorCount(errors);
//        return new ArrayList<>(conflictsMap.values());
    }

//    private void fillInFromEvaluatedExclusionTrigger(EvaluatedAssignment<UserType> evaluatedAssignment, EvaluatedExclusionTrigger exclusionTrigger, boolean isWarning, Map<String, ConflictDto> conflictsMap) {
//        EvaluatedAssignment<F> conflictingAssignment = exclusionTrigger.getConflictingAssignment();
//        PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>) evaluatedAssignment.getTarget();
//        PrismObject<F> exclusionTargetObj = (PrismObject<F>) conflictingAssignment.getTarget();
//
//        AssignmentConflictDto<F> dto1 = new AssignmentConflictDto<>(exclusionTargetObj,
//                conflictingAssignment.getAssignment(true) != null);
//        AssignmentConflictDto<F> dto2 = new AssignmentConflictDto<>(addedAssignmentTargetObj,
//                evaluatedAssignment.getAssignment(true) != null);
//        ConflictDto conflict = new ConflictDto(dto1, dto2, isWarning);
//        String oid1 = exclusionTargetObj.getOid();
//        String oid2 = addedAssignmentTargetObj.getOid();
//        if (!conflictsMap.containsKey(oid1 + oid2) && !conflictsMap.containsKey(oid2 + oid1)) {
//            conflictsMap.put(oid1 + oid2, conflict);
//        } else if (!isWarning) {
//            // error is stronger than warning, so we replace (potential) warnings with this error
//            // TODO Kate please review this
//            if (conflictsMap.containsKey(oid1 + oid2)) {
//                conflictsMap.replace(oid1 + oid2, conflict);
//            }
//            if (conflictsMap.containsKey(oid2 + oid1)) {
//                conflictsMap.replace(oid2 + oid1, conflict);
//            }
//        }
//    }
//
//    private void fillInConflictedObjects(EvaluatedAssignment<UserType> evaluatedAssignment, Collection<EvaluatedPolicyRuleTrigger<?>> triggers, boolean isWarning, Map<String, ConflictDto> conflictsMap) {
//
//        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
//
//            if (trigger instanceof EvaluatedExclusionTrigger) {
//                fillInFromEvaluatedExclusionTrigger(evaluatedAssignment, (EvaluatedExclusionTrigger) trigger, isWarning, conflictsMap);
//            } else if (trigger instanceof EvaluatedCompositeTrigger) {
//                EvaluatedCompositeTrigger compositeTrigger = (EvaluatedCompositeTrigger) trigger;
//                Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers = compositeTrigger.getInnerTriggers();
//                fillInConflictedObjects(evaluatedAssignment, innerTriggers, isWarning, conflictsMap);
//            }
//        }
//
//    }

    private String getSubresultWarningMessages(OperationResult result) {
        if (result == null || result.getSubresults() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        result.getSubresults().forEach(subresult -> {
            if (subresult.isWarning()) {
                sb.append(subresult.getMessage());
                sb.append("\n");
            }
        });
        return sb.toString();
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
            List<AssignmentType> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta delta = getPrismContext().deltaFactory().container().create(ItemPath.EMPTY_PATH, def.getItemName(), def);

        for (AssignmentType a : assignments) {
            PrismContainerValue newValue = a.asPrismContainerValue();
            newValue.applyDefinition(def, false);
            delta.addValueToAdd(newValue.clone());
        }

        if (!delta.isEmpty()) {
            delta = focusDelta.addModification(delta);
        }

        return delta;
    }

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
