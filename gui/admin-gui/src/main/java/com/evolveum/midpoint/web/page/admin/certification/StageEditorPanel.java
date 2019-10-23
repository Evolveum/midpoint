/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;

/**
 * @author lazyman
 */
public class StageEditorPanel extends BasePanel<StageDefinitionDto> {

    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";

    public StageEditorPanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);

        initPanelLayout();
    }

    private void initPanelLayout() {
        AjaxLink<Void> name = new AjaxLink<Void>(ID_NAME) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        Label nameLabel = new Label(ID_NAME_LABEL, getModel().getObject().getName() == null || getModel().getObject().getName().trim().equals("") ?
                "Stage definition #" +  getModel().getObject().getNumber()
                : getModel().getObject().getName());
        name.add(nameLabel);

        add(name);
    }

    private void nameClickPerformed(AjaxRequestTarget target) {
        TabbedPanel tabbedPanel = this.findParent(TabbedPanel.class);
        IModel<List<ITab>> tabsModel = tabbedPanel.getTabs();
        List<ITab> tabsList = tabsModel.getObject();
        PropertyModel<String> tabNameModel;
        if (getModel().getObject().getName() == null || getModel().getObject().getName().trim().equals("")){
            tabNameModel = new PropertyModel<>(getModel(), StageDefinitionDto.F_NUMBER);
        } else {
            tabNameModel = new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME);
        }

        for (ITab tab : tabsList){
            if (tab.getTitle().getObject().equals(tabNameModel.getObject())){
                int i = tabsList.indexOf(tab);
                tabbedPanel.setSelectedTab(i);
                target.add(tabbedPanel);
                return;
            }
        }

        tabsList.add(new AbstractTab(tabNameModel) {
            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new DefinitionStagePanel(panelId, getModel());
            }
        });
        tabbedPanel.setSelectedTab(tabsList.size() - 1);
        target.add(tabbedPanel);
    }

}
