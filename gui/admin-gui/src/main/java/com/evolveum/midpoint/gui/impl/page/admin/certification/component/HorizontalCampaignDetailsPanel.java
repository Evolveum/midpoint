/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.List;

public class HorizontalCampaignDetailsPanel extends BasePanel<List<DetailsTableItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_COMPONENT_1 = "detailsComponent1";
    private static final String ID_DETAILS_COMPONENT_2 = "detailsComponent2";

    public HorizontalCampaignDetailsPanel(String id, IModel<List<DetailsTableItem>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, getModel()) {

            @Override
            protected void populateItem(ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                if (data.isValueComponentBeforeLabel()) {
                    item.add(data.createValueComponent(ID_DETAILS_COMPONENT_1));
                    item.add(new Label(ID_DETAILS_COMPONENT_2, () -> data.getLabel().getObject()));
                } else {
                    item.add(new Label(ID_DETAILS_COMPONENT_1, () -> data.getLabel().getObject()));
                    item.add(data.createValueComponent(ID_DETAILS_COMPONENT_2));
                }

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        add(details);
    }
}
