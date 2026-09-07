/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create.CreateScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete.DeleteScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.SchemaScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchFilterScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update.UpdateScriptConnectorStepPanel;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;

/**
 * "Repair object class" button: visible whenever a matching
 * {@link WaitingFixObjectClassConnectorStepPanel} sits earlier in the current wizard branch
 * (i.e. the object class's script-review step has been reached), regardless of whether a midPoint
 * error is currently pending - a user may want to trigger a fix proactively, not only right after a
 * failure. Clicking it collects every midPoint error currently reported for
 * {@link #OBJECT_CLASS_SCRIPT_STEP_IDS} and, if there is at least one (the fix endpoint requires
 * one), triggers the waiting step and jumps the wizard to it; the waiting step itself submits the
 * fix, polls it, applies the result, and (by virtue of its position right before the object class's
 * script-review step in {@code createChildrenSteps()}) hands control back to that step once done.
 * <p>
 * A plain {@link AjaxIconButton} (not a wrapping {@code Panel}) so it can be added directly into a
 * {@code RepeatingView} custom-buttons strip next to "Regenerate" - a Panel there renders an extra
 * empty row element, since the repeater's per-item markup expects a single plain component tag.
 * <p>
 * Embedded identically by every script step of an object class ({@code ScriptsConnectorStepPanel}
 * for the schema step, {@code ScriptConnectorStepPanel} for search/create/update/delete, and
 * {@code NextStepsConnectorStepPanel}), since a blocking error on an early step (e.g. schema) would
 * otherwise leave the fix unreachable if it only lived on the final "next steps" step.
 */
public class RepairObjectClassButton extends AjaxIconButton {

    /**
     * PANEL_TYPE constants of every editable script step of one object class (native/ConnID schema,
     * search all/by-id/filter, create, update, delete). These are fixed strings, not per-object-class
     * ids - the wizard reuses the same step instance across object classes - so the whole set can be
     * used to collect every midPoint error currently reported for the object class shown here.
     */
    public static final List<String> OBJECT_CLASS_SCRIPT_STEP_IDS = List.of(
            SchemaScriptConnectorStepPanel.PANEL_TYPE,
            SearchAllScriptConnectorStepPanel.PANEL_TYPE,
            SearchByIdScriptConnectorStepPanel.PANEL_TYPE,
            SearchFilterScriptConnectorStepPanel.PANEL_TYPE,
            CreateScriptConnectorStepPanel.PANEL_TYPE,
            UpdateScriptConnectorStepPanel.PANEL_TYPE,
            DeleteScriptConnectorStepPanel.PANEL_TYPE);

    private final AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> hostStep;
    private final IModel<String> objectClassModel;
    private final SerializableSupplier<List<ConnDevArtifactType>> currentScriptsSupplier;

    public RepairObjectClassButton(
            String id,
            AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> hostStep,
            IModel<String> objectClassModel) {
        this(id, hostStep, objectClassModel, null);
    }

    /**
     * @param currentScriptsSupplier optional: the host's currently-edited script(s), possibly not
     * yet saved (e.g. because they just failed validation) - sent to the fix as overrides so it
     * fixes what the user is actually looking at, not a stale or empty stored version. Pass
     * {@code null} when the host has no editable script of its own (e.g. {@code NextStepsConnectorStepPanel}).
     */
    public RepairObjectClassButton(
            String id,
            AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> hostStep,
            IModel<String> objectClassModel,
            SerializableSupplier<List<ConnDevArtifactType>> currentScriptsSupplier) {
        super(id, Model.of("fa fa-wrench"), new ResourceModel("NextStepsConnectorStepPanel.repairObjectClass"));
        this.hostStep = hostStep;
        this.objectClassModel = objectClassModel;
        this.currentScriptsSupplier = currentScriptsSupplier;
        showTitleAsLabel(true);
        add(new VisibleBehaviour(() -> objectClassModel.getObject() != null && findWaitingFixStep().isPresent()));
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        onRepairPerformed(target);
    }

    /** Every midPoint error currently reported for any script step of this object class. */
    private List<String> collectErrorMessages() {
        if (!(hostStep.getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            return List.of();
        }
        return ConnectorDevelopmentWizardUtil.collectErrorMessages(
                parentWizardModel.getOperationResultsForFixSteps(OBJECT_CLASS_SCRIPT_STEP_IDS));
    }

    /**
     * The nearest {@link WaitingFixObjectClassConnectorStepPanel} preceding the currently
     * active step in this wizard branch - mirrors how "regenerate" locates its waiting step. Only
     * branches that have one wired into {@code createChildrenSteps()} support the fix action.
     */
    private Optional<WaitingFixObjectClassConnectorStepPanel> findWaitingFixStep() {
        if (!(hostStep.getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            return Optional.empty();
        }
        List<WizardStep> steps = parentWizardModel.getActiveChildrenSteps();
        int activeStepIndex = parentWizardModel.getActiveStepIndex();
        for (int i = activeStepIndex - 1; i >= 0; i--) {
            if (steps.get(i) instanceof WaitingFixObjectClassConnectorStepPanel waitingPanel) {
                return Optional.of(waitingPanel);
            }
        }
        return Optional.empty();
    }

    private void onRepairPerformed(AjaxRequestTarget target) {
        List<String> errors = collectErrorMessages();
        if (errors.isEmpty()) {
            return;
        }
        if (!(hostStep.getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            return;
        }
        List<ConnDevArtifactType> currentScripts = currentScriptsSupplier != null
                ? currentScriptsSupplier.get().stream()
                        .filter(Objects::nonNull)
                        .map(RepairObjectClassButton::toFreshOverride)
                        .filter(Objects::nonNull)
                        .toList()
                : List.of();
        findWaitingFixStep().ifPresent(waitingPanel -> {
            waitingPanel.resetFix(hostStep.getPageBase(), errors, currentScripts);
            parentWizardModel.setActiveStepWithinActivePart(waitingPanel.getStepId());
            parentWizardModel.fireActiveStepChanged();
            target.add(hostStep.getWizard().getPanel());
        });
    }

    /**
     * Rebuilds a fresh, unwrapped {@link ConnDevArtifactType} carrying only the object class and
     * content, the same way {@code ConnectorDevelopmentOperation.submitGenerateEndpointBasedScript}
     * builds its own repair override ({@code artifactDef.create(objectClass).setContent(currentScript)}).
     * Never forward the host's own artifact instance directly: it can originate from a live
     * {@code PrismContainerValueWrapper} (see {@code ScriptsConnectorStepPanel}/{@code
     * ScriptConnectorStepPanel}'s {@code valueModel}), and cloning that into the fix task's work
     * definition fails to marshal ({@code confirm} being required but unset). Returns {@code null}
     * for an artifact that isn't one of the known CRUD/search/schema kinds.
     */
    private static ConnDevArtifactType toFreshOverride(ConnDevArtifactType artifact) {
        var knownType = ConnectorDevelopmentArtifacts.classify(artifact);
        if (knownType == null) {
            return null;
        }
        return knownType.create(artifact.getObjectClass()).content(artifact.getContent());
    }
}
