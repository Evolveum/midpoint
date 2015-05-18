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

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/certificationTypes", action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)
        })
public class PageCertificationTypes extends PageAdminWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertificationTypes.class);

    private static final String DOT_CLASS = PageCertificationTypes.class.getName() + ".";
    private static final String OPERATION_RUN_CERTIFICATION = DOT_CLASS + "runCertification";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CERTIFICATION_TYPES_TABLE = "certificationTypesTable";

    public PageCertificationTypes() {
        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("page.subTitle").getString();
            }
        };
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ObjectDataProvider provider = new ObjectDataProvider(PageCertificationTypes.this, AccessCertificationDefinitionType.class);
        provider.setQuery(createQuery());
        TablePanel table = new TablePanel<>(ID_CERTIFICATION_TYPES_TABLE, provider, initColumns());
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<IColumn<AccessCertificationDefinitionType, String>> initColumns() {
        List<IColumn<AccessCertificationDefinitionType, String>> columns = new ArrayList<>();

        IColumn column;
        column = new LinkColumn<SelectableBean<AccessCertificationDefinitionType>>(createStringResource("PageCertificationTypes.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationDefinitionType>> rowModel) {
                // TODO
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCertificationTypes.table.description"), "value.description");
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportType>>(new Model(), null){

            @Override
            public String getFirstCap(){
                return PageCertificationTypes.this.createStringResource("PageCertificationTypes.button.run").getString();
            }

            @Override
            public String getSecondCap(){
                return PageCertificationTypes.this.createStringResource("PageCertificationTypes.button.configure").getString();
            }

            @Override
            public String getFirstColorCssClass(){
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                //runReportPerformed(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                //configurePerformed(target, model.getObject().getValue());
            }
        };
        columns.add(column);

        return columns;
    }

//    private void runReportPerformed(AjaxRequestTarget target, ReportType report){
//        LOGGER.debug("Run report performed for {}", new Object[]{report.asPrismObject()});
//
//        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
//        try {
//            Task task = createSimpleTask(OPERATION_RUN_REPORT);
//            getReportManager().runReport(report.asPrismObject(), task, result);
//        } catch (Exception ex) {
//            result.recordFatalError(ex);
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//
//        showResult(result);
//        target.add(getFeedbackPanel());
//    }

//    private void configurePerformed(AjaxRequestTarget target, ReportType report){
//        PageParameters params = new PageParameters();
//        params.add(OnePageParameterEncoder.PARAMETER, report.getOid());
//        setResponsePage(PageReport.class, params);
//    }

//    private ObjectDataProvider getDataProvider(){
//        DataTable table = getReportTable().getDataTable();
//        return (ObjectDataProvider) table.getDataProvider();
//    }

//    private TablePanel getReportTable(){
//        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CERTIFICATION_TYPES_TABLE));
//    }

    private ObjectQuery createQuery(){
        ObjectQuery query = new ObjectQuery();
        return query;
    }
}
