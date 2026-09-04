/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.io.IOException;
import java.util.List;

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
 * {@code getFixObjectClassStatus}, and on completion saves every returned script before letting the
 * wizard continue forward.
 * <p>
 * The fix operation is always object-class-wide (schema, search, create, update, delete fixed
 * together in one pass) regardless of which script step triggered it. One instance of this same
 * class is placed in every object-class operation branch's {@code createChildrenSteps()}, right
 * before that branch's own script-review step, so completing the fix naturally lands the wizard back
 * on whichever step the user was on. The {@code branchPanelType} constructor argument - not a
 * per-branch subclass - is what keeps each branch's instance distinguishable: it's the {@code
 * PANEL_TYPE} of the sibling {@code *ObjectClassConnectorStepPanel} this instance is fixing scripts
 * for (e.g. {@link com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllObjectClassConnectorStepPanel#PANEL_TYPE}),
 * combined with the object class name to form {@link #getStepId()}. No {@code @PanelType}/{@code
 * @PanelInstance} is needed here - those feed a separate GUI subsystem (object-details-page tab
 * configuration) that wizard steps don't participate in.
 * <p>
 * Unlike the generation-waiting steps this is modeled after, it must stay invisible until explicitly
 * triggered by {@link RepairObjectClassButton} (see {@link #triggered}) - there being no prior fix
 * task is the normal, permanent state for an object class that was never repaired, not a "not done
 * yet" one.
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
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
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

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        Object rawResult = getResult();
        if (rawResult instanceof ConnDevFixObjectClassResultType fixResult) {
            Task task = getPageBase().createSimpleTask(OP_APPLY_FIX);
            OperationResult result = task.getResult();
            try {
                for (ConnDevArtifactType artifact : fixResult.getArtifact()) {
                    getDetailsModel().getConnectorDevelopmentOperation().saveArtifact(artifact, task, result);
                }
                if (getWizard() instanceof WizardModelWithParentSteps parentWizardModel) {
                    parentWizardModel.removeOperationResultsForFixSteps(RepairObjectClassButton.OBJECT_CLASS_SCRIPT_STEP_IDS);
                    for (WizardStep step : parentWizardModel.getActiveChildrenSteps()) {
                        if (step instanceof ScriptsConnectorStepPanel scriptsStep) {
                            scriptsStep.detachLoadedScripts();
                        } else if (step instanceof ScriptConnectorStepPanel scriptStep) {
                            scriptStep.detachLoadedScript();
                        }
                    }
                }
                if (!fixResult.getChangedOperation().isEmpty()) {
                    getPageBase().success(createStringResource(
                            "NextStepsConnectorStepPanel.repairObjectClass.success",
                            fixResult.getChangedOperation().size()).getString());
                } else {
                    getPageBase().info(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.noChange").getString());
                }
            } catch (IOException | CommonException e) {
                getPageBase().error(createStringResource(
                        "NextStepsConnectorStepPanel.repairObjectClass.error", e.getMessage()).getString());
                target.add(getFeedback());
                return false;
            }
        } else {
            getPageBase().error(createStringResource("NextStepsConnectorStepPanel.repairObjectClass.error", "").getString());
            target.add(getFeedback());
            return false;
        }
        triggered = false;
        return super.onNextPerformed(target);
    }
}
