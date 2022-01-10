/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

public class OidSearchItemWrapper extends AbstractSearchItemWrapper {

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
    public boolean isApplyFilter() {
        return isVisible();
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        if (StringUtils.isEmpty((String)getValue().getValue())) {
            return null;
        }
        return pageBase.getPrismContext().queryFor(ObjectType.class)
                .id((String)getValue().getValue())
                .buildFilter();
    }
}
