/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.page.admin.certification.dto.SearchingUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CertificationCasesPanel extends
        ContainerableListPanel<AccessCertificationCaseType, PrismContainerValueWrapper<AccessCertificationCaseType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CertificationCasesPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";
    private final String campaignOid;
    private final int stageNumber;
    IModel<AccessCertificationCampaignType> campaignModel;

    public CertificationCasesPanel(String id, String campaignOid, int stageNumber) {
        super(id, AccessCertificationCaseType.class);
        this.campaignOid = campaignOid;
        this.stageNumber = stageNumber;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
    }

    private void initModels() {
        campaignModel = new LoadableModel<>(true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCampaignType load() {
                return loadCampaign();
            }
        };
    }

    private AccessCertificationCampaignType loadCampaign() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CAMPAIGN);
        OperationResult result = task.getResult();
        PrismObject<AccessCertificationCampaignType> campaign =
                WebModelServiceUtils.loadObject(AccessCertificationCampaignType.class, campaignOid, getPageBase(), task, result);
        return campaign != null ? campaign.asObjectable() : null;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String>> createDefaultColumns() {
        return initColumns();
    }

    protected IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String> createActionsColumn() {
        return createShowDetailsColumn();
    }

    private List<IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String>> initColumns() {
        return ColumnUtils.getDefaultCertCaseColumns(stageNumber, getPageBase());
    }

    private IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String> createShowDetailsColumn() {
        return new AbstractColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AccessCertificationCaseType>>> cellItem,
                    String componentId, final IModel<PrismContainerValueWrapper<AccessCertificationCaseType>> rowModel) {

                cellItem.add(new AjaxLinkPanel(componentId, createStringResource("CertificationItemsPanel.showDetails")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        showResponseDetailsPopup(target, rowModel);
                    }
                });
            }

        };
    }

    private void showResponseDetailsPopup(AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<AccessCertificationCaseType>> rowModel) {
        CertResponseDetailsPanel panel = new CertResponseDetailsPanel(getPageBase().getMainPopupBodyId(),
                rowModel,
//                Model.of(rowModel.getRealValue()),
                stageNumber);
        getPageBase().showMainPopup(panel, target);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<AccessCertificationCaseType>> createProvider() {
        return CertificationCasesPanel.this.createProvider(getSearchModel());
    }

    private ContainerListDataProvider<AccessCertificationCaseType> createProvider(
            IModel<Search<AccessCertificationCaseType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CertificationCasesPanel.this.getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationCaseType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            @Serial private static final long serialVersionUID = 1L;


            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                PrismContext prismContext = PrismContext.get();
                ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class).build();
                query.addFilter(prismContext.queryFactory().createOwnerHasOidIn(campaignOid));
//                query.addFilter(createIterationFilter());
                query.addFilter(createCasesFilter());
                return query;
            }

            private ObjectFilter createCasesFilter() {
                int iteration = campaignModel != null ? campaignModel.getObject().getIteration() : 0;
                return getPageBase().getPrismContext().queryFor(AccessCertificationCaseType.class)
                        .item(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_ITERATION)
                        .eq(iteration)
                        .and()
                        .item(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_STAGE_NUMBER)
                        .eq(stageNumber)
                        .or()
                        .item(AccessCertificationCaseType.F_ITERATION)
                        .eq(iteration)
                        .and()
                        .item(AccessCertificationCaseType.F_STAGE_NUMBER)
                        .eq(stageNumber)
                        .or()
                        .item(AccessCertificationCaseType.F_ITERATION)
                        .eq(iteration)
                        .and()
                        .not()
                        .exists(AccessCertificationCaseType.F_WORK_ITEM)
                        .build()
                        .getFilter();
            }

//            private ObjectFilter createIterationFilter() {
//                int iteration = campaignModel != null ? campaignModel.getObject().getIteration() : 0;
//                return getPageBase().getPrismContext().queryFor(AccessCertificationCaseType.class)
//                        .item(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_ITERATION)
//                        .eq(iteration)
//                        .build()
//                        .getFilter();
//            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                return SearchingUtils.createObjectOrderings(sortParam, false, getPrismContext());
            }

        };
        provider.setSort(AccessCertificationCaseType.F_CURRENT_STAGE_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    @Override
    public List<AccessCertificationCaseType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(PrismValueWrapper::getRealValue).collect(Collectors.toList());
    }

}
