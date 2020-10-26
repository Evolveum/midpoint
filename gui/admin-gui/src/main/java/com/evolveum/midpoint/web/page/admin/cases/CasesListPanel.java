/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
//TODO very-very temporary panel. to be refactored/unified with PageCases table
public class CasesListPanel extends BasePanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_CASES_TABLE = "casesTable";

    public CasesListPanel(String id){
        super(id);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        MainObjectListPanel<CaseType> table = new MainObjectListPanel<CaseType>(ID_CASES_TABLE,
                CaseType.class, Collections.emptyList()) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
                CasesListPanel.this.getPageBase().navigateToNext(PageCase.class, pageParameters);
            }

            @Override
            protected IColumn<SelectableBean<CaseType>, String> createCheckboxColumn() {
                if (isDashboard()){
                    return null;
                } else {
                    return super.createCheckboxColumn();
                }
            }


            @Override
            protected List<IColumn<SelectableBean<CaseType>, String>> createDefaultColumns() {
                return (List) ColumnUtils.getDefaultCaseColumns(CasesListPanel.this.getPageBase(), isDashboard());
            }

            @Override
            protected boolean isCreateNewObjectEnabled(){
                return false;
            }

            @Override
            protected ObjectQuery customizeContentQuery(ObjectQuery query) {
                if (query == null) {
                    query = CasesListPanel.this.getPageBase().getPrismContext().queryFor(CaseType.class)
                            .build();
                }
                ObjectFilter casesFilter = getCasesFilter();
                if (casesFilter != null){
                    query.addFilter(casesFilter);
                }
                return query;
            }

            @Override
            protected WebMarkupContainer initButtonToolbar(String id) {
                return null;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu(){
                return new ArrayList<>();
            }

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                if (!isDashboard()){
                    return super.createHeader(headerId);
                } else {
                    WebMarkupContainer headerContainer = new WebMarkupContainer(headerId);
                    headerContainer.setVisible(false);
                    return headerContainer;
                }
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_CASE_CHILD_CASES_TAB;
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
                return CasesListPanel.this.isDashboard();
            }

            @Override
            protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
                return WebComponentUtil.createMetadataOrdering(sortParam, "createTimestamp", getPrismContext());
            }

            @Override
            protected void setDefaultSorting(BaseSortableDataProvider provider){
                provider.setSort(MetadataType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    protected ObjectFilter getCasesFilter(){
        return null;
    }

    protected boolean isDashboard(){
        return false;
    }

 }
