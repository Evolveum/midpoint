/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.impl.prism.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

/**
 * @author skublik
 */

public abstract class MultivalueContainerDetailsPanel<C extends Containerable> extends BasePanel<PrismContainerValueWrapper<C>> {
    private static final long serialVersionUID = 1L;

    private final static String ID_DISPLAY_NAME = "displayName";
    private final static String ID_BASIC_PANEL = "basicPanel";
    protected final static String ID_SPECIFIC_CONTAINERS_PANEL = "specificContainersPanel";

    public MultivalueContainerDetailsPanel(String id, IModel<PrismContainerValueWrapper<C>> model){
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    protected abstract DisplayNamePanel<C> createDisplayNamePanel(String displayNamePanelId);

    private void initLayout(){

        DisplayNamePanel<C> displayNamePanel = createDisplayNamePanel(ID_DISPLAY_NAME);

        displayNamePanel.setOutputMarkupId(true);
        add(displayNamePanel);

        addBasicContainerValuePanel(ID_BASIC_PANEL);
        add(getSpecificContainers(ID_SPECIFIC_CONTAINERS_PANEL));
    }

    protected WebMarkupContainer getSpecificContainers(String contentAreaId) {
        return new WebMarkupContainer(contentAreaId);
    }

    protected void addBasicContainerValuePanel(String idPanel){
        add(getBasicContainerValuePanel(idPanel));
    }

    protected Panel getBasicContainerValuePanel(String idPanel){
        ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                .visibilityHandler(wrapper -> getBasicTabVisibity(wrapper))
                .showOnTopLevel(true)
                .editabilityHandler(wrapper -> getBasicTabEditability(wrapper));

        if (getMandatoryHandler() != null) {
            builder.mandatoryHandler(getMandatoryHandler());
        }

        ItemPanelSettings settings = builder.build();
        Panel containerValue = getPageBase().initContainerValuePanel(idPanel, getModel(), settings);
        return containerValue;
    }

    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?, ?, ?> itemWrapper) { //, ItemPath parentPath) {
        return ItemVisibility.AUTO;
    }

    protected boolean getBasicTabEditability(ItemWrapper<?,?,?,?> itemWrapper) {
        return true;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

}
