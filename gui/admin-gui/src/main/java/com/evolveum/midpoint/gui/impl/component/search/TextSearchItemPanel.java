/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class TextSearchItemPanel extends PropertySearchItemPanel<TextSearchItemWrapper> {

    public TextSearchItemPanel(String id, IModel<TextSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        ItemDefinition<?> itemDef = getModelObject().getItemDef();
        PrismObject<LookupTableType> lookupTablePrism = WebComponentUtil.findLookupTable(itemDef, getPageBase());
        LookupTableType lookupTable = lookupTablePrism != null ? lookupTablePrism.asObjectable() : null;
        if (lookupTable != null) {
            return createAutoCompetePanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE), lookupTable);
        } else {
            return new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), TextSearchItemWrapper.F_VALUE));
        }
    }
}
