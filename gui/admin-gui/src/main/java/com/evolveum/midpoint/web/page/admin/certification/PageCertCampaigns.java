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

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.commons.lang.time.DurationFormatUtils;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certificationCampaigns", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)
        })
public class PageCertCampaigns extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaigns.class);

    private static final String DOT_CLASS = PageCertCampaigns.class.getName() + ".";
    private static final String OPERATION_OPEN_NEXT_STAGE = DOT_CLASS + "openNextStage";
    private static final String OPERATION_CLOSE_STAGE = DOT_CLASS + "closeStage";
    private static final String OPERATION_CLOSE_CAMPAIGN = DOT_CLASS + "closeCampaign";
    private static final String OPERATION_START_REMEDIATION = DOT_CLASS + "startRemediation";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CAMPAIGNS_TABLE = "campaignsTable";
    public static final String OP_START_CAMPAIGN = "PageCertCampaigns.button.startCampaign";
    public static final String OP_CLOSE_CAMPAIGN = "PageCertCampaigns.button.closeCampaign";
    public static final String OP_CLOSE_STAGE = "PageCertCampaigns.button.closeStage";
    public static final String OP_OPEN_NEXT_STAGE = "PageCertCampaigns.button.openNextStage";
    public static final String OP_START_REMEDIATION = "PageCertCampaigns.button.startRemediation";

    public PageCertCampaigns(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                String definitionOid = getDefinitionOid();
                if (definitionOid == null) {
                    return null;
                }
                PrismObject<AccessCertificationDefinitionType> definitionPrismObject =
                        WebModelUtils.loadObject(AccessCertificationDefinitionType.class, definitionOid, new OperationResult("dummy"), PageCertCampaigns.this);
                if (definitionPrismObject == null) {
                    return null;
                }
                return definitionPrismObject.asObjectable().getName().getOrig();
            }
        };
    }


    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ObjectDataProvider provider = new ObjectDataProvider(this, AccessCertificationCampaignType.class);
        provider.setQuery(createQuery());
        Collection<SelectorOptions<GetOperationOptions>> resolveDefinition =
                SelectorOptions.createCollection(AccessCertificationCampaignType.F_DEFINITION_REF,
                        GetOperationOptions.createResolve());
        provider.setOptions(resolveDefinition);
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
                AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                certStatPerformed(target, campaign.getOid());
            }

            private void certStatPerformed(AjaxRequestTarget target, String oid) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                setResponsePage(new PageCertCampaignStatistics(parameters));
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCertCampaigns.table.description"), "value.description");
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("PageCertCampaigns.table.state"), "value.state") {
            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(getPage(), en).getString();
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccessCertificationCampaignType>, String>(createStringResource("PageCertCampaigns.table.stage")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item, String componentId,
                                     final IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                        int currentStage = campaign.getCurrentStageNumber();
                        //int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
                        if (AccessCertificationCampaignStateType.IN_REVIEW_STAGE.equals(campaign.getState()) ||
                                AccessCertificationCampaignStateType.REVIEW_STAGE_DONE.equals(campaign.getState())) {
                            return currentStage;
                        } else {
                            return null;
                        }
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccessCertificationCampaignType>, String>(createStringResource("PageCertCampaigns.table.stages")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item, String componentId,
                                     final IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        AccessCertificationCampaignType campaign = rowModel.getObject().getValue();
                        int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
                        return numOfStages;
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccessCertificationCampaignType>, String>(createStringResource("PageCertCampaigns.table.deadline")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccessCertificationCampaignType>>> item, String componentId,
                                     final IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        return deadline(rowModel);
                    }
                }));
            }
        };
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<AccessCertificationCampaignType>>(new Model(), null) {

            @Override
            public boolean isFirstButtonEnabled(IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getValue();
                String button = determineAction(campaign);
                return button != null;
            }

            @Override
            public String getFirstCap() {
                AccessCertificationCampaignType campaign = getRowModel().getObject().getValue();
                String button = determineAction(campaign);
                if (button != null) {
                    return PageCertCampaigns.this.createStringResource(button).getString();
                } else {
                    return "-";     // TODO make this button invisible
                }
            }

            @Override
            public String getSecondCap() {
                return PageCertCampaigns.this.createStringResource("PageCertCampaigns.button.closeCampaign").getString();
            }

            @Override
            public String getFirstColorCssClass() {
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            @Override
            public String getSecondColorCssClass() {
                return BUTTON_COLOR_CLASS.DANGER.toString();
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                executeCampaignStateOperation(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                closeCampaign(target, model.getObject().getValue());
            }
        };
        columns.add(column);

        return columns;
    }

    protected String determineAction(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getCurrentStageNumber();
        int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
        AccessCertificationCampaignStateType state = campaign.getState();
        String button;
        switch (state) {
            case CREATED:
                button = numOfStages > 0 ? OP_START_CAMPAIGN : null;
                break;
            case IN_REVIEW_STAGE:
                button = OP_CLOSE_STAGE;
                break;
            case REVIEW_STAGE_DONE:
                button = currentStage < numOfStages ? OP_OPEN_NEXT_STAGE : OP_START_REMEDIATION;
                break;
            case IN_REMEDIATION:
            case CLOSED:
            default:
                button = null;
                break;
        }
        return button;
    }

    private String deadline(IModel<SelectableBean<AccessCertificationCampaignType>> campaignModel) {
        AccessCertificationCampaignType campaign = campaignModel.getObject().getValue();
        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        XMLGregorianCalendar end;
        Boolean stageLevelInfo;
        if (campaign.getCurrentStageNumber() == 0) {
            end = campaign.getEnd();
            stageLevelInfo = false;
        } else if (currentStage != null) {
            end = currentStage.getEnd();
            stageLevelInfo = true;
        } else {
            end = null;
            stageLevelInfo = null;
        }

        if (end == null) {
            return "";
        } else {
            long delta = XmlTypeConverter.toMillis(end) - System.currentTimeMillis();

            // round to hours; we always round down
            long precision = 3600000L;      // 1 hour
            if (Math.abs(delta) > precision) {
                delta = (delta / precision) * precision;
            }

            //todo i18n
            if (delta > 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.inForStage" : "PageCertCampaigns.inForCampaign";
                return new StringResourceModel(key, this, null, null,
                        DurationFormatUtils.formatDurationWords(delta, true, true)).getString();
            } else if (delta < 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.agoForStage" : "PageCertCampaigns.agoForCampaign";
                return new StringResourceModel(key, this, null, null,
                        DurationFormatUtils.formatDurationWords(-delta, true, true)).getString();
            } else {
                String key = stageLevelInfo ? "PageCertCampaigns.nowForStage" : "PageCertCampaigns.nowForCampaign";
                return getString(key);
            }
        }
    }

    private void executeCampaignStateOperation(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
        LOGGER.debug("Advance/close certification campaign performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_OPEN_NEXT_STAGE);
        try {
            int currentStage = campaign.getCurrentStageNumber();
            CertificationManager cm = getCertificationManager();
            String action = determineAction(campaign);
            Task task;
            switch (action) {
                case OP_START_CAMPAIGN:
                case OP_OPEN_NEXT_STAGE:
                    task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
                    cm.openNextStage(campaign.getOid(), currentStage + 1, task, result);
                    break;
                case OP_CLOSE_STAGE:
                    task = createSimpleTask(OPERATION_CLOSE_STAGE);
                    cm.closeCurrentStage(campaign.getOid(), currentStage, task, result);
                    break;
                case OP_START_REMEDIATION:
                    task = createSimpleTask(OPERATION_START_REMEDIATION);
                    cm.startRemediation(campaign.getOid(), task, result);
                    break;
                case OP_CLOSE_CAMPAIGN:     // not used
                    task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
                    cm.closeCampaign(campaign.getOid(), task, result);
                    break;
                default:
                    throw new IllegalStateException("Unknown action: " + action);
            }
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void closeCampaign(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
        LOGGER.debug("Close certification campaign performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_CLOSE_CAMPAIGN);
        try {
            CertificationManager cm = getCertificationManager();
            Task task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
            cm.closeCampaign(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private TablePanel getCampaignsTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CAMPAIGNS_TABLE));
    }


    private ObjectQuery createQuery() {
        // TODO filtering based on e.g. campaign state/stage (not started, active, finished)
        ObjectQuery query = new ObjectQuery();
        String definitionOid = getDefinitionOid();
        if (definitionOid != null) {
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(definitionOid, ObjectTypes.ACCESS_CERTIFICATION_DEFINITION);
            ObjectFilter filter = null;
            try {
                filter = RefFilter.createReferenceEqual(new ItemPath(AccessCertificationCampaignType.F_DEFINITION_REF),
                        AccessCertificationCampaignType.class, getPrismContext(), ref.asReferenceValue());
            } catch (SchemaException e) {
                throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
            }
            query = ObjectQuery.createObjectQuery(filter);
        }
        return query;
    }

    private String getDefinitionOid() {
        StringValue definitionOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return definitionOid != null ? definitionOid.toString() : null;
    }
}
