/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

public class DetailsNavigationPanel extends BasePanel<List<ContainerPanelConfigurationType>> {

    private static final String ID_NAV = "menu";
    private static final String ID_NAV_ITEM = "navItem";
    private static final String ID_NAV_ITEM_ICON = "navItemIcon";
    private static final String ID_SUB_NAVIGATION = "subNavigation";

    public DetailsNavigationPanel(String id, IModel<List<ContainerPanelConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<ContainerPanelConfigurationType> listView = new ListView<>(ID_NAV, getModel()) {

            @Override
            protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                WebMarkupContainer icon = new WebMarkupContainer(ID_NAV_ITEM_ICON);
                icon.setOutputMarkupId(true);
                icon.add(AttributeAppender.append("class",
                        item.getModelObject().getDisplay() != null ? item.getModelObject().getDisplay().getCssClass() :
                                GuiStyleConstants.CLASS_CIRCLE_FULL));
                item.add(icon);
                AjaxLink<Void> link = new AjaxLink<>(ID_NAV_ITEM) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onClickPerformed(item.getModelObject(), target);
                    }
                };
                link.setBody(Model.of(createButtonLabel(item.getModelObject())));
                item.add(link);

                DetailsNavigationPanel subPanel = new DetailsNavigationPanel(ID_SUB_NAVIGATION, Model.ofList(item.getModelObject().getPanel())) {

                    @Override
                    protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                        if (config.getPath() == null) {
                            config.setPath(item.getModelObject().getPath());
                        }
                        DetailsNavigationPanel.this.onClickPerformed(config, target);
                    }
                };
                item.add(subPanel);

//                item.add(new Label(ID_NAV_ITEM, item.getModel()));
            }
        };
        listView.setOutputMarkupId(true);
        add(listView);
    }

    protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {

    }

    private String createButtonLabel(ContainerPanelConfigurationType config) {
        if (config.getDisplay() == null) {
            return "N/A";
        }

        if (config.getDisplay().getLabel() == null) {
            return "N/A";
        }

        return config.getDisplay().getLabel().getOrig();
    }

}
