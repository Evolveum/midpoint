/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.OidSearchItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public class OidSearchItemWrapper extends FilterableSearchItemWrapper<String> implements QueryWrapper {

    @Override
    public Class<OidSearchItemPanel> getSearchItemPanelClass() {
        return OidSearchItemPanel.class;
    }

    @Override
    public IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return LocalizationUtil.translate("SearchPanel.oid");
            }
        };
    }

    @Override
    public IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return LocalizationUtil.translate("SearchPanel.oid.help");
            }
        };
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("");
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

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variables) throws SchemaException {
        if (StringUtils.isEmpty(getValue().getValue())) {
            return pageBase.getPrismContext().queryFor(ObjectType.class).build();
        }
        return pageBase.getPrismContext().queryFor(ObjectType.class)
                .id(getValue().getValue())
                .build();
    }

    @Override
    public String getAdvancedError() {
        return null;
    }

    @Override
    public void setAdvancedError(String advancedError) {

    }
}
