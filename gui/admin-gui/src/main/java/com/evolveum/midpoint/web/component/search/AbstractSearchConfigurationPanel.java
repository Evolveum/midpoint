/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author Kateryna Honchar
 */
public abstract class AbstractSearchConfigurationPanel<O extends ObjectType> extends BasePanel<SearchConfigDto> {

    private static final Trace LOG = TraceManager.getTrace(AbstractSearchConfigurationPanel.class);

    private static String ID_CONFIGURATION_PANEL = "configurationPanel";
    private static String ID_BUTTONS_PANEL = "buttonsPanel";

    public AbstractSearchConfigurationPanel(String id, IModel<SearchConfigDto> searchConfigModel){
        super(id, searchConfigModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer configPanel = new WebMarkupContainer(ID_CONFIGURATION_PANEL);
        configPanel.setOutputMarkupId(true);
        add(configPanel);

        initConfigurationPanel(configPanel);

        WebMarkupContainer buttonsPanel = new WebMarkupContainer(ID_BUTTONS_PANEL); //here should be buttons like Apply, Save to profile etc.
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);
    }

    protected abstract void initConfigurationPanel(WebMarkupContainer configPanel);

    @NotNull
    protected abstract Class<O> getObjectClass();
}
