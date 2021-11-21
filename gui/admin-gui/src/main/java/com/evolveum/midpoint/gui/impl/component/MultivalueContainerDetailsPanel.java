/*
 * Copyright (c) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.TabbedPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public abstract class MultivalueContainerDetailsPanel<C extends Containerable>
        extends BasePanel<PrismContainerValueWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DETAILS = "details";

    private boolean isAddDefaultPanel = true;
    private ContainerPanelConfigurationType config;

    public MultivalueContainerDetailsPanel(String id, IModel<PrismContainerValueWrapper<C>> model) {
        super(id, model);
    }

    public MultivalueContainerDetailsPanel(String id, IModel<PrismContainerValueWrapper<C>> model, boolean addDefaultPanel) {
        super(id, model);
        this.isAddDefaultPanel = addDefaultPanel;
    }

    public MultivalueContainerDetailsPanel(String id, IModel<PrismContainerValueWrapper<C>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model);
        this.isAddDefaultPanel = addDefaultPanel;
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    protected abstract DisplayNamePanel<C> createDisplayNamePanel(String displayNamePanelId);

    private void initLayout() {

        List<ITab> tabs = createTabs();
        if (isAddDefaultPanel) {
            tabs.add(0, addBasicContainerValuePanel());
        }
        TabbedPanel tabbedPanel = WebComponentUtil.createTabPanel(ID_DETAILS, getPageBase(), tabs, null);
        tabbedPanel.setOutputMarkupId(true);
        add(tabbedPanel);

        DisplayNamePanel<C> displayNamePanel = createDisplayNamePanel(ID_DISPLAY_NAME);

        displayNamePanel.setOutputMarkupId(true);
        add(displayNamePanel);
    }

    @NotNull protected List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
//        if (isAddDefaultPanel) {
//            tabs.add(addBasicContainerValuePanel());
//        }
        return tabs;
    }
    protected AbstractTab addBasicContainerValuePanel() {
        PanelTab panel = new PanelTab(createStringResource("Basic")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return getBasicContainerValuePanel(panelId);
            }
        };
        return panel;

    }

    protected Panel getBasicContainerValuePanel(String idPanel) {
        ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                .visibilityHandler(wrapper -> getBasicTabVisibity(wrapper))
                .editabilityHandler(wrapper -> getBasicTabEditability(wrapper));

        if (getMandatoryHandler() != null) {
            builder.mandatoryHandler(getMandatoryHandler());
        }
        if (config != null) {
            builder.panelConfiguration(config);
        }

        ItemPanelSettings settings = builder.build();
        Panel containerValue = getPageBase().initContainerValuePanel(idPanel, getModel(), settings);
        return containerValue;
    }

    public ContainerPanelConfigurationType getConfig() {
        return config;
    }

    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) { //, ItemPath parentPath) {
        return ItemVisibility.AUTO;
    }

    protected boolean getBasicTabEditability(ItemWrapper<?, ?> itemWrapper) {
        return true;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

}
