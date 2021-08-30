/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.PagePreviewChanges;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PageAssignmentHolderDetails<AH extends AssignmentHolderType, AHDM extends AssignmentHolderDetailsModel<AH>> extends AbstractPageObjectDetails<AH, AHDM> implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentHolderDetails.class);
    public PageAssignmentHolderDetails(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected OperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> wrapperModel) {
        return new AssignmentHolderOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void addArchetypePerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                OperationResult result = new OperationResult(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
                if (newAssignmentsList.size() > 1) {
                    result.recordWarning(getString("PageAdminObjectDetails.change.archetype.more.than.one.selected"));
                    getPageBase().showResult(result);
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                AssignmentType oldArchetypAssignment = getOldArchetypeAssignment(result);
                if (oldArchetypAssignment == null) {
                    getPageBase().showResult(result);
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                changeArchetype(oldArchetypAssignment, newAssignmentsList, result, target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.savePerformed(target);
            }
        };
    }

    private void changeArchetype(AssignmentType oldArchetypAssignment, List<AssignmentType> newAssignmentsList, OperationResult result, AjaxRequestTarget target) {
        try {
            ObjectDelta<AH> delta = getPrismContext().deltaFor(getModelPrismObject().getCompileTimeClass())
                    .item(AssignmentHolderType.F_ASSIGNMENT)
                    .delete(oldArchetypAssignment.clone())
                    .asObjectDelta(getModelPrismObject().getOid());
            delta.addModificationAddContainer(AssignmentHolderType.F_ASSIGNMENT, newAssignmentsList.iterator().next());

            Task task = createSimpleTask(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
            getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);

        } catch (Exception e) {
            LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage(), e);
            result.recordFatalError(getString("PageAdminObjectDetails.change.archetype.failed", e.getMessage()), e);

        }
        result.computeStatusIfUnknown();
        showResult(result);
        target.add(getFeedbackPanel());
        PageAssignmentHolderDetails.this.refresh(target);
    }

    private AssignmentType getOldArchetypeAssignment(OperationResult result) {
        PrismContainer<AssignmentType> assignmentContainer = getModelWrapperObject().getObjectOld().findContainer(AssignmentHolderType.F_ASSIGNMENT);
        if (assignmentContainer == null) {
            //should not happen either
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        List<AssignmentType> oldAssignments = assignmentContainer.getRealValues().stream().filter(a -> WebComponentUtil.isArchetypeAssignment(a)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oldAssignments)) {
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        if (oldAssignments.size() > 1) {
            result.recordFatalError(getString("PageAdminObjectDetails.archetype.change.no.single.archetype"));
            return null;
        }
        return oldAssignments.iterator().next();
    }

    @Override
    public void startProcessing(AjaxRequestTarget target, OperationResult result) {
        LOGGER.trace("startProcessing called, making main panel invisible");
        getDetailsPanel().setVisible(false);
        target.add(getDetailsPanel());
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if (previewRequested) {
            finishPreviewProcessing(target, result);
            return;
        }

        setTimezoneIfNeeded(result);

        boolean focusAddAttempted = getDelta() != null && getDelta().isAdd();
        boolean focusAddSucceeded = focusAddAttempted && StringUtils.isNotEmpty(getDelta().getOid());

        // we don't want to allow resuming editing if a new focal object was created (on second 'save' there would be a conflict with itself)
        // and also in case of partial errors, like those related to projections (many deltas would be already executed, and this could cause problems on second 'save').
        boolean canContinueEditing = !focusAddSucceeded && result.isFatalError();

        boolean canExitPage;
        if (returningFromAsync) {
            canExitPage = getProgressPanel().isAllSuccess() || result.isInProgress() || result.isHandledError(); // if there's at least a warning in the progress table, we would like to keep the table open
        } else {
            canExitPage = !canContinueEditing;                            // no point in staying on page if we cannot continue editing (in synchronous case i.e. no progress table present)
        }

        if (!isKeepDisplayingResults() && canExitPage) {
            showResult(result);
            redirectBack();
        } else {
            if (returningFromAsync) {
                getProgressPanel().showBackButton(target);
                getProgressPanel().hideAbortButton(target);
            }
            showResult(result);
            target.add(getFeedbackPanel());

            if (canContinueEditing) {
                getProgressPanel().hideBackButton(target);
                getProgressPanel().showContinueEditingButton(target);
            }
        }
    }

    private void setTimezoneIfNeeded(OperationResult result) {
        if (result.isSuccess() && getDelta() != null && SecurityUtils.getPrincipalUser().getOid().equals(getDelta().getOid())) {
            Session.get().setLocale(WebComponentUtil.getLocale());
            LOGGER.debug("Using {} as locale", getLocale());
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(WebModelServiceUtils.getTimezone());
            LOGGER.debug("Using {} as time zone", WebSession.get().getClientInfo().getProperties().getTimeZone());
        }
    }

    protected boolean isKeepDisplayingResults() {
        return getExecuteChangesOptionsDto().isKeepDisplayingResults();
    }

    private ObjectDelta<AH> getDelta() {
        return getObjectDetailsModels().getDelta();
    }

        private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
        getDetailsPanel().setVisible(true);
        getProgressPanel().hide();
        getProgressPanel().hideAbortButton(target);
        getProgressPanel().hideBackButton(target);
        getProgressPanel().hideContinueEditingButton(target);

        showResult(result);
        target.add(getFeedbackPanel());

        Map<PrismObject<AH>, ModelContext<? extends ObjectType>> modelContextMap = new LinkedHashMap<>();
        modelContextMap.put(getModelPrismObject(), getProgressPanel().getPreviewResult());

        //TODO
//        processAdditionalFocalObjectsForPreview(modelContextMap);

        navigateToNext(new PagePreviewChanges(modelContextMap, getModelInteractionService()));
    }


    @Override
    public void continueEditing(AjaxRequestTarget target) {
        getDetailsPanel().setVisible(true);
        getProgressPanel().hide();
        getProgressPanel().hideAbortButton(target);
        getProgressPanel().hideBackButton(target);
        getProgressPanel().hideContinueEditingButton(target);
        target.add(this);
    }

    protected AHDM createObjectDetailsModels() {
        return (AHDM) new AssignmentHolderDetailsModel<>(createPrismObejctModel(), this);
    }

}
