/**
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class MergeObjectsPanel<F extends FocusType> extends BasePanel{
    private static final Trace LOGGER = TraceManager.getTrace(MergeObjectsPanel.class);
    private static final String DOT_CLASS = MergeObjectsPanel.class.getName() + ".";
    private static final String OPERATION_GET_MERGE_OBJECT_PREVIEW = DOT_CLASS + "getMergeObjectPreview";
    private static final String OPERATION_LOAD_MERGE_TYPE_NAMES = DOT_CLASS + "loadMergeTypeNames";


    private static final String ID_MERGE_OBJECT_DETAILS_PANEL = "mergeObjectDetailsPanel";
    private static final String ID_MERGE_WITH_OBJECT_DETAILS_PANEL = "mergeWithObjectDetailsPanel";
    private static final String ID_MERGE_RESULT_OBJECT_DETAILS_PANEL = "mergeResultObjectDetailsPanel";
    private static final String ID_MERGE_RESULT_PANEL_CONTAINER = "mergeResultPanelContainer";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_MERGE_DELTA_PREVIEW_BUTTON = "mergeDeltaPreview";
    private static final String ID_MERGE_BUTTON = "merge";
    private static final String ID_FORM = "mainForm";
    private static final String ID_MERGE_TYPE_SELECTOR = "mergeType";
    private static final String ID_SWITCH_DIRECTION_BUTTON = "switchDirectionButton";
    private static final String ID_OBJECTS_PANEL = "objectsPanel";

    private IModel<F> mergeObjectModel;
    private IModel<F> mergeWithObjectModel;
    private PrismObject<F> mergeResultObject;
    private MergeDeltas<F> mergeDeltas;
    private Class<F> type;
    private PageBase pageBase;
    private IModel<String> mergeTypeModel;
    private String currentMergeType = "";
    private IModel<List<String>> mergeTypeChoicesModel;
    private List<String> mergeTypeChoices;

    public MergeObjectsPanel(String id){
        super(id);
    }

    public MergeObjectsPanel(String id, IModel<F> mergeObjectModel, IModel<F> mergeWithObjectModel, Class<F> type, PageBase pageBase){
        super(id);
        this.mergeObjectModel = mergeObjectModel;
        this.mergeWithObjectModel = mergeWithObjectModel;
        this.type = type;
        this.pageBase = pageBase;
        mergeTypeChoices = getMergeTypeNames();

        initModels();
        initLayout();
    }

    private void initModels(){
        mergeTypeModel = new IModel<String>() {
            @Override
            public String getObject() {
                return currentMergeType;
            }

            @Override
            public void setObject(String mergeType) {
                currentMergeType = mergeType;
            }

            @Override
            public void detach() {

            }
        };

        mergeTypeChoicesModel = new IModel<List<String>>() {
            @Override
            public List<String> getObject() {
                return mergeTypeChoices;
            }

            @Override
            public void setObject(List<String> strings) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private void initLayout(){
        Form mainForm =  new Form(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        DropDownChoicePanel mergeTypeSelect = new DropDownChoicePanel(ID_MERGE_TYPE_SELECTOR,
                mergeTypeModel, mergeTypeChoicesModel);

        mergeTypeSelect.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                mergeResultObject = getMergeObjectsResult();
                WebMarkupContainer resultObjectPanel = (WebMarkupContainer)get(ID_FORM)
                        .get(ID_OBJECTS_PANEL).get(ID_MERGE_RESULT_PANEL_CONTAINER);
                resultObjectPanel.addOrReplace(getMergeResultObjectPanel());
                target.add(resultObjectPanel);
            }
        });
        mergeTypeSelect.setOutputMarkupId(true);
        mainForm.add(mergeTypeSelect);

        final WebMarkupContainer objectsPanel = new WebMarkupContainer(ID_OBJECTS_PANEL);
        objectsPanel.setOutputMarkupId(true);
        mainForm.addOrReplace(objectsPanel);

        initObjectsPanel(objectsPanel);

        AjaxButton switchDirectionButton = new AjaxButton(ID_SWITCH_DIRECTION_BUTTON,
                pageBase.createStringResource("MergeObjectsPanel.switchDirection")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                F temp = mergeObjectModel.getObject();
                mergeObjectModel.setObject(mergeWithObjectModel.getObject());
                mergeWithObjectModel.setObject(temp);
                initObjectsPanel(objectsPanel);

                ajaxRequestTarget.add(objectsPanel);
            }
        };
        switchDirectionButton.setOutputMarkupId(true);
        mainForm.add(switchDirectionButton);
    }

    private void initObjectsPanel(WebMarkupContainer objectsPanel){

        MergeObjectDetailsPanel mergeObjectPanel = new MergeObjectDetailsPanel(ID_MERGE_OBJECT_DETAILS_PANEL,
                mergeObjectModel.getObject(), type);
        mergeObjectPanel.setOutputMarkupId(true);
        objectsPanel.addOrReplace(mergeObjectPanel);

        MergeObjectDetailsPanel mergeWithObjectPanel = new MergeObjectDetailsPanel(ID_MERGE_WITH_OBJECT_DETAILS_PANEL,
                mergeWithObjectModel.getObject(), type);
        mergeWithObjectPanel.setOutputMarkupId(true);
        objectsPanel.addOrReplace(mergeWithObjectPanel);

        mergeResultObject = getMergeObjectsResult();

        WebMarkupContainer mergeResultPanelContainer = new WebMarkupContainer(ID_MERGE_RESULT_PANEL_CONTAINER);
        mergeResultPanelContainer.setOutputMarkupId(true);
        objectsPanel.addOrReplace(mergeResultPanelContainer);
        mergeResultPanelContainer.addOrReplace(getMergeResultObjectPanel());
    }

    private Component getMergeResultObjectPanel(){
        Component mergeObjectsResultPanel;
        if (mergeResultObject != null) {
            mergeObjectsResultPanel = new MergeObjectDetailsPanel(ID_MERGE_RESULT_OBJECT_DETAILS_PANEL,
                    mergeResultObject.asObjectable(), type);
        } else {
            mergeObjectsResultPanel = new Label(ID_MERGE_RESULT_OBJECT_DETAILS_PANEL,
                    pageBase.createStringResource("PageMergeObjects.noMergeResultObjectWarning"));
        }
        mergeObjectsResultPanel.setOutputMarkupId(true);
        return mergeObjectsResultPanel;
    }

    private List<String> getMergeTypeNames(){
        List<String> mergeTypeNamesList = new ArrayList<>();
        Task task = pageBase.createAnonymousTask(OPERATION_LOAD_MERGE_TYPE_NAMES);
        OperationResult result = task.getResult();

        PrismObject<SystemConfigurationType> config;
        try {
            config = pageBase.getModelService().getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, task, result);
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException
                | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
            return null;
        }
        if (config != null && config.asObjectable() != null){
            List<MergeConfigurationType> list = config.asObjectable().getMergeConfiguration();
            if (list != null) {
                for (MergeConfigurationType mergeType : list) {
                    mergeTypeNamesList.add(mergeType.getName());
                }
                if (mergeTypeNamesList.size() > 0){
                    currentMergeType = mergeTypeNamesList.get(0);
                }
            }
        }
        return mergeTypeNamesList;
    }
    
    private PrismObject<F> getMergeObjectsResult() {
        OperationResult result = new OperationResult(OPERATION_GET_MERGE_OBJECT_PREVIEW);
        PrismObject<F> mergeResultObject = null;
        try {
            Task task = pageBase.createSimpleTask(OPERATION_GET_MERGE_OBJECT_PREVIEW);
            mergeResultObject = pageBase.getModelInteractionService().mergeObjectsPreviewObject(type,
                    mergeObjectModel.getObject().getOid(), mergeWithObjectModel.getObject().getOid(), currentMergeType, task, result);
            mergeDeltas = pageBase.getModelInteractionService().mergeObjectsPreviewDeltas(type,
                    mergeObjectModel.getObject().getOid(), mergeWithObjectModel.getObject().getOid(), currentMergeType, task, result);
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError("Couldn't get merge object for preview.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get merge object for preview", ex);
            pageBase.showResult(result);
        }
        return mergeResultObject;
    }

    public PrismObject<F> getMergeResultObject() {
        return mergeResultObject;
    }

    public MergeDeltas<F> getMergeDeltas() {
        return mergeDeltas;

    }

    public String getMergeConfigurationName(){
        return currentMergeType;
    }
}
