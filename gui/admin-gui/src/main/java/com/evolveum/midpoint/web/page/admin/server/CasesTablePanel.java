/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.gui.impl.page.admin.cases.PageCase;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
* @author lskublik
 */

public abstract class CasesTablePanel extends MainObjectListPanel<CaseType> {

    private static final long serialVersionUID = 1L;

    public CasesTablePanel(String id) {
        this(id, null);
    }

    public CasesTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, CaseType.class, options);
    }

    public CasesTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType config) {
        super(id, CaseType.class, options, config);
    }

    @Override
    protected List<IColumn<SelectableBean<CaseType>, String>> createDefaultColumns() {
        return ColumnUtils.getDefaultCaseColumns(getPageBase(), isDashboard());
    }

    @Override
    protected ISelectableDataProvider<CaseType, SelectableBean<CaseType>> createProvider() {
        SelectableBeanObjectDataProvider<CaseType> provider = createSelectableBeanObjectDataProvider(() -> getCustomizeContentQuery(),
                (sortParam) -> WebComponentUtil.createMetadataOrdering(sortParam, "createTimestamp", getPrismContext()));
        provider.setSort(MetadataType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);

        return provider;
    }

    @Override
    protected boolean isCreateNewObjectEnabled() {
        return false;
    }


    protected ObjectQuery getCustomizeContentQuery() {
        ObjectFilter casesFilter = getCasesFilter();
        ObjectQuery query = null;
        if (casesFilter != null){
            query = CasesTablePanel.this.getPageBase().getPrismContext().queryFactory().createQuery(casesFilter);
        }
        return query;
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
    protected boolean isHeaderVisible() {
        return !isDashboard();
    }

    protected ObjectFilter getCasesFilter(){
        return null;
    }

    protected boolean isDashboard(){
        return false;
    }
}
