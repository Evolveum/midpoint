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
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.IconedObjectNamePanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.prism.show.SceneUtil;
import com.evolveum.midpoint.web.page.admin.server.TaskChangesPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by honchar
 */
public class WorkItemDetailsPanel extends BasePanel<CaseWorkItemType>{
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = WorkItemDetailsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(WorkItemDetailsPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";

    private static final String ID_DISPLAY_NAME_PANEL = "displayNamePanel";
    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_FOR = "requestedFor";
    private static final String ID_TARGET = "target";
    private static final String ID_REASON = "reason";
    private static final String ID_COMMENT = "requesterCommentMessage";
    private static final String ID_DELTAS_TO_APPROVE = "deltasToBeApproved";

    private IModel<SceneDto> sceneModel;

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
        EvaluatedTriggerGroupListPanel reasonPanel = new EvaluatedTriggerGroupListPanel(ID_REASON,
                Model.ofList(WebComponentUtil.computeTriggers(parentCase != null ? parentCase.getWorkflowContext() : null,
                        parentCase != null ? parentCase.getStageNumber() : 0)));
        reasonPanel.setOutputMarkupId(true);
        add(reasonPanel);

        add(new ScenePanel(ID_DELTAS_TO_APPROVE, sceneModel));


    }
}
