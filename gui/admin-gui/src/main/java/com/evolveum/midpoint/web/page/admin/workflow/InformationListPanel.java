/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

public class InformationListPanel extends BasePanel<List<InformationType>> {

    private static final String ID_INFORMATION_LIST = "informationList";
    private static final String ID_INFORMATION = "information";

    public InformationListPanel(String id, IModel<List<InformationType>> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        ListView<InformationType> list = new ListView<>(ID_INFORMATION_LIST, getModel()) {
            @Override
            protected void populateItem(ListItem<InformationType> item) {
                InformationPanel information = new InformationPanel(ID_INFORMATION, item.getModel());
                information.add(new VisibleBehaviour(() -> item.getModelObject() != null));

                item.add(information);
            }
        };
        add(list);
    }
}
