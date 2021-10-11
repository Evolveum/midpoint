/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 */
public abstract class AbstractItemWrapperColumnPanel<IW extends ItemWrapper, VW extends PrismValueWrapper> extends BasePanel<IW> {

    private static final long serialVersionUID = 1L;
    protected PageBase pageBase;
    protected ItemPath itemName;

    private ColumnType columnType;

    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    AbstractItemWrapperColumnPanel(String id, IModel<IW> model, ColumnType columnType) {
        super(id, model);
        this.columnType = columnType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        //TODO migth fix MID-6125, not sure about this one, it's really hard to replicate
        // but prismContext is transient so it might be lost during the serilaization/deserialization
        if (getModelObject() != null) {
            AbstractItemWrapperColumnPanel.this.getModelObject().revive(getPageBase().getPrismContext());
        }
        ListView<VW> listView = new ListView<VW>(ID_VALUES, new PropertyModel<>(getModel(), "values")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<VW> item) {
                populate(item);
            }
        };
        listView.setReuseItems(true);
        listView.setOutputMarkupId(true);

        add(listView);
    }

    protected void populate(ListItem<VW> item) {
        if (item.getModelObject() != null && item.getModelObject().getParent() != null) {
            item.getModelObject().getParent().revive(getPageBase().getPrismContext());
        }
        switch (columnType) {
            case STRING:
                Label label = new Label(ID_VALUE, createLabel(item.getModelObject()));
                item.add(label);
                break;
            case LINK:
                item.add(createLink(ID_VALUE, item.getModel()));
                break;
            case VALUE:
                item.add(createValuePanel(ID_VALUE, getModel(), item.getModelObject()));
                break;

        }

    }

    protected abstract String createLabel(VW object);
    protected abstract Panel createLink(String id, IModel<VW> object);
    protected abstract Panel createValuePanel(String id, IModel<IW> model, VW object);
}
