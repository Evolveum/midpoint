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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.BUTTON_COLOR_CLASS;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDecisionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDecisionDtoProvider;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
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
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/decisions",
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)})

public class PageCertDecisions extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager
            .getTrace(PageCertDecisions.class);

    private static final String DOT_CLASS = PageCertDecisions.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DECISIONS_TABLE = "decisionsTable";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SHOW_NOT_DECIDED_ONLY = "showNotDecidedOnly";
    private static final String ID_TABLE_HEADER = "tableHeader";

    CertDecisionHelper helper = new CertDecisionHelper();

    private IModel<Boolean> showNotDecidedOnlyModel = new Model<>(false);

    public PageCertDecisions() {
        initLayout();
    }

    //region Data
    private CertDecisionDtoProvider createProvider() {
        CertDecisionDtoProvider provider = new CertDecisionDtoProvider(PageCertDecisions.this);
        provider.setQuery(createCaseQuery());
        provider.setCampaignQuery(createCampaignQuery());
        provider.setReviewerOid(getCurrentUserOid());
        provider.setSort(AccessCertificationCaseType.F_REVIEW_DEADLINE.getLocalPart(), SortOrder.ASCENDING);        // default sorting
        return provider;
    }

    private ObjectQuery createCaseQuery() {
        ObjectQuery query = new ObjectQuery();

        // implemented as a special notDecidedOnly parameter
//		Boolean notDecidedOnly = showNotDecidedOnlyModel.getObject();
//		if (Boolean.TRUE.equals(notDecidedOnly)) {
//
//			PrismContainerDefinition pcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AccessCertificationCaseType.class);
//
//			ObjectFilter filter;
//			try {
//				filter = OrFilter.createOr(
//						EqualFilter.createEqual(new ItemPath(AccessCertificationCaseType.F_DECISION,
//								AccessCertificationDecisionType.F_RESPONSE), pcd, null),
//						EqualFilter.createEqual(new ItemPath(AccessCertificationCaseType.F_DECISION,
//								AccessCertificationDecisionType.F_RESPONSE), pcd, AccessCertificationResponseType.NO_RESPONSE));
//			} catch (SchemaException e) {
//				throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
//			}
//
//			query.setFilter(filter);
//		}
        return query;
    }

    private ObjectQuery createCampaignQuery() {
        ObjectQuery query = new ObjectQuery();
        return query;
    }

    private String getCurrentUserOid() {
        try {
            return getSecurityEnforcer().getPrincipal().getOid();
        } catch (SecurityViolationException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't get currently logged user OID",
                    e);
        }
    }
    //endregion

    //region Layout
    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);
        CertDecisionDtoProvider provider = createProvider();
        BoxedTablePanel table = new BoxedTablePanel(ID_DECISIONS_TABLE, provider, initColumns()) {

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCertDecisions.this, showNotDecidedOnlyModel);
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        // adding this on outer feedback panel prevents displaying the error messages
        //addVisibleOnWarningBehavior(getMainFeedbackPanel());
        //addVisibleOnWarningBehavior(getTempFeedbackPanel());
    }

//	private void addVisibleOnWarningBehavior(Component c) {
//		c.add(new VisibleEnableBehaviour() {
//			@Override
//			public boolean isVisible() {
//				return PageCertDecisions.this.getFeedbackMessages().hasMessage(FeedbackMessage.WARNING);
//			}
//		});
//	}

    private List<IColumn<CertDecisionDto, String>> initColumns() {
        List<IColumn<CertDecisionDto, String>> columns = new ArrayList<>();

        IColumn column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = helper.createObjectNameColumn(this, "PageCertDecisions.table.objectName");
        columns.add(column);

        column = helper.createTargetTypeColumn(this);
        columns.add(column);

        column = helper.createTargetNameColumn(this, "PageCertDecisions.table.targetName");
        columns.add(column);

        column = helper.createDetailedInfoColumn(this);
        columns.add(column);

        column = new LinkColumn<CertDecisionDto>(
                createStringResource("PageCertDecisions.table.campaignName"),
                AccessCertificationCaseType.F_CAMPAIGN_REF.getLocalPart(), CertDecisionDto.F_CAMPAIGN_NAME) {
            @Override
            public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, IModel<CertDecisionDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                AccessCertificationCampaignType campaign = rowModel.getObject().getCampaign();
                if (campaign != null && campaign.getDescription() != null) {
                    item.add(AttributeModifier.replace("title", campaign.getDescription()));
                    item.add(new TooltipBehavior());
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertDecisionDto> rowModel) {
                CertDecisionDto dto = rowModel.getObject();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, dto.getCampaignRef().getOid());
                setResponsePage(new PageCertCampaign(parameters, PageCertDecisions.this));
            }
        };
        columns.add(column);

        column = new AbstractColumn<CertDecisionDto, String>(
                createStringResource("PageCertDecisions.table.campaignStage")) {
            @Override
            public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, final IModel<CertDecisionDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        CertDecisionDto dto = rowModel.getObject();
                        return dto.getCampaignStageNumber() + "/" + dto.getCampaignStageCount();
                    }
                }));
                String stageName = rowModel.getObject().getCurrentStageName();
                if (stageName != null) {
                    item.add(AttributeModifier.replace("title", stageName));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

        column = new PropertyColumn<CertDecisionDto, String>(
                createStringResource("PageCertDecisions.table.requested"),
                AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP.getLocalPart(),
                CertDecisionDto.F_REVIEW_REQUESTED) {
            @Override
            public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, IModel<CertDecisionDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                CertDecisionDto dto = rowModel.getObject();
                Date started = dto.getStageStarted();
                if (started != null) {
                    item.add(AttributeModifier.replace("title", WebMiscUtil.formatDate(started)));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

        column = new PropertyColumn<CertDecisionDto, String>(createStringResource("PageCertDecisions.table.deadline"),
                AccessCertificationCaseType.F_REVIEW_DEADLINE.getLocalPart(), CertDecisionDto.F_DEADLINE_AS_STRING) {
            @Override
            public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, final IModel<CertDecisionDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                XMLGregorianCalendar deadline = rowModel.getObject().getCertCase().getReviewDeadline();
                if (deadline != null) {
                    item.add(AttributeModifier.replace("title", WebMiscUtil.formatDate(deadline)));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

        column = new MultiButtonColumn<CertDecisionDto>(new Model(), 6) {

            private final String[] captionKeys = {
                    "PageCertDecisions.menu.accept",
                    "PageCertDecisions.menu.revoke",
                    "PageCertDecisions.menu.reduce",
                    "PageCertDecisions.menu.notDecided",
                    "PageCertDecisions.menu.delegate",
                    "PageCertDecisions.menu.noResponse"
            };

            private final AccessCertificationResponseType[] responses = {
                    ACCEPT, REVOKE, REDUCE, NOT_DECIDED, DELEGATE, NO_RESPONSE
            };

            @Override
            public String getCaption(int id) {
                return PageCertDecisions.this.createStringResource(captionKeys[id]).getString();
            }

            @Override
            public boolean isButtonEnabled(int id, IModel<CertDecisionDto> model) {
                return !decisionEquals(model, responses[id]);
            }

            @Override
            public String getButtonColorCssClass(int id) {
                return getDecisionButtonColor(getRowModel(), responses[id]);
            }

            @Override
            public void clickPerformed(int id, AjaxRequestTarget target,
                                       IModel<CertDecisionDto> model) {
                recordActionPerformed(target, model.getObject(), responses[id]);
            }

        };
        columns.add(column);

        column = new DirectlyEditablePropertyColumn(
                createStringResource("PageCertDecisions.table.comment"),
                CertDecisionDto.F_COMMENT) {
            @Override
            public void onBlur(AjaxRequestTarget target, IModel model) {
                // TODO determine somehow if the model.comment was really changed
                recordActionPerformed(target, (CertDecisionDto) model.getObject(), null);
            }
        };
        columns.add(column);

        columns.add(new InlineMenuHeaderColumn(createInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createMenu("PageCertDecisions.menu.acceptSelected", ACCEPT));
        items.add(createMenu("PageCertDecisions.menu.revokeSelected", REVOKE));
        items.add(createMenu("PageCertDecisions.menu.reduceSelected", REDUCE));
        items.add(createMenu("PageCertDecisions.menu.notDecidedSelected", NOT_DECIDED));
        items.add(createMenu("PageCertDecisions.menu.delegateSelected", DELEGATE));
        items.add(createMenu("PageCertDecisions.menu.noResponseSelected", NO_RESPONSE));
        return items;
    }

    private InlineMenuItem createMenu(String titleKey, final AccessCertificationResponseType response) {
        return new InlineMenuItem(createStringResource(titleKey), false,
                new HeaderMenuAction(this) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recordActionOnSelected(response, target);
                    }
                });
    }

    private String getDecisionButtonColor(IModel<CertDecisionDto> model, AccessCertificationResponseType response) {
        if (decisionEquals(model, response)) {
            return BUTTON_COLOR_CLASS.PRIMARY.toString();
        } else {
            return BUTTON_COLOR_CLASS.DEFAULT.toString();
        }
    }

    private boolean decisionEquals(IModel<CertDecisionDto> model, AccessCertificationResponseType response) {
        return model.getObject().getResponse() == response;
    }

    private Table getDecisionsTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_DECISIONS_TABLE));
    }
    //endregion

    //region Actions

    private void recordActionOnSelected(AccessCertificationResponseType response, AjaxRequestTarget target) {
        List<CertDecisionDto> certDecisionDtoList = WebMiscUtil.getSelectedData(getDecisionsTable());
        if (certDecisionDtoList.isEmpty()) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        PrismContext prismContext = getPrismContext();

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        for (CertDecisionDto certDecisionDto : certDecisionDtoList) {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            AccessCertificationDecisionType newDecision = new AccessCertificationDecisionType(prismContext);
            newDecision.setResponse(response);
            newDecision.setStageNumber(0);
            newDecision.setComment(certDecisionDto.getComment());
            try {
                getCertificationManager().recordDecision(
                        certDecisionDto.getCampaignRef().getOid(),
                        certDecisionDto.getCaseId(), newDecision, task, resultOne);
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

    // if response is null this means keep the current one in decisionDto
    private void recordActionPerformed(AjaxRequestTarget target,
                                       CertDecisionDto decisionDto, AccessCertificationResponseType response) {
        PrismContext prismContext = getPrismContext();
        AccessCertificationDecisionType newDecision = new AccessCertificationDecisionType(prismContext);
        if (response != null) {
            newDecision.setResponse(response);
        } else {
            newDecision.setResponse(decisionDto.getResponse());
        }
        newDecision.setStageNumber(0);
        newDecision.setComment(decisionDto.getComment());
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
        try {
            Task task = createSimpleTask(OPERATION_RECORD_ACTION);
            getCertificationManager().recordDecision(
                    decisionDto.getCampaignRef().getOid(),
                    decisionDto.getCaseId(), newDecision, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(getFeedbackPanel());
        target.add((Component) getDecisionsTable());
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createCaseQuery();

        Table panel = getDecisionsTable();
        DataTable table = panel.getDataTable();
        CertDecisionDtoProvider provider = (CertDecisionDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        provider.setNotDecidedOnly(Boolean.TRUE.equals(showNotDecidedOnlyModel.getObject()));
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add((Component) getDecisionsTable());
    }


    //endregion

//	protected void dispatchToObjectDetailsPage(ObjectReferenceType objectRef) {
//		if (objectRef == null) {
//			return;		// should not occur
//		}
//		QName type = objectRef.getType();
//		PageParameters parameters = new PageParameters();
//		parameters.add(OnePageParameterEncoder.PARAMETER, objectRef.getOid());
//		if (RoleType.COMPLEX_TYPE.equals(type)) {
//            setResponsePage(new PageRole(parameters, this));
//        } else if (OrgType.COMPLEX_TYPE.equals(type)) {
//            setResponsePage(new PageOrgUnit(parameters, this));
//        } else if (UserType.COMPLEX_TYPE.equals(type)) {
//            setResponsePage(new PageUser(parameters, this));
//        } else if (ResourceType.COMPLEX_TYPE.equals(type)) {
//			setResponsePage(new PageResource(parameters, this));
//		} else {
//            // nothing to do
//        }
//	}

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<Boolean> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<Boolean> model = (IModel) getDefaultModel();

            CheckBox showNotDecidedOnlyBox = new CheckBox(ID_SHOW_NOT_DECIDED_ONLY, model);
            showNotDecidedOnlyBox.add(createFilterAjaxBehaviour());
            searchForm.add(showNotDecidedOnlyBox);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageCertDecisions page = (PageCertDecisions) getPage();
                    page.searchFilterPerformed(target);
                }
            };
        }
    }
}
