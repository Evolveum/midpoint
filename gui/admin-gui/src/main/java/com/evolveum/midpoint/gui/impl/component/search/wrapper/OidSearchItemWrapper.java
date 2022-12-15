/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.OidSearchItemPanel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.commons.lang3.StringUtils;

public class OidSearchItemWrapper extends FilterableSearchItemWrapper<String> {

    @Override
    public Class<OidSearchItemPanel> getSearchItemPanelClass() {
        return OidSearchItemPanel.class;
    }

    @Override
    public String getName() {
        return "SearchPanel.oid";
    }

    @Override
    public String getHelp() {
        return "SearchPanel.oid.help";
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return SearchBoxModeType.OID.equals(searchBoxMode);
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (StringUtils.isEmpty(getValue().getValue())) {
            return null;
        }
        return pageBase.getPrismContext().queryFor(type)
                .id(getValue().getValue())
                .buildFilter();
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

}
