/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.page.admin.certification.dto.SearchingUtils;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
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
    private String campaignOid;
    private int stageNumber;

    public CertificationCasesPanel(String id, String campaignOid, int stageNumber) {
        super(id, AccessCertificationCaseType.class);
        this.campaignOid = campaignOid;
        this.stageNumber = stageNumber;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String>> createDefaultColumns() {
        return initColumns();
    }

    private List<IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String>> columns =
                ColumnUtils.getDefaultCertCaseColumns(stageNumber);
        columns.add(createShowDetailsColumn());
        return columns;
    }

    private IColumn<PrismContainerValueWrapper<AccessCertificationCaseType>, String> createShowDetailsColumn() {
        return new AjaxLinkColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AccessCertificationCaseType>> rowModel) {
                return createStringResource("CertificationItemsPanel.showDetails");
            }


            @Override
            public void onClick(AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<AccessCertificationCaseType>> rowModel) {
                //todo uncomment when response details panel is implemented
//                showResponseDetailsPopup(target, rowModel.getObject());
            }

        };
    }

    private void showResponseDetailsPopup(AjaxRequestTarget target,
            PrismContainerValueWrapper<AccessCertificationCaseType> rowModel) {
        CertResponseDetailsPanel panel = new CertResponseDetailsPanel(getPageBase().getMainPopupBodyId(),
                Model.of(rowModel.getRealValue()), stageNumber);
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
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getCertDecisions();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                PrismContext prismContext = PrismContext.get();
                ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class).build();
                query.addFilter(prismContext.queryFactory().createOwnerHasOidIn(campaignOid));
                return query;
            }

//            @NotNull
//            private ObjectQuery createFinalQuery(InOidFilter inOidFilter, PrismContext prismContext) {
//                ObjectQuery query = getQuery();
//                if (query != null) {
//                    query = query.clone();
//                    if (query.getFilter() == null) {
//                        query.setFilter(inOidFilter);
//                    } else {
//                        query.setFilter(prismContext.queryFactory().createAnd(query.getFilter(), inOidFilter));
//                    }
//                } else {
//                    query = getPrismContext().queryFactory().createQuery(inOidFilter);
//                }
//                return query;
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