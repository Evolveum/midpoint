/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.HelpTab;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import java.util.List;

/**
 * @author lskublik
 */
public abstract class WaitingScriptConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String OP_IS_GENERATION_RUNNING = WaitingScriptConnectorStepPanel.class.getName() + ".isGenerationRunning";

    private String repairScript;
    private List<String> repairErrors = List.of();

    public WaitingScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    public void resetScript(PageBase pageBase) {
        resetScript(pageBase, null, List.of());
    }

    public void resetScript(PageBase pageBase, String currentScript, List<String> errorMessages) {
        repairScript = currentScript;
        repairErrors = errorMessages != null ? errorMessages : List.of();
        followingScriptPanel().ifPresent(ScriptConnectorStepPanel::dropGeneratedScript);
        restartTask();
    }

    /** The editor step of the same operation - the first script step after this waiting step (fix steps may sit in between). */
    public Optional<ScriptConnectorStepPanel> followingScriptPanel() {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return Optional.empty();
        }
        List<WizardStep> steps = wizardModel.getActiveChildrenSteps();
        int index = steps.indexOf(this);
        if (index < 0) {
            return Optional.empty();
        }
        for (int i = index + 1; i < steps.size(); i++) {
            if (steps.get(i) instanceof ScriptConnectorStepPanel scriptPanel) {
                return Optional.of(scriptPanel);
            }
        }
        return Optional.empty();
    }

    /** Whether a generation task for this operation is currently running (not finished, failed or suspended). */
    public boolean isGenerationRunning() {
        try {
            String token = ConnectorDevelopmentWizardUtil.getTaskToken(
                    getActivityType(),
                    getObjectClassName(),
                    getScriptType(),
                    getDetailsModel().getObjectWrapper().getOid(),
                    getDetailsModel().getPageAssignmentHolder());
            if (token == null) {
                return false;
            }
            Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_IS_GENERATION_RUNNING);
            StatusInfo<ConnDevGenerateArtifactResultType> status = getDetailsModel().getServiceLocator()
                    .getConnectorService().getGenerateArtifactStatus(token, task, task.getResult());
            return status != null && !status.isComplete() && !status.isHalted() && !status.isSuspended();
        } catch (CommonException e) {
            return false;
        }
    }

    protected String getRepairScript() {
        return repairScript;
    }

    protected List<String> getRepairErrors() {
        return repairErrors;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        String token = getFinishedTaskToken();
        if (token != null) {
            followingScriptPanel().ifPresent(scriptPanel -> scriptPanel.setGeneratedScriptToken(token));
        }
        return super.onNextPerformed(target);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT;
    }

    @Override
    public abstract ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType();

    /**
     * The waiting screen shows the same documentation as the script screen it precedes.
     */
    @Override
    protected List<HelpTab> computeHelpTabs() {
        String key = ConnectorDevelopmentWizardUtil.helpTopicFor(getScriptType());
        return key == null
                ? List.of()
                : ConnectorDevelopmentWizardUtil.helpTabs(getDetailsModel(), key, null);
    }

    @Override
    public boolean isCompleted() {
        PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue =
                ConnectorDevelopmentWizardUtil.getObjectClassValueWrapper(getDetailsModel(), getObjectClassName());
        if (!isReloaded() && ConnectorDevelopmentWizardUtil.existContainerValue(objectClassValue, ConnDevObjectClassInfoType.F_ATTRIBUTE)
                && ConnectorDevelopmentWizardUtil.existContainerValue(objectClassValue, getScriptType().itemName)) {
            return true;
        }

        return super.isCompleted();
    }
}
