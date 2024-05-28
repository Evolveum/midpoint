/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

public class CertificationItemsPanel extends ContainerableListPanel<AccessCertificationWorkItemType,
        PrismContainerValueWrapper<AccessCertificationWorkItemType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CertificationItemsPanel.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    public CertificationItemsPanel(String id) {
        super(id, AccessCertificationWorkItemType.class);
    }

    public CertificationItemsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationWorkItemType.class, configurationType);
    }

    public CertificationItemsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationWorkItemType.class, configurationType);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createDefaultColumns() {
        return createColumns();
    }

    private List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createColumns() {
        return ColumnUtils.getDefaultCertWorkItemColumns();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return createRowActions();
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<AccessCertificationWorkItemType>> createProvider() {
        return CertificationItemsPanel.this.createProvider(getSearchModel());
    }

    @Override
    public List<AccessCertificationWorkItemType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(PrismValueWrapper::getRealValue).collect(Collectors.toList());
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCheckboxColumn() {
        if (!isPreview()) {
            return new CheckBoxHeaderColumn<>();
        }
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createIconColumn() {
        return new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(
                        IconAndStylesUtil.createDefaultBlackIcon(AccessCertificationWorkItemType.COMPLEX_TYPE));
            }

        };
    }


    @Override
    protected String getStorageKey() {
        return SessionStorage.KEY_WORK_ITEMS;
    }

    private ContainerListDataProvider<AccessCertificationWorkItemType> createProvider(IModel<Search<AccessCertificationWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CertificationItemsPanel.this.getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getCertDecisions();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getOpenCertWorkItemsQuery(true);
            }

        };
//        provider.setSort(CaseWorkItemType.F_DEADLINE.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    private List<InlineMenuItem> createRowActions() {
        final AvailableResponses availableResponses = new AvailableResponses(getPageBase());
        List<InlineMenuItem> items = new ArrayList<>();
        int buttonsCount = 0;
        if (availableResponses.isAvailable(ACCEPT)) {
            items.add(createResponseMenu(buttonsCount, ACCEPT));
            buttonsCount++;
        }
        if (availableResponses.isAvailable(REVOKE)) {
            items.add(createResponseMenu(buttonsCount, REVOKE));
            buttonsCount++;
        }
        if (availableResponses.isAvailable(REDUCE)) {
            items.add(createResponseMenu(buttonsCount, REDUCE));
            buttonsCount++;
        }
        if (availableResponses.isAvailable(NOT_DECIDED)) {
            items.add(createResponseMenu(buttonsCount, NOT_DECIDED));
            buttonsCount++;
        }
        if (availableResponses.isAvailable(NO_RESPONSE)) {
            items.add(createResponseMenu(buttonsCount, NO_RESPONSE));
        }
//        addCommentWorkItemAction(items); //todo is it possible just to add comment without any response?
//        addForwardWorkItemAction(items);
        return items;
    }

    private InlineMenuItem createResponseMenu(int buttonsCount, final AccessCertificationResponseType response) {
        CertificationItemResponseHelper helper = new CertificationItemResponseHelper(response);
        if (buttonsCount < 2) {
            return new ButtonInlineMenuItem(createStringResource(helper.getLabelKey())) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiDisplayTypeUtil.getIconCssClass(helper.getResponseDisplayType()));
                }

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PrismContainerValueWrapper<AccessCertificationWorkItemType> wi =
                                    (PrismContainerValueWrapper<AccessCertificationWorkItemType>) getRowModel().getObject();
                            confirmAction(response, Collections.singletonList(wi.getRealValue()), target);
                        }
                    };
                }
            };
        } else {
            return new InlineMenuItem(createStringResource(helper.getLabelKey())) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PrismContainerValueWrapper<AccessCertificationWorkItemType> wi =
                                    (PrismContainerValueWrapper<AccessCertificationWorkItemType>) getRowModel().getObject();
                            confirmAction(response, Collections.singletonList(wi.getRealValue()), target);
                        }
                    };
                }
            };
        }
    }

    private void confirmAction(AccessCertificationResponseType response, List<AccessCertificationWorkItemType> items,
            AjaxRequestTarget target) {
        ConfirmationPanelWithComment confirmationPanel = new ConfirmationPanelWithComment(getPageBase().getMainPopupBodyId(),
                createStringResource("ResponseConfirmationPanel.confirmation", LocalizationUtil.translateEnum(response))) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void yesPerformedWithComment(AjaxRequestTarget target, String comment) {
                recordActionOnSelected(response, items, comment, target);
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    private void recordActionOnSelected(AccessCertificationResponseType response, List<AccessCertificationWorkItemType> items,
            String comment, AjaxRequestTarget target) {
        if (CollectionUtils.isEmpty(items)) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = getPageBase().createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        items.stream().forEach(item -> {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            try {
                AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(item);
                AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
                getPageBase().getCertificationService().recordDecision(
                        campaign.getOid(),
                        certCase.getId(), item.getId(),
                        response, comment, task, resultOne);
            } catch (Exception ex) {
                resultOne.recordFatalError(ex);
            } finally {
                resultOne.computeStatusIfUnknown();
            }
        });
        result.computeStatus();

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(getFeedbackPanel());
        refreshTable(target);
    }

    private void addCommentWorkItemAction(List<InlineMenuItem> items) {
        items.add(new InlineMenuItem(createStringResource("CertificationItemsPanel.action.comment")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<AccessCertificationWorkItemType> wi =
                                (PrismContainerValueWrapper<AccessCertificationWorkItemType>) getRowModel().getObject();

                        CommentPanel commentPanel = new CommentPanel(getPageBase().getMainPopupBodyId(),
                                Model.of(WorkItemTypeUtil.getComment(wi.getRealValue()))) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void savePerformed(AjaxRequestTarget target, String comment) {
                                String outcome = WorkItemTypeUtil.getOutcome(wi.getRealValue());
                                AccessCertificationResponseType response;
                                if (outcome == null) {
                                    response = NO_RESPONSE;
                                } else {
                                    response = AccessCertificationResponseType.fromValue(outcome);
                                }
                                recordActionOnSelected(response, Collections.singletonList(wi.getRealValue()), comment, target);
                            }
                        };
                        getPageBase().showMainPopup(commentPanel, target);
                    }
                };
            }
        });
    }

    protected ObjectQuery getOpenCertWorkItemsQuery(boolean notDecidedOnly) {
        String campaignOid = getCampaignOid();
        ObjectQuery query;
        if (StringUtils.isNotEmpty(campaignOid)) {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .ownedBy(AccessCertificationCaseType.class, AccessCertificationCaseType.F_WORK_ITEM)
                    .id(campaignOid)
                    .build();
        } else {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .build();
        }
        MidPointPrincipal principal = null;
        if (isMyCertItems()) {
            principal = getPageBase().getPrincipal();
        }
        return QueryUtils.createQueryForOpenWorkItems(query, principal, notDecidedOnly);
    }

    protected String getCampaignOid() {
        return null;
    }

    protected boolean isMyCertItems() {
        return false;
    }

}
