/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

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
    private static final String ID_PARENT_CASE_CONTAINER = "parentCaseContainer";
    private static final String ID_PARENT_CASE = "parentCase";
    private static final String ID_TARGET = "target";
    private static final String ID_REASON = "reason";
    private static final String ID_COMMENT = "requesterCommentMessage";
    private static final String ID_DELTAS_TO_APPROVE = "deltasToBeApproved";
    private static final String ID_ADDITIONAL_INFORMATION = "additionalInformation";
    private static final String ID_ADDITIONAL_ATTRIBUTES = "additionalAttributes";
    private static final String ID_APPROVER_CONTAINER = "commentContainer";
    private static final String ID_APPROVER_COMMENT = "approverComment";
    private static final String ID_CUSTOM_FORM = "customForm";
    private static final String ID_CASE_WORK_ITEM_EVIDENCE = "caseWorkItemEvidence";
    private static final String ID_CASE_WORK_ITEM_EVIDENCE_FORM = "caseWorkItemEvidenceForm";


    private IModel<SceneDto> sceneModel;
    private String approverCommentValue = null;
    private byte[] evidenceFile = null;

    public WorkItemDetailsPanel(String id, IModel<CaseWorkItemType> caseWorkItemTypeIModel) {
        super(id, caseWorkItemTypeIModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        sceneModel = new LoadableModel<SceneDto>(false) {
            @Override
            protected SceneDto load() {
                PageBase pageBase = WorkItemDetailsPanel.this.getPageBase();
                CaseType parentCase = CaseTypeUtil.getCase(WorkItemDetailsPanel.this.getModelObject());
                if (CaseTypeUtil.isManualProvisioningCase(parentCase)){
                    return WebComponentUtil.createSceneDtoForManualCase(parentCase, pageBase,  OPERATION_PREPARE_DELTA_VISUALIZATION);
                } else {
                    return WebComponentUtil.createSceneDto(WorkItemDetailsPanel.this.getModelObject(), pageBase, OPERATION_PREPARE_DELTA_VISUALIZATION);
                }
            }
        };
        evidenceFile = WorkItemTypeUtil.getEvidence(getModelObject());
    }

    private void initLayout(){
        LinkedReferencePanel requestedBy = new LinkedReferencePanel(ID_REQUESTED_BY,
                Model.of(WorkItemTypeUtil.getRequestorReference(getModelObject())));
        requestedBy.setOutputMarkupId(true);
        add(requestedBy);

        LinkedReferencePanel requestedFor;
        AssignmentHolderType object = WebComponentUtil.getObjectFromAddDeltyForCase(CaseTypeUtil.getCase(getModelObject()));
        if (object == null) {
            requestedFor = new LinkedReferencePanel(ID_REQUESTED_FOR,
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

            requestedFor = new LinkedReferencePanel(ID_REQUESTED_FOR, Model.of(ort));
        }
        requestedFor.setOutputMarkupId(true);
        add(requestedFor);


        LinkedReferencePanel approver = new LinkedReferencePanel(ID_APPROVER,
                getModelObject() != null && getModelObject().getAssigneeRef() != null && getModelObject().getAssigneeRef().size() > 0 ?
                Model.of(getModelObject().getAssigneeRef().get(0)) : Model.of());
        approver.setOutputMarkupId(true);
        add(approver);

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
        LinkedReferencePanel parentCaseLink = new LinkedReferencePanel(ID_PARENT_CASE, Model.of(parentCaseRef));
        parentCaseLink.setOutputMarkupId(true);
        parentCaseContainer.add(parentCaseLink);

        LinkedReferencePanel target = new LinkedReferencePanel(ID_TARGET,
                Model.of(WorkItemTypeUtil.getTargetReference(getModelObject())));
        target.setOutputMarkupId(true);
        add(target);

        CaseType parentCase = CaseTypeUtil.getCase(getModelObject());
        add(new Label(ID_COMMENT, CaseTypeUtil.getRequesterComment(parentCase)));

        EvaluatedTriggerGroupListPanel reasonPanel = new EvaluatedTriggerGroupListPanel(ID_REASON,
                Model.ofList(WebComponentUtil.computeTriggers(parentCase != null ? parentCase.getApprovalContext() : null,
                        parentCase != null && parentCase.getStageNumber() != null ? parentCase.getStageNumber() : 0)));
        reasonPanel.setOutputMarkupId(true);
        add(reasonPanel);


        if (CaseTypeUtil.isApprovalCase(parentCase) || CaseTypeUtil.isManualProvisioningCase(parentCase)){
            ScenePanel scenePanel = new ScenePanel(ID_DELTAS_TO_APPROVE, sceneModel);
            scenePanel.setOutputMarkupId(true);
            add(scenePanel);
        } else {
            add(new WebMarkupContainer(ID_DELTAS_TO_APPROVE));
        }

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
                focus = new UserType(getPageBase().getPrismContext());        // TODO (this should not occur anyway)
            }
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CUSTOM_FORM);
            DynamicFormPanel<?> customForm = new DynamicFormPanel<>(ID_CUSTOM_FORM,
                    focus.asPrismObject(), formOid, null, task, getPageBase(), false);
            additionalAttributes.add(customForm);
        } else {
            additionalAttributes.add(new Label(ID_CUSTOM_FORM));
        }

        Form evidenceForm = new Form(ID_CASE_WORK_ITEM_EVIDENCE_FORM);
        evidenceForm.add(new VisibleBehaviour(() -> CaseTypeUtil.isManualProvisioningCase(parentCase) &&
                (!SchemaConstants.CASE_STATE_CLOSED.equals(parentCase.getState()) || WorkItemTypeUtil.getEvidence(getModelObject()) != null)));
        evidenceForm.setMultiPart(true);
        add(evidenceForm);

        UploadDownloadPanel evidencePanel = new UploadDownloadPanel(ID_CASE_WORK_ITEM_EVIDENCE, parentCase != null &&
                SchemaConstants.CASE_STATE_CLOSED.equals(parentCase.getState()) && WorkItemTypeUtil.getEvidence(getModelObject()) != null){
            private static final long serialVersionUID = 1L;

            @Override
            public void updateValue(byte[] file) {
                if (file != null) {
                    evidenceFile = Arrays.copyOf(file, file.length);
                }
            }

            @Override
            public InputStream getStream() {
                return evidenceFile != null ? new ByteArrayInputStream((byte[]) evidenceFile) : new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public String getDownloadContentType() {
                return "image/jpeg";
            }

        };
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
        evidenceForm.add(evidencePanel);

        WebMarkupContainer commentContainer = new WebMarkupContainer(ID_APPROVER_CONTAINER);
        commentContainer.setOutputMarkupId(true);
        commentContainer.add(new VisibleBehaviour(() -> isAuthorizedForActions()));
        add(commentContainer);

        TextArea<String> approverComment = new TextArea<String>(ID_APPROVER_COMMENT, new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(String newValue) {
                approverCommentValue = newValue;
            }

            @Override
            public String getObject() {
                return approverCommentValue;
            }
        });
        approverComment.setOutputMarkupId(true);
        approverComment.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        commentContainer.add(approverComment);

    }

    private boolean isAuthorizedForActions() {
        Task task = getPageBase().createSimpleTask(OPERATION_CHECK_ACTIONS_AUTHORIZATION);
        OperationResult result = task.getResult();
        try {
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getWorkflowManager().isCurrentUserAuthorizedToSubmit(getModelObject(), task, result) ||
                                    getPageBase().getWorkflowManager().isCurrentUserAuthorizedToDelegate(getModelObject(), task, result) ||
                                    getPageBase().getWorkflowManager().isCurrentUserAuthorizedToClaim(getModelObject()),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Unable to check user authorization for workitem actions: {}", ex.getLocalizedMessage());
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

    public String getApproverComment(){
        return approverCommentValue;
    }

    public byte[] getWorkItemEvidence(){
        return evidenceFile;
    }

    public Component getCustomForm(){
        return get(createComponentPath(ID_ADDITIONAL_ATTRIBUTES, ID_CUSTOM_FORM));
    }
}
