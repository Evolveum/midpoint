/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTablePanel;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumnPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

// todo not finished
public class CertItemDetailsPanel extends BasePanel<AccessCertificationCaseType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_ACTIONS_PANEL = "actionsPanel";

    public CertItemDetailsPanel(String id, IModel<AccessCertificationCaseType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_TITLE, createStringResource("ResponseViewPopup.title"));
        label.setOutputMarkupId(true);
        add(label);

        IModel<DisplayType> detailsPanelDisplayModel = createDetailsPanelDisplayModel();
        IModel<List<DetailsTableItem>> detailsModel = createDetailsPanelModel();
        DetailsTablePanel detailsPanel = new DetailsTablePanel(ID_DETAILS_PANEL, detailsPanelDisplayModel, detailsModel);
        detailsPanel.setOutputMarkupId(true);
        add(detailsPanel);

    }

    private IModel<DisplayType> createDetailsPanelDisplayModel() {
        return () -> {
            //todo
            DisplayType display = new DisplayType()
                    .label("John Doe")
                    .help("Reviewer");
            return display;
        };
    }

    private IModel<List<DetailsTableItem>> createDetailsPanelModel() {
        return () -> {
            List<DetailsTableItem> list = new ArrayList<>();
            list.add(new DetailsTableItem(createStringResource("WorkItemsPanel.object")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    return new ObjectReferenceColumnPanel(id, Model.of(getModelObject().getObjectRef()));
                }
            });
            return list;

        };
    }

    ;
}
