/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * Created by honchar
 */
public class OperationRequestCaseTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = OperationRequestCaseTabPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(OperationRequestCaseTabPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";
    private static final String OPERATION_LOAD_CONNECTED_TASK = DOT_CLASS + "loadConnectedTask";
    private static final String ID_NAVIGATE_TO_TASK_LINK = "navigateToTaskLink";
    private static final String ID_NAVIGATE_TO_TASK_CONTAINER = "navigateToTaskContainer";

    private static final String ID_REQUEST_DETAILS_PANELS = "requestDetailsPanels";
    private static final String ID_OPERATIONAL_REQUEST_CASE_PANEL = "operationRequestCasePanel";
    private IModel<List<SceneDto>> sceneModel;

    public OperationRequestCaseTabPanel(String id, Form<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        sceneModel = new LoadableModel<List<SceneDto>>(false) {
            @Override
            protected List<SceneDto> load() {
                PageBase pageBase = OperationRequestCaseTabPanel.this.getPageBase();

                CaseType caseObject =  getObjectWrapper().getObject().asObjectable();
                OperationResult result = new OperationResult(OPERATION_PREPARE_DELTA_VISUALIZATION);
                Task task = pageBase.createSimpleTask(OPERATION_PREPARE_DELTA_VISUALIZATION);
                try {
                    ChangesByState changesByState = pageBase.getWorkflowManager().getChangesByState(caseObject,
                            pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    List<SceneDto> sceneDtoList = WebComponentUtil.computeChangesCategorizationList(changesByState, caseObject.getObjectRef(),
                             pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    return sceneDtoList;
                } catch (Exception ex){
                    LOGGER.error("Couldn't prepare delta visualization, ", ex.getLocalizedMessage());
                }
                return null;
            }
        };
    }


    private void initLayout(){
        ListView<SceneDto> requestDetailsPanels = new ListView<SceneDto>(ID_REQUEST_DETAILS_PANELS, sceneModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                ScenePanel scenePanel = new ScenePanel(ID_OPERATIONAL_REQUEST_CASE_PANEL, item.getModel());
                scenePanel.setOutputMarkupId(true);
                item.add(scenePanel);
            }
        };
        requestDetailsPanels.setOutputMarkupId(true);
        add(requestDetailsPanels);

        PrismObject<TaskType> executingChangesTask = WebComponentUtil.getCaseExecutingChangesTask(OPERATION_LOAD_CONNECTED_TASK,
                getObjectWrapper().getOid(), OperationRequestCaseTabPanel.this.getPageBase());
        WebMarkupContainer taskLinkContainer = new WebMarkupContainer(ID_NAVIGATE_TO_TASK_CONTAINER);
        taskLinkContainer.setOutputMarkupId(true);
        taskLinkContainer.add(new VisibleBehaviour(() -> executingChangesTask != null));
        add(taskLinkContainer);

        AjaxButton redirectToTaskLink = new AjaxButton(ID_NAVIGATE_TO_TASK_LINK,
                Model.of(WebComponentUtil.getDisplayNameOrName(executingChangesTask, true))) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                ObjectReferenceType taskRef = null;
                if (executingChangesTask != null) {
                    taskRef = new ObjectReferenceType();
                    taskRef.setOid(executingChangesTask.getOid());
                    taskRef.setType(TaskType.COMPLEX_TYPE);
                }
                if (StringUtils.isNotEmpty(taskRef.getOid())) {
                    WebComponentUtil.dispatchToObjectDetailsPage(taskRef, OperationRequestCaseTabPanel.this, false);
                }
            }
        };
        redirectToTaskLink.setOutputMarkupId(true);
        taskLinkContainer.add(redirectToTaskLink);

    }

}
