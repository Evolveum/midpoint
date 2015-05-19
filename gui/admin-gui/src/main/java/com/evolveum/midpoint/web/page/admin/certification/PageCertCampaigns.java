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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certificationCampaigns", action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)
        })
public class PageCertCampaigns extends PageAdminWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaigns.class);

    private static final String DOT_CLASS = PageCertCampaigns.class.getName() + ".";
    private static final String OPERATION_START_CAMPAIGN = DOT_CLASS + "startCertificationCampaign";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CAMPAIGNS_TABLE = "campaignsTable";

    public PageCertCampaigns() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ObjectDataProvider provider = new ObjectDataProvider(PageCertCampaigns.this, AccessCertificationCampaignType.class);
        provider.setQuery(createQuery());
        TablePanel table = new TablePanel<>(ID_CAMPAIGNS_TABLE, provider, initColumns());
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<IColumn<AccessCertificationCampaignType, String>> initColumns() {
        List<IColumn<AccessCertificationCampaignType, String>> columns = new ArrayList<>();

        IColumn column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<AccessCertificationCampaignType>>(createStringResource("PageCertCampaigns.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                // TODO show campaign details (or cases?)
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCertCampaigns.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccessCertificationCampaignType>, String>(createStringResource("PageCertCampaigns.table.stage")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item, String componentId,
                                     final IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        return rowModel.getObject().getValue().getCurrentStageNumber();
                        // TODO provide also the total number of stages (taken from campaign definition)
                    }
                }));
            }
        };
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<AccessCertificationCampaignType>>(new Model(), null) {

            @Override
            public String getFirstCap() {
                return PageCertCampaigns.this.createStringResource("PageCertCampaigns.button.start").getString();
            }

            @Override
            public String getSecondCap() {
                return PageCertCampaigns.this.createStringResource("PageCertCampaigns.button.configure").getString();
            }

            @Override
            public String getFirstColorCssClass() {
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            // TODO enable this button only if there is a stage to advance to
            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                startCampaignPerformed(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                // TODO
                //configurePerformed(target, model.getObject().getValue());
            }
        };
        columns.add(column);

        return columns;
    }

    private void startCampaignPerformed(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
        LOGGER.debug("Start certification campaign performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_START_CAMPAIGN);
        try {
            Task task = createSimpleTask(OPERATION_START_CAMPAIGN);
            getCertificationManager().nextStage(campaign, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private TablePanel getCampaignsTable(){
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CAMPAIGNS_TABLE));
    }


    private ObjectQuery createQuery() {
        // TODO filtering based on e.g. campaign state/stage (not started, active, finished)
        ObjectQuery query = new ObjectQuery();
        return query;
    }
}
