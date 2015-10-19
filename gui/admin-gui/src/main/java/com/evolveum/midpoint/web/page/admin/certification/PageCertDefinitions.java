/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.MultiButtonColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/definitions", action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)
        })
public class PageCertDefinitions extends PageAdminWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertDefinitions.class);

    private static final String DOT_CLASS = PageCertDefinitions.class.getName() + ".";
    private static final String OPERATION_CREATE_CAMPAIGN = DOT_CLASS + "createCampaign";
    private static final String OPERATION_DELETE_DEFINITION = DOT_CLASS + "deleteDefinition";
    private static final String DIALOG_CONFIRM_DELETE = "confirmDeletePopup";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DEFINITIONS_TABLE = "definitionsTable";
    private AccessCertificationDefinitionType singleDelete;

    public PageCertDefinitions() {
        initLayout();
    }

    //region Data
    private ObjectDataProvider createProvider() {
        ObjectDataProvider provider = new ObjectDataProvider(PageCertDefinitions.this, AccessCertificationDefinitionType.class);
        provider.setQuery(createQuery());
        return provider;
    }

    private ObjectDataProvider getDataProvider() {
        DataTable table = getDefinitionsTable().getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private ObjectQuery createQuery() {
        // TODO implement searching capabilities here
        ObjectQuery query = new ObjectQuery();
        return query;
    }
    //endregion

    //region Layout

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ObjectDataProvider provider = createProvider();
        
        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE,
                createStringResource("PageCertDefinitions.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteDefinitionPerformed(target, singleDelete);
            }
        });
        BoxedTablePanel table = new BoxedTablePanel<>(ID_DEFINITIONS_TABLE, provider, initColumns());
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }
    
    private Table getTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_DEFINITIONS_TABLE));
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(singleDelete == null){
                    return "";
                } else {
                    return createStringResource("PageCertDefinitions.deleteDefinitionConfirmSingle",
                            singleDelete.getName()).getString();
                }
            }
        };
    }
    private List<IColumn<AccessCertificationDefinitionType, String>> initColumns() {
        List<IColumn<AccessCertificationDefinitionType, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn());
        
        IColumn column;
        
//        column = new CheckBoxHeaderColumn<>();
//        columns.add(column);

        column = new LinkColumn<SelectableBean<AccessCertificationDefinitionType>>(createStringResource("PageCertDefinitions.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationDefinitionType>> rowModel) {
                definitionDetailsPerformed(target, rowModel.getObject().getValue().getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCertDefinitions.table.description"), "value.description");
        columns.add(column);

        column = new MultiButtonColumn<SelectableBean<AccessCertificationDefinitionType>>(new Model(), 3) {

            private final String[] captionKeys = {
                    "PageCertDefinitions.button.createCampaign",
                    "PageCertDefinitions.button.showCampaigns",
                    "PageCertDefinitions.button.deleteDefinition"
            };

            private final DoubleButtonColumn.BUTTON_COLOR_CLASS[] colors = {
                    DoubleButtonColumn.BUTTON_COLOR_CLASS.PRIMARY,
                    DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT,
                    DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER
            };

            @Override
            public String getCaption(int id) {
                return PageCertDefinitions.this.createStringResource(captionKeys[id]).getString();
            }

            @Override
            public String getButtonColorCssClass(int id) {
                return colors[id].toString();
            }

            @Override
            public void clickPerformed(int id, AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationDefinitionType>> model) {
                switch (id) {
                    case 0: createCampaignPerformed(target, model.getObject().getValue()); break;
                    case 1: showCampaignsPerformed(target, model.getObject().getValue()); break;
                    case 2: deleteConfirmation(target, model.getObject().getValue()); break;
                }
            }

        };
        columns.add(column);

        return columns;
    }

    private Table getDefinitionsTable(){
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_DEFINITIONS_TABLE));
    }
    //endregion Layout

    //region Actions
    private void showCampaignsPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, definition.getOid());
        setResponsePage(PageCertCampaigns.class, parameters);
    }

    private void createCampaignPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
        LOGGER.debug("Create certification campaign performed for {}", definition.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_CREATE_CAMPAIGN);
        try {
            Task task = createSimpleTask(OPERATION_CREATE_CAMPAIGN);
            getCertificationManager().createCampaign(definition, null, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
    
    private void deleteConfirmation(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
    	
    	   this.singleDelete = definition;
           ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
           dialog.show(target);
       }

    private void deleteDefinitionPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
        OperationResult result = new OperationResult(OPERATION_DELETE_DEFINITION);
        try {
            Task task = createSimpleTask(OPERATION_DELETE_DEFINITION);
            ObjectDelta<AccessCertificationDefinitionType> delta =
                    ObjectDelta.createDeleteDelta(AccessCertificationDefinitionType.class, definition.getOid(),
                            getPrismContext());
            getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
        } catch (Exception ex) {
            result.recordPartialError("Couldn't delete campaign definition.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't delete campaign definition", ex);
        }

        result.computeStatusIfUnknown();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The definition has been successfully deleted.");
        }

        Table table = getDefinitionsTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), (Component) table);
    }

    private void editAsXmlPerformed(AccessCertificationDefinitionType definition){
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, definition.getOid());
        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, AccessCertificationDefinitionType.class.getSimpleName());
        setResponsePage(PageDebugView.class, parameters);
    }

    private void definitionDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        setResponsePage(new PageCertDefinition(parameters, PageCertDefinitions.this));
    }

    //endregion
}
