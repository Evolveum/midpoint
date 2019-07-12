/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.IconedObjectNamePanel;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public class WorkItemDetailsPanel extends BasePanel<CaseWorkItemType>{
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = WorkItemDetailsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(WorkItemDetailsPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";
    private static final String OPERATION_LOAD_CUSTOM_FORM = DOT_CLASS + "loadCustomForm";
    private static final String OPERATION_LOAD_CASE_FOCUS_OBJECT = DOT_CLASS + "loadCaseFocusObject";

    private static final String ID_DISPLAY_NAME_PANEL = "displayNamePanel";
    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_FOR = "requestedFor";
    private static final String ID_TARGET = "target";
    private static final String ID_REASON = "reason";
    private static final String ID_COMMENT = "requesterCommentMessage";
    private static final String ID_DELTAS_TO_APPROVE = "deltasToBeApproved";
    private static final String ID_ADDITIONAL_ATTRIBUTES = "additionalAttributes";
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
        sceneModel = new LoadableModel<SceneDto>() {
            @Override
            protected SceneDto load() {
                PageBase pageBase = WorkItemDetailsPanel.this.getPageBase();
                return WebComponentUtil.createSceneDto(WorkItemDetailsPanel.this.getModelObject(), pageBase,  OPERATION_PREPARE_DELTA_VISUALIZATION);
            }
        };
        evidenceFile = WorkItemTypeUtil.getEvidence(getModelObject());
    }

    private void initLayout(){
        IconedObjectNamePanel requestedBy = new IconedObjectNamePanel(ID_REQUESTED_BY,
                WorkItemTypeUtil.getRequestorReference(getModelObject()));
        requestedBy.setOutputMarkupId(true);
        add(requestedBy);

        //todo fix what is requested for object ?
        IconedObjectNamePanel requestedFor = new IconedObjectNamePanel(ID_REQUESTED_FOR,
                WorkItemTypeUtil.getObjectReference(getModelObject()));
        requestedFor.setOutputMarkupId(true);
        add(requestedFor);

        IconedObjectNamePanel target = new IconedObjectNamePanel(ID_TARGET,
                WorkItemTypeUtil.getTargetReference(getModelObject()));
        target.setOutputMarkupId(true);
        add(target);

        CaseType parentCase = CaseTypeUtil.getCase(getModelObject());
        add(new Label(ID_COMMENT, CaseTypeUtil.getRequesterComment(parentCase)));

        EvaluatedTriggerGroupListPanel reasonPanel = new EvaluatedTriggerGroupListPanel(ID_REASON,
                Model.ofList(WebComponentUtil.computeTriggers(parentCase != null ? parentCase.getApprovalContext() : null,
                        parentCase != null && parentCase.getStageNumber() != null ? parentCase.getStageNumber() : 0)));
        reasonPanel.setOutputMarkupId(true);
        add(reasonPanel);


        if (CaseTypeUtil.isApprovalCase(parentCase)){
            ScenePanel scenePanel = new ScenePanel(ID_DELTAS_TO_APPROVE, sceneModel);
            scenePanel.setOutputMarkupId(true);
            scenePanel.add(new VisibleBehaviour(() -> CaseTypeUtil.isApprovalCase(parentCase)));
            add(scenePanel);
        } else {
            add(new WebMarkupContainer(ID_DELTAS_TO_APPROVE));
        }


        ApprovalStageDefinitionType level = ApprovalContextUtil.getCurrentStageDefinition(parentCase);
        WebMarkupContainer additionalAttributes = new WebMarkupContainer(ID_ADDITIONAL_ATTRIBUTES);
        add(additionalAttributes);
        additionalAttributes.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null);
            };
        });

        if (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null) {
            String formOid = level.getFormRef().getOid();
            ObjectType focus = getCaseFocusObject(parentCase);
            if (focus == null) {
                focus = new UserType(getPageBase().getPrismContext());		// TODO (this should not occur anyway)
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
        add(evidenceForm);

        UploadDownloadPanel evidencePanel = new UploadDownloadPanel(ID_CASE_WORK_ITEM_EVIDENCE,
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
        add(approverComment);

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
