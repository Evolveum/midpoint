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
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.action.*;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.certification.CertMiscUtil;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

public class CertificationItemsPanel extends ContainerableListPanel<AccessCertificationWorkItemType,
        PrismContainerValueWrapper<AccessCertificationWorkItemType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CertificationItemsPanel.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    private String campaignOid;

    public CertificationItemsPanel(String id, String campaignOid) {
        super(id, AccessCertificationWorkItemType.class);
        this.campaignOid = campaignOid;
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
        List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> columns =
                ColumnUtils.getDefaultCertWorkItemColumns(!isMyCertItems(), showOnlyNotDecidedItems());
        List<AbstractGuiAction<AccessCertificationWorkItemType>> actions = getCertItemActions();

        columns.add(new GuiActionColumn<>(actions, getPageBase()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationWorkItemType unwrapRowModelObject(
                    PrismContainerValueWrapper<AccessCertificationWorkItemType> rowModelObject) {
                return rowModelObject.getRealValue();
            }

            @Override
            protected List<AccessCertificationWorkItemType> getSelectedItems() {
                return CertificationItemsPanel.this.getSelectedRealObjects();
            }
        });
        return columns;
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

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(
                        IconAndStylesUtil.createDefaultBlackIcon(AccessCertificationWorkItemType.COMPLEX_TYPE));
            }

        };
    }

    public CertDecisionsStorage getPageStorage() {
        return getSession().getSessionStorage().getCertDecisions();
    }

//    @Override
//    protected String getStorageKey() {
//        return SessionStorage.KEY_CERT_DECISIONS;
//    }

    private ContainerListDataProvider<AccessCertificationWorkItemType> createProvider(IModel<Search<AccessCertificationWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CertificationItemsPanel.this.getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getCertDecisions();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getOpenCertWorkItemsQuery();
            }

        };
//        provider.setSort(CaseWorkItemType.F_DEADLINE.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

//    private void confirmAction(AccessCertificationResponseType response, List<AccessCertificationWorkItemType> items,
//            AjaxRequestTarget target) {
//        ConfirmationPanelWithComment confirmationPanel = new ConfirmationPanelWithComment(getPageBase().getMainPopupBodyId(),
//                createStringResource("ResponseConfirmationPanel.confirmation", LocalizationUtil.translateEnum(response))) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void yesPerformedWithComment(AjaxRequestTarget target, String comment) {
//                recordActionOnSelected(response, items, comment, target);
//            }
//        };
//        getPageBase().showMainPopup(confirmationPanel, target);
//    }

    private void recordActionOnSelected(AccessCertificationResponseType response, List<AccessCertificationWorkItemType> items,
            String comment, AjaxRequestTarget target) {
        if (CollectionUtils.isEmpty(items)) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = getPageBase().createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        items.forEach(item -> {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            CertMiscUtil.recordCertItemResponse(item, response, comment, resultOne, task, getPageBase());
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

    protected ObjectQuery getOpenCertWorkItemsQuery() {
        ObjectQuery query;
        if (StringUtils.isNotEmpty(campaignOid)) {
            query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(Collections.singletonList(campaignOid),
                    getPageBase().getPrincipal(), false);
        } else {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .build();
        }
        MidPointPrincipal principal = null;
        if (isMyCertItems()) {
            principal = getPageBase().getPrincipal();
        }
        return QueryUtils.createQueryForOpenWorkItems(query, principal, false);
    }

    protected boolean isMyCertItems() {
        return true;
    }

    protected boolean showOnlyNotDecidedItems() {
        return false;
    }

    private List<AbstractGuiAction<AccessCertificationWorkItemType>> getCertItemActions() {
        List<AccessCertificationResponseType> availableResponses = new AvailableResponses(getPageBase()).getResponseValues();   //from sys config
        if (CollectionUtils.isEmpty(availableResponses)) {
            availableResponses = Arrays.stream(values()).filter(r -> r != DELEGATE).collect(Collectors.toList());
        }
        List<GuiActionType> actions = getCertItemsViewActions();
        return CertMiscUtil.mergeCertItemsResponses(availableResponses, actions, getPageBase());
    }

    private List<GuiActionType> getCertItemsViewActions() {
        CompiledObjectCollectionView collectionView = getObjectCollectionView();
        return collectionView == null ? new ArrayList<>() : collectionView.getActions();
    }

}
