/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.workflow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.form.TextArea;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.cases.component.CorrelationContextPanel;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Created by honchar
 */
public class WorkItemDetailsPanel extends BasePanel<CaseWorkItemType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = WorkItemDetailsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(WorkItemDetailsPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";
    private static final String OPERATION_LOAD_CUSTOM_FORM = DOT_CLASS + "loadCustomForm";
    private static final String OPERATION_LOAD_CASE_FOCUS_OBJECT = DOT_CLASS + "loadCaseFocusObject";
    private static final String OPERATION_CHECK_ACTIONS_AUTHORIZATION = DOT_CLASS + "checkActionsAuthorization";

    private static final String ID_DISPLAY_NAME_PANEL = "displayNamePanel";
    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_FOR = "requestedFor";
    private static final String ID_APPROVER = "approver";
    private static final String ID_CANDIDATE_CONTAINER = "candidateContainer";
    private static final String ID_CANDIDATE = "candidate";
    private static final String ID_PARENT_CASE_CONTAINER = "parentCaseContainer";
    private static final String ID_PARENT_CASE = "parentCase";
    private static final String ID_TARGET = "target";
    private static final String ID_REASON = "reason";
    private static final String ID_COMMENT = "requesterCommentMessage";
    private static final String ID_DELTAS_TO_APPROVE = "deltasToBeApproved";
    private static final String ID_ADDITIONAL_INFORMATION = "additionalInformation";
    private static final String ID_ADDITIONAL_ATTRIBUTES = "additionalAttributes";
    private static final String ID_APPROVER_CONTAINER = "commentContainer";
    private static final String ID_COMMENT_LABEL = "commentLabel";
    private static final String ID_APPROVER_COMMENT = "approverComment";
    private static final String ID_CUSTOM_FORM = "customForm";
    private static final String ID_CASE_WORK_ITEM_EVIDENCE = "caseWorkItemEvidence";
    private static final String ID_CASE_WORK_ITEM_EVIDENCE_FORM = "caseWorkItemEvidenceForm";

    private IModel<VisualizationDto> visualizationModel;
    private LoadableDetachableModel<PrismObject<CaseType>> caseModel;

    public WorkItemDetailsPanel(String id, IModel<CaseWorkItemType> caseWorkItemTypeIModel) {
        super(id, caseWorkItemTypeIModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getRequestedByLinkPanel().add(AttributeAppender.append("aria-description", getString("workItemPanel.requestedBy")));
        getRequestedForLinkPanel().add(AttributeAppender.append("aria-description", getString("workItemPanel.requestedFor")));
        getTargetLinkPanel().add(AttributeAppender.append("aria-description", getString("workItemPanel.target")));
        getApproverLinkPanel().add(AttributeAppender.append("aria-description", getString("workItemPanel.approver")));
        ((RepeatingView)get(createComponentPath(ID_CANDIDATE_CONTAINER, ID_CANDIDATE)))
                .stream().forEach(child -> ((LinkedReferencePanel)child).getLinkPanel().add(
                        AttributeAppender.append("aria-description", getString("workItemPanel.candidateActors"))));
        getParentCaseLinkPanel().add(AttributeAppender.append("aria-description", getString("workItemPanel.parentCase")));
    }

    private AjaxLink getParentCaseLinkPanel() {
        return ((LinkedReferencePanel) get(createComponentPath(ID_PARENT_CASE_CONTAINER, ID_PARENT_CASE))).getLinkPanel();
    }

    private AjaxLink getRequestedByLinkPanel() {
        return ((LinkedReferencePanel) get(ID_REQUESTED_BY)).getLinkPanel();
    }

    private AjaxLink getRequestedForLinkPanel() {
        return ((LinkedReferencePanel) get(ID_REQUESTED_FOR)).getLinkPanel();
    }

    private AjaxLink getTargetLinkPanel() {
        return ((LinkedReferencePanel) get(ID_TARGET)).getLinkPanel();
    }

    private AjaxLink getApproverLinkPanel() {
        return ((LinkedReferencePanel) get(ID_APPROVER)).getLinkPanel();
    }

    private void initModels() {
        visualizationModel = new LoadableModel<>(false) {
            @Override
            protected VisualizationDto load() {
                PageBase pageBase = WorkItemDetailsPanel.this.getPageBase();
                CaseType parentCase = CaseTypeUtil.getCase(WorkItemDetailsPanel.this.getModelObject());
                if (CaseTypeUtil.isManualProvisioningCase(parentCase)) {
                    return WebComponentUtil.createVisualizationDtoForManualCase(parentCase, pageBase, OPERATION_PREPARE_DELTA_VISUALIZATION);
                } else {
                    return WebComponentUtil.createVisualizationDto(WorkItemDetailsPanel.this.getModelObject(), pageBase, OPERATION_PREPARE_DELTA_VISUALIZATION);
                }
            }
        };

        caseModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<CaseType> load() {
                CaseType parentCase = CaseTypeUtil.getCase(WorkItemDetailsPanel.this.getModelObject());
                return parentCase == null ? null : parentCase.asPrismObject();
            }
        };
    }

    private void initLayout() {
        LinkedReferencePanel<?> requestedBy = new LinkedReferencePanel<>(ID_REQUESTED_BY,
                Model.of(WorkItemTypeUtil.getRequestorReference(getModelObject())));
        requestedBy.setOutputMarkupId(true);
        add(requestedBy);

        LinkedReferencePanel<?> requestedFor;
        AssignmentHolderType object = WebComponentUtil.getObjectFromAddDeltaForCase(CaseTypeUtil.getCase(getModelObject()));
        if (object == null) {
            requestedFor = new LinkedReferencePanel<>(ID_REQUESTED_FOR,
                    Model.of(WorkItemTypeUtil.getObjectReference(getModelObject())));
        } else {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(object.getOid());
            ort.setType(WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass()));

            PrismReferenceValue referenceValue = getPageBase().getPrismContext().itemFactory()
                    .createReferenceValue(object.getOid(),
                            WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass()));
            referenceValue.setObject(object.asPrismObject());

            ort.setupReferenceValue(referenceValue);

            requestedFor = new LinkedReferencePanel<>(ID_REQUESTED_FOR, Model.of(ort));
        }
        requestedFor.setOutputMarkupId(true);
        add(requestedFor);

        LinkedReferencePanel<?> approver = new LinkedReferencePanel<>(ID_APPROVER, getApproverModel());
        approver.setOutputMarkupId(true);
        add(approver);

        WebMarkupContainer candidateContainer = new WebMarkupContainer(ID_CANDIDATE_CONTAINER);
        candidateContainer.setOutputMarkupId(true);
        candidateContainer.add(new VisibleBehaviour(() -> CaseTypeUtil.isWorkItemClaimable(getModelObject())));
        add(candidateContainer);

        RepeatingView candidateLinksPanel = new RepeatingView(ID_CANDIDATE);
        candidateLinksPanel.setOutputMarkupId(true);
        List<ObjectReferenceType> candidates = getModelObject() != null ? getModelObject().getCandidateRef() : null;
        if (candidates != null) {
            candidates.forEach(candidate -> {
                LinkedReferencePanel<ObjectReferenceType> candidatePanel = new LinkedReferencePanel<>(candidateLinksPanel.newChildId(), Model.of(candidate));
                candidatePanel.setOutputMarkupId(true);
                candidateLinksPanel.add(candidatePanel);
            });
        }
        candidateContainer.add(candidateLinksPanel);

        WebMarkupContainer parentCaseContainer = new WebMarkupContainer(ID_PARENT_CASE_CONTAINER);
        parentCaseContainer.add(new VisibleBehaviour(() -> getPageBase() instanceof PageCaseWorkItem));
        parentCaseContainer.setOutputMarkupId(true);
        add(parentCaseContainer);

        CaseType parentCaseObj = getModelObject() != null && CaseTypeUtil.getCase(getModelObject()) != null ?
                CaseTypeUtil.getCase(getModelObject()) : null;
        ObjectReferenceType parentCaseRef = null;
        if (parentCaseObj != null) {
            parentCaseRef = new ObjectReferenceType();
            parentCaseRef.setOid(parentCaseObj.getOid());
            parentCaseRef.setType(CaseType.COMPLEX_TYPE);
            parentCaseRef.setupReferenceValue(getPageBase().getPrismContext().itemFactory()
                    .createReferenceValue(parentCaseObj.asPrismObject()));
        }
        LinkedReferencePanel<ObjectReferenceType> parentCaseLink = new LinkedReferencePanel<>(ID_PARENT_CASE, Model.of(parentCaseRef));
        parentCaseLink.setOutputMarkupId(true);
        parentCaseContainer.add(parentCaseLink);

        LinkedReferencePanel<ObjectReferenceType> target = new LinkedReferencePanel<>(ID_TARGET, () -> WorkItemTypeUtil.getTargetReference(getModelObject()));
        target.setOutputMarkupId(true);
        add(target);

        CaseType parentCase = CaseTypeUtil.getCase(getModelObject());
        Label comment = new Label(ID_COMMENT, CaseTypeUtil.getRequesterComment(parentCase));
        comment.setOutputMarkupId(true);
        add(comment);

        EvaluatedTriggerGroupListPanel reasonPanel = new EvaluatedTriggerGroupListPanel(ID_REASON,
                Model.ofList(WebComponentUtil.computeTriggers(parentCase != null ? parentCase.getApprovalContext() : null,
                        parentCase != null && parentCase.getStageNumber() != null ? parentCase.getStageNumber() : 0)));
        reasonPanel.setOutputMarkupId(true);
        add(reasonPanel);

        Component visualizationPanel;
        if (CaseTypeUtil.isApprovalCase(parentCase) || CaseTypeUtil.isManualProvisioningCase(parentCase)) {
            visualizationPanel = new VisualizationPanel(ID_DELTAS_TO_APPROVE, visualizationModel);

        } else if (CaseTypeUtil.isCorrelationCase(parentCase)) {
            visualizationPanel = new CorrelationContextPanel(
                    ID_DELTAS_TO_APPROVE, new CaseDetailsModels(caseModel, getPageBase()), getModel(), new ContainerPanelConfigurationType());
        } else {
            visualizationPanel = new WebMarkupContainer(ID_DELTAS_TO_APPROVE);
        }
        visualizationPanel.setOutputMarkupId(true);
        add(visualizationPanel);

        InformationListPanel additionalInformation = new InformationListPanel(ID_ADDITIONAL_INFORMATION,
                Model.ofList(getModelObject().getAdditionalInformation()));
        additionalInformation.setOutputMarkupId(true);
        add(additionalInformation);

        ApprovalStageDefinitionType level = ApprovalContextUtil.getCurrentStageDefinition(parentCase);
        WebMarkupContainer additionalAttributes = new WebMarkupContainer(ID_ADDITIONAL_ATTRIBUTES);
        add(additionalAttributes);
        additionalAttributes.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null);
            }
        });

        if (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null) {
            String formOid = level.getFormRef().getOid();
            ObjectType focus = getCaseFocusObject(parentCase);
            if (focus == null) {
                focus = new UserType(); // TODO (this should not occur anyway)
            }
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CUSTOM_FORM);
            DynamicFormPanel<?> customForm = new DynamicFormPanel<>(ID_CUSTOM_FORM,
                    focus.asPrismObject(), formOid, null, task, getPageBase(), false);
            additionalAttributes.add(customForm);
        } else {
            additionalAttributes.add(new Label(ID_CUSTOM_FORM));
        }

        Form<?> evidenceForm = new Form<>(ID_CASE_WORK_ITEM_EVIDENCE_FORM);
        evidenceForm.add(new VisibleBehaviour(() -> CaseTypeUtil.isManualProvisioningCase(parentCase) &&
                (!isParentCaseClosed() || WorkItemTypeUtil.getEvidence(getModelObject()) != null)));
        evidenceForm.setMultiPart(true);
        add(evidenceForm);

        UploadDownloadPanel evidencePanel = new UploadDownloadPanel(ID_CASE_WORK_ITEM_EVIDENCE, isParentCaseClosed() && WorkItemTypeUtil.getEvidence(getModelObject()) != null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateValue(byte[] file) {
                if (file != null) {
                    WorkItemTypeUtil.setEvidence(getModelObject(), file);
                }
            }

            @Override
            public InputStream getInputStream() {
                byte[] evidenceFile = WorkItemTypeUtil.getEvidence(getModelObject());
                return evidenceFile != null ? new ByteArrayInputStream(evidenceFile) : new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public String getDownloadContentType() {
                return "image/jpeg";
            }

        };
        evidenceForm.add(evidencePanel);
        evidencePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        evidencePanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                CaseWorkItemType workItem = WorkItemDetailsPanel.this.getModelObject();
                CaseType caseObj = CaseTypeUtil.getCase(workItem);
                return CaseTypeUtil.isManualProvisioningCase(caseObj);
            }
        });

        WebMarkupContainer commentContainer = new WebMarkupContainer(ID_APPROVER_CONTAINER);
        commentContainer.setOutputMarkupId(true);
        commentContainer.add(new VisibleBehaviour(this::isAuthorizedForActions));
        add(commentContainer);

        String key = CaseTypeUtil.isCorrelationCase(caseModel.getObject().asObjectable()) ? "PageCaseWorkItem.caseWorkItem.comment" : "workItemPanel.approverComment";
        Label commentLabel = new Label(ID_COMMENT_LABEL, createStringResource(key));
        commentLabel.setOutputMarkupId(true);
        commentContainer.add(commentLabel);

        TextArea<String> approverComment = new TextArea<>(ID_APPROVER_COMMENT, new PropertyModel<>(getModel(), "output.comment"));
        approverComment.add(AttributeAppender.append("aria-labelledby", commentLabel.getMarkupId()));
        approverComment.add(new EnableBehaviour(() -> !isParentCaseClosed()));
        approverComment.setOutputMarkupId(true);
        approverComment.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        commentContainer.add(approverComment);

    }

    public Component getReasonPanel() {
        return get(ID_REASON);
    }

    public Component getCommentPanel() {
        return get(ID_COMMENT);
    }

    public Component getDeltasPanel() {
        return get(ID_DELTAS_TO_APPROVE);
    }

    private boolean isAuthorizedForActions() {
        if (CaseTypeUtil.isCaseWorkItemClosed(getModelObject())
                || isParentCaseClosed()
                || CaseTypeUtil.isWorkItemClaimable(getModelObject())) {
            return false;
        }

        Task task = getPageBase().createSimpleTask(OPERATION_CHECK_ACTIONS_AUTHORIZATION);
        OperationResult result = task.getResult();
        try {
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getCaseManager().isCurrentUserAuthorizedToComplete(getModelObject(), task, result) ||
                            getPageBase().getCaseManager().isCurrentUserAuthorizedToDelegate(getModelObject(), task, result),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Unable to check user authorization for workitem actions: {}", ex.getLocalizedMessage());
            result.recordException(ex);
        } finally {
            result.close();
        }
        return false;
    }

    protected PrismObject<? extends FocusType> getPowerDonor() {
        return null;
    }

    // Expects that we deal with primary changes of the focus (i.e. not of projections)
    // Beware: returns the full object; regardless of the security settings
    public ObjectType getCaseFocusObject(CaseType caseType) {
        ApprovalContextType wfc = caseType.getApprovalContext();
        if (wfc == null || wfc.getDeltasToApprove() == null || wfc.getDeltasToApprove().getFocusPrimaryDelta() == null) {
            return null;
        }
        ObjectType focus = null;
        ObjectDeltaType delta = wfc.getDeltasToApprove().getFocusPrimaryDelta();
        if (delta.getChangeType() == ChangeTypeType.ADD) {
            focus = CloneUtil.clone((ObjectType) delta.getObjectToAdd());
        } else if (delta.getChangeType() == ChangeTypeType.MODIFY) {
            String oid = delta.getOid();
            if (oid == null) {
                throw new IllegalStateException("No OID in object modify delta: " + delta);
            }
            if (delta.getObjectType() == null) {
                throw new IllegalStateException("No object type in object modify delta: " + delta);
            }
            Class<? extends ObjectType> clazz = ObjectTypes.getObjectTypeFromTypeQName(delta.getObjectType())
                    .getClassDefinition();
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CASE_FOCUS_OBJECT);
            PrismObject<?> object = getPageBase().runPrivileged(() ->
                    WebModelServiceUtils.loadObject(clazz, oid, getPageBase(), task, task.getResult()));
            if (object != null) {
                focus = (ObjectType) object.asObjectable();
                try {
                    ObjectDelta<Objectable> objectDelta = DeltaConvertor.createObjectDelta(delta, getPageBase().getPrismContext());
                    objectDelta.applyTo((PrismObject) focus.asPrismObject());
                } catch (SchemaException e) {
                    throw new SystemException("Cannot apply delta to focus object: " + e.getMessage(), e);
                }
                focus = (ObjectType) object.asObjectable();
            }
        } else {
            // DELETE case: nothing to do here
        }
        return focus;
    }

    public Component getCustomForm() {
        return get(createComponentPath(ID_ADDITIONAL_ATTRIBUTES, ID_CUSTOM_FORM));
    }

    private boolean isParentCaseClosed() {
        CaseType parentCase = CaseTypeUtil.getCase(getModelObject());
        return parentCase != null && SchemaConstants.CASE_STATE_CLOSED.equals(parentCase.getState());
    }

    private IModel<ObjectReferenceType> getApproverModel() {
        if (isParentCaseClosed()) {
            return getModelObject() != null && getModelObject().getPerformerRef() != null ?
                    Model.of(getModelObject().getPerformerRef()) : Model.of();
        } else {
            return getModelObject() != null && getModelObject().getAssigneeRef() != null && getModelObject().getAssigneeRef().size() > 0 ?
                    Model.of(getModelObject().getAssigneeRef().get(0)) : Model.of();
        }
    }
}
