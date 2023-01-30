/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.ButtonColorClass;
import com.evolveum.midpoint.web.component.data.provider.CertWorkItemDtoProvider;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.*;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.OBJECT;
import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.TARGET;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/decisions", matchUrlForSecurity = "/admin/certification/decisions")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS,
                        label = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_MY_CERTIFICATION_DECISIONS_DESCRIPTION)})

public class PageCertDecisions extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertDecisions.class);

    private static final String DOT_CLASS = PageCertDecisions.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DECISIONS_TABLE = "decisionsTable";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SHOW_NOT_DECIDED_ONLY = "showNotDecidedOnly";
    private static final String ID_TABLE_HEADER = "tableHeader";

    private CertDecisionHelper helper = new CertDecisionHelper();

    boolean isDisplayingAllItems() {
        return false;
    }

    public PageCertDecisions() {
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    //region Data
    private CertWorkItemDtoProvider createProvider() {
        CertWorkItemDtoProvider provider = new CertWorkItemDtoProvider(PageCertDecisions.this);
        provider.setQuery(createCaseQuery());
        provider.setCampaignQuery(createCampaignQuery());
        provider.setReviewerOid(getCurrentUserOid());
        provider.setNotDecidedOnly(getCertDecisionsStorage().getShowNotDecidedOnly());
        provider.setAllItems(isDisplayingAllItems());
        provider.setSort(SearchingUtils.CURRENT_REVIEW_DEADLINE, SortOrder.ASCENDING);        // default sorting
        return provider;
    }

    private ObjectQuery createCaseQuery() {
        return getPrismContext().queryFactory().createQuery();
    }

    private ObjectQuery createCampaignQuery() {
        return getPrismContext().queryFactory().createQuery();
    }

    private String getCurrentUserOid() {
        try {
            return getSecurityContextManager().getPrincipal().getOid();
        } catch (SecurityViolationException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't get currently logged user OID", e);
        }
    }
    //endregion

    //region Layout
    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);
        CertWorkItemDtoProvider provider = createProvider();
        BoxedTablePanel<CertWorkItemDto> table = new BoxedTablePanel<CertWorkItemDto>(ID_DECISIONS_TABLE, provider, initColumns(),
                UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCertDecisions.this,
                        Model.of(getCertDecisionsStorage().getShowNotDecidedOnly()));
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        // adding this on outer feedback panel prevents displaying the error messages
        //addVisibleOnWarningBehavior(getMainFeedbackPanel());
        //addVisibleOnWarningBehavior(getTempFeedbackPanel());
    }

//    private void addVisibleOnWarningBehavior(Component c) {
//        c.add(new VisibleEnableBehaviour() {
//            @Override
//            public boolean isVisible() {
//                return PageCertDecisions.this.getFeedbackMessages().hasMessage(FeedbackMessage.WARNING);
//            }
//        });
//    }

    private List<IColumn<CertWorkItemDto, String>> initColumns() {
        List<IColumn<CertWorkItemDto, String>> columns = new ArrayList<>();

        IColumn<CertWorkItemDto, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = helper.createTypeColumn(OBJECT, this);
        columns.add(column);

        column = helper.createObjectNameColumn(this, "PageCertDecisions.table.objectName");
        columns.add(column);

        column = helper.createTypeColumn(TARGET, this);
        columns.add(column);

        column = helper.createTargetNameColumn(this, "PageCertDecisions.table.targetName");
        columns.add(column);

        if (isDisplayingAllItems()) {
            column = helper.createReviewerNameColumn(this, "PageCertDecisions.table.reviewer");
            columns.add(column);
        }

        column = helper.createDetailedInfoColumn(this);
        columns.add(column);

        column = helper.createConflictingNameColumn(this, "PageCertDecisions.table.conflictingTargetName");
        columns.add(column);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGN_URL)) {

            column = new AjaxLinkColumn<CertWorkItemDto>(
                    createStringResource("PageCertDecisions.table.campaignName"),
                    SearchingUtils.CAMPAIGN_NAME, CertWorkItemDto.F_CAMPAIGN_NAME) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, IModel<CertWorkItemDto> rowModel) {
                    super.populateItem(item, componentId, rowModel);
                    AccessCertificationCampaignType campaign = rowModel.getObject().getCampaign();
                    if (campaign != null && campaign.getDescription() != null) {
                        item.add(AttributeModifier.replace("title", campaign.getDescription()));
                        item.add(new TooltipBehavior());
                    }
                }

                @Override
                public void onClick(AjaxRequestTarget target, IModel<CertWorkItemDto> rowModel) {
                    CertWorkItemDto dto = rowModel.getObject();
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, dto.getCampaignRef().getOid());
                    navigateToNext(PageCertCampaign.class, parameters);
                }
            };
        } else {
            column = new AbstractColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.campaignName"), SearchingUtils.CAMPAIGN_NAME) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId,
                        final IModel<CertWorkItemDto> rowModel) {
                    item.add(new Label(componentId, new IModel<Object>() {
                        @Override
                        public Object getObject() {
                            return rowModel.getObject().getCampaignName();
                        }
                    }));
                }
            };
        }
        columns.add(column);

        column = new PropertyColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.iteration"),
                CertCaseOrWorkItemDto.F_ITERATION) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "countLabel";
            }

        };
        columns.add(column);

        column = new AbstractColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.campaignStage")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        CertWorkItemDto dto = rowModel.getObject();
                        return dto.getCampaignStageNumber() + "/" + dto.getCampaignStageCount();
                    }
                }));
                String stageName = rowModel.getObject().getCurrentStageName();
                if (stageName != null) {
                    item.add(AttributeModifier.replace("title", stageName));
                    item.add(new TooltipBehavior());
                }
            }

            @Override
            public String getCssClass() {
                return "countLabel";
            }
        };
        columns.add(column);

        column = new AbstractColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.escalation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        CertWorkItemDto dto = rowModel.getObject();
                        Integer n = dto.getEscalationLevelNumber();
                        return n != null ? String.valueOf(n) : null;
                    }
                }));
                String info = rowModel.getObject().getEscalationLevelInfo();
                if (info != null) {
                    item.add(AttributeModifier.replace("title", info));
                    item.add(new TooltipBehavior());
                }
            }

            @Override
            public String getCssClass() {
                return "countLabel";
            }
        };
        columns.add(column);

        column = new PropertyColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.requested"),
                SearchingUtils.CURRENT_REVIEW_REQUESTED_TIMESTAMP, CertWorkItemDto.F_REVIEW_REQUESTED) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, IModel<CertWorkItemDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                CertWorkItemDto dto = rowModel.getObject();
                Date started = dto.getStageStarted();
                if (started != null) {
                    item.add(AttributeModifier.replace("title", WebComponentUtil.getShortDateTimeFormattedValue(started, PageCertDecisions.this)));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

            column = new PropertyColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.deadline"),
                SearchingUtils.CURRENT_REVIEW_DEADLINE, CertWorkItemDto.F_DEADLINE_AS_STRING) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                    super.populateItem(item, componentId, rowModel);
                    XMLGregorianCalendar deadline = rowModel.getObject().getCertCase().getCurrentStageDeadline();
                    if (deadline != null) {
                        item.add(AttributeModifier.replace("title", WebComponentUtil.formatDate(deadline)));
                        item.add(new TooltipBehavior());
                    }
                }
        };
        columns.add(column);

        final AvailableResponses availableResponses = new AvailableResponses(this);
        final int responses = availableResponses.getResponseKeys().size();

        column = new AbstractColumn<CertWorkItemDto, String>(new Model<>()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> cellItem, String componentId,
                                     IModel<CertWorkItemDto> rowModel) {

                cellItem.add(new MultiButtonPanel<CertWorkItemDto>(componentId, rowModel, responses + 1) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component createButton(int index, String componentId, IModel<CertWorkItemDto> model) {
                        AjaxIconButton btn;
                        if (index < responses) {
                            btn = buildDefaultButton(componentId, null, new Model(availableResponses.getTitle(index)),
                                    new Model<>("btn btn-sm " + getDecisionButtonColor(model, availableResponses.getResponseValues().get(index))),
                                    target ->
                                            recordActionPerformed(target, model.getObject(), availableResponses.getResponseValues().get(index)));
                            btn.add(new EnableBehaviour(() -> !decisionEquals(model, availableResponses.getResponseValues().get(index))));
                        } else {
                            btn = buildDefaultButton(componentId, null, new Model(availableResponses.getTitle(index)),
                                    new Model<>("btn btn-sm " + ButtonColorClass.DANGER), null);
                            btn.setEnabled(false);
                            btn.add(new VisibleBehaviour(() -> !availableResponses.isAvailable(model.getObject().getResponse())));
                        }

                        return btn;
                    }
                });
            }
        };
        columns.add(column);

        column = new DirectlyEditablePropertyColumn<CertWorkItemDto>(
                createStringResource("PageCertDecisions.table.comment"),
                CertWorkItemDto.F_COMMENT) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onBlur(AjaxRequestTarget target, IModel model) {
                // TODO determine somehow if the model.comment was really changed
                recordActionPerformed(target, (CertWorkItemDto) model.getObject(), null);
            }
        };
        columns.add(column);

        columns.add(new InlineMenuHeaderColumn(createInlineMenu(availableResponses)));

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu(AvailableResponses availableResponses) {
        List<InlineMenuItem> items = new ArrayList<>();
        if (availableResponses.isAvailable(ACCEPT)) {
            items.add(createMenu("PageCertDecisions.menu.acceptSelected", ACCEPT));
        }
        if (availableResponses.isAvailable(REVOKE)) {
            items.add(createMenu("PageCertDecisions.menu.revokeSelected", REVOKE));
        }
        if (availableResponses.isAvailable(REDUCE)) {
            items.add(createMenu("PageCertDecisions.menu.reduceSelected", REDUCE));
        }
        if (availableResponses.isAvailable(NOT_DECIDED)) {
            items.add(createMenu("PageCertDecisions.menu.notDecidedSelected", NOT_DECIDED));
        }
        if (availableResponses.isAvailable(NO_RESPONSE)) {
            items.add(createMenu("PageCertDecisions.menu.noResponseSelected", NO_RESPONSE));
        }
        return items;
    }

    private InlineMenuItem createMenu(String titleKey, final AccessCertificationResponseType response) {
        return new InlineMenuItem(createStringResource(titleKey)) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertDecisions.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recordActionOnSelected(response, target);
                    }
                };
            }
        };
    }

    private String getDecisionButtonColor(IModel<CertWorkItemDto> model, AccessCertificationResponseType response) {
        if (decisionEquals(model, response)) {
            return ButtonColorClass.PRIMARY.toString();
        } else {
            return ButtonColorClass.DEFAULT.toString();
        }
    }

    private boolean decisionEquals(IModel<CertWorkItemDto> model, AccessCertificationResponseType response) {
        return model.getObject().getResponse() == response;
    }

    private Table getDecisionsTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_DECISIONS_TABLE));
    }
    //endregion

    //region Actions

    private void recordActionOnSelected(AccessCertificationResponseType response, AjaxRequestTarget target) {
        List<CertWorkItemDto> workItemDtoList = WebComponentUtil.getSelectedData(getDecisionsTable());
        if (workItemDtoList.isEmpty()) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        for (CertWorkItemDto workItemDto : workItemDtoList) {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            try {
                getCertificationService().recordDecision(
                        workItemDto.getCampaignRef().getOid(),
                        workItemDto.getCaseId(), workItemDto.getWorkItemId(),
                        response, workItemDto.getComment(), task, resultOne);
            } catch (Exception ex) {
                resultOne.recordFatalError(ex);
            } finally {
                resultOne.computeStatusIfUnknown();
            }
        }
        result.computeStatus();

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(getFeedbackPanel());
        target.add((Component) getDecisionsTable());
    }

    // if response is null this means keep the current one in workItemDto
    private void recordActionPerformed(AjaxRequestTarget target, CertWorkItemDto workItemDto, AccessCertificationResponseType response) {
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
        try {
            Task task = createSimpleTask(OPERATION_RECORD_ACTION);
            if (response == null) {
                response = workItemDto.getResponse();
            }
            // TODO work item ID
            getCertificationService().recordDecision(
                    workItemDto.getCampaignRef().getOid(),
                    workItemDto.getCaseId(), workItemDto.getWorkItemId(),
                    response, workItemDto.getComment(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            showResult(result);
        }
//        resetCertWorkItemCountModel();
        target.add(this);
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createCaseQuery();

        Table panel = getDecisionsTable();
        DataTable table = panel.getDataTable();
        CertWorkItemDtoProvider provider = (CertWorkItemDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        provider.setNotDecidedOnly(getCertDecisionsStorage().getShowNotDecidedOnly());
        provider.setAllItems(isDisplayingAllItems());
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add((Component) getDecisionsTable());
    }

    private CertDecisionsStorage getCertDecisionsStorage(){
        return getSessionStorage().getCertDecisions();
    }

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<Boolean> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new MidpointForm(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<Boolean> model = (IModel<Boolean>) getDefaultModel();

            CheckBox showNotDecidedOnlyBox = new CheckBox(ID_SHOW_NOT_DECIDED_ONLY, model);
            showNotDecidedOnlyBox.add(createFilterAjaxBehaviour());
            searchForm.add(showNotDecidedOnlyBox);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageCertDecisions page = (PageCertDecisions) getPage();
                    page.getCertDecisionsStorage().setShowNotDecidedOnly((Boolean) getDefaultModelObject());
                    page.searchFilterPerformed(target);

                }
            };
        }
    }
}
