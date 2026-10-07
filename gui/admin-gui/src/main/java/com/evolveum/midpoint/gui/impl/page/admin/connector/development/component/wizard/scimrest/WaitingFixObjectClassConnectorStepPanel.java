/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevFixObjectClassResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/**
 * Waiting step for an object-class-wide fix: submits {@code submitFixObjectClass}, polls it via
 * {@code getFixObjectClassStatus}, and on completion hands the returned scripts to the sibling
 * {@link FixObjectClassReviewConnectorStepPanel} for review/validation before anything is saved
 * (see {@link #onNextPerformed}) - an empty result (nothing needed fixing) has nothing to review,
 * so the wizard just continues as before.
 * <p>
 * The fix is always object-class-wide (schema, search, create, update, delete fixed together),
 * regardless of which script step triggered it. The {@code branchPanelType} constructor argument
 * is what keeps each operation branch's own instance distinguishable, combined with the object
 * class name to form {@link #getStepId()}.
 * <p>
 * Stays invisible until explicitly triggered by {@link RepairObjectClassButton} (see
 * {@link #triggered}) - no prior fix task is the normal, permanent state for an object class that
 * was never repaired, not a "not done yet" one.
 */
public class WaitingFixObjectClassConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String CLASS_DOT = WaitingFixObjectClassConnectorStepPanel.class.getName() + ".";
    private static final String OP_APPLY_FIX = CLASS_DOT + "applyFixObjectClassResult";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel;
    private final String branchPanelType;

    private List<String> midpointErrors = List.of();
    private List<ConnDevArtifactType> currentScripts = List.of();
    private boolean triggered = false;

    public WaitingFixObjectClassConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel,
            String branchPanelType) {
        super(helper);
        this.objectClassModel = objectClassModel;
        this.branchPanelType = branchPanelType;
    }

    public IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> getObjectClassModel() {
        return objectClassModel;
    }

    @Override
    public String getStepId() {
        return "cdw-connector-waiting-fix-" + branchPanelType + "-" + getObjectClassName();
    }

    @Override
    protected String getObjectClassName() {
        return getObjectClassModel().getObject().getRealValue().getName();
    }

    /**
     * Starts (or restarts) the fix task with the given midPoint errors and, optionally, script
     * content to use in place of what is stored in the session (e.g. content the user was editing
     * that failed validation and so was never saved). Navigation is the caller's job.
     */
    public void resetFix(PageBase pageBase, List<String> midpointErrors, List<ConnDevArtifactType> currentScripts) {
        this.midpointErrors = midpointErrors != null ? midpointErrors : List.of();
        this.currentScripts = currentScripts != null ? currentScripts : List.of();
        triggered = true;
        restartTask();
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_FIX_OBJECT_CLASS;
    }

    @Override
    protected boolean objectClassRequired() {
        return true;
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation()
                .submitFixObjectClass(getObjectClassName(), midpointErrors, currentScripts, regenerate, task, result);
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws CommonException {
        return getDetailsModel().getServiceLocator().getConnectorService().getFixObjectClassStatus(token, task, result);
    }

    /**
     * Hidden until {@link #resetFix} has actually been called once - a fresh object class with no
     * fix task yet is the normal state, not an unfinished background job.
     */
    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> triggered && super.isStepVisible().getObject();
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingFixObjectClass");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingFixObjectClass.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingFixObjectClass.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-wrench");
    }

    /**
     * On a fix with at least one regenerated script, hands the batch to the sibling {@link
     * FixObjectClassReviewConnectorStepPanel} (wired directly after this one in every branch's
     * {@code createChildrenSteps()}) instead of saving directly - that step validates the whole
     * batch together and saves it only once it validates clean. An empty result (nothing needed
     * fixing) has nothing to review, so the wizard just continues as before.
     */
    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        Object rawResult = getResult();
        if (!(rawResult instanceof ConnDevFixObjectClassResultType fixResult)) {
            getPageBase().error(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.error", "").getString());
            target.add(getFeedback());
            return false;
        }
        triggered = false;
        if (fixResult.getArtifact().isEmpty()) {
            getPageBase().info(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.noChange").getString());
            return super.onNextPerformed(target);
        }
        if (!(getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            getPageBase().error(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.error", "").getString());
            target.add(getFeedback());
            return false;
        }
        Optional<FixObjectClassReviewConnectorStepPanel> reviewStep = findReviewStep(parentWizardModel);
        if (reviewStep.isEmpty()) {
            getPageBase().error(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.error", "").getString());
            target.add(getFeedback());
            return false;
        }
        reviewStep.get().resetArtifacts(fixResult.getArtifact());
        parentWizardModel.setActiveStepWithinActivePart(reviewStep.get().getStepId());
        parentWizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
        return false;
    }

    /** The {@link FixObjectClassReviewConnectorStepPanel} wired directly after this instance in the current branch. */
    private Optional<FixObjectClassReviewConnectorStepPanel> findReviewStep(WizardModelWithParentSteps parentWizardModel) {
        List<WizardStep> steps = parentWizardModel.getActiveChildrenSteps();
        int myIndex = steps.indexOf(this);
        if (myIndex >= 0 && myIndex + 1 < steps.size()
                && steps.get(myIndex + 1) instanceof FixObjectClassReviewConnectorStepPanel reviewStep) {
            return Optional.of(reviewStep);
        }
        return Optional.empty();
    }
}
