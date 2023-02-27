/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsTablePanel extends BasePanel<List<DetailsTableItem>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";

    private IModel<String> iconCssClass;

    private IModel<String> title;

    public DetailsTablePanel(String id, IModel<String> iconCssClass, IModel<String> title, IModel<List<DetailsTableItem>> model) {
        super(id, model);

        this.iconCssClass = iconCssClass;
        this.title = title;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeModifier.append("class", "card"));

        WebComponent icon = new WebComponent(ID_ICON);
        icon.add(AttributeModifier.append("class", iconCssClass));
        icon.add(new VisibleBehaviour(() -> iconCssClass.getObject() != null));
        add(icon);

        Label title = new Label(ID_TITLE, this.title);
        title.setRenderBodyOnly(true);
        title.add(new VisibleBehaviour(() -> this.title.getObject() != null));
        add(title);

        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, getModel()) {

            @Override
            protected void populateItem(ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                item.add(new Label(ID_LABEL, () -> data.getLabel().getObject()));
                item.add(data.createValueComponent(ID_VALUE));

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        add(details);
    }
}
