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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar
 */
public class OperationRequestCaseTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = OperationRequestCaseTabPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(OperationRequestCaseTabPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";

    private static String ID_OPERATIONAL_REQUEST_CASE_PANEL = "operationRequestCasePanel";
    private IModel<SceneDto> sceneModel;

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
        sceneModel = new LoadableModel<SceneDto>() {
            @Override
            protected SceneDto load() {
                PageBase pageBase = OperationRequestCaseTabPanel.this.getPageBase();

                CaseType caseObject =  getObjectWrapper().getObject().asObjectable();
                OperationResult result = new OperationResult(OPERATION_PREPARE_DELTA_VISUALIZATION);
                Task task = pageBase.createSimpleTask(OPERATION_PREPARE_DELTA_VISUALIZATION);
                try {
                    ChangesByState changesByState = pageBase.getWorkflowManager().getChangesByState(caseObject,
                            pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    List<SceneDto> sceneDtoList = WebComponentUtil.computeChangesCategorizationList(changesByState, caseObject.getObjectRef(),
                             pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    return sceneDtoList != null && sceneDtoList.size() > 0 ? sceneDtoList.get(0) : null;
                } catch (Exception ex){
                    LOGGER.error("Couldn't prepare delta visualization, ", ex.getLocalizedMessage());
                }
                return null;
            }
        };
    }


    private void initLayout(){
        ScenePanel scenePanel = new ScenePanel(ID_OPERATIONAL_REQUEST_CASE_PANEL, sceneModel);
        scenePanel.setOutputMarkupId(true);
        add(scenePanel);

//        add(new WebMarkupContainer(ID_OPERATIONAL_REQUEST_CASE_PANEL));
    }

}
