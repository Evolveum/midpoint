/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Kateryna Honchar
 */
public abstract class AbstractSearchConfigurationPanel extends BasePanel<Search> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOG = TraceManager.getTrace(AbstractSearchConfigurationPanel.class);

    protected static final String ID_CONFIGURATION_PANEL = "configurationPanel";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";
    private static final String ID_APPLY_FILTER_BUTTON = "applyFilterButton";
    private static final String ID_SAVE_FILTER_BUTTON = "saveFilterButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    public AbstractSearchConfigurationPanel(String id, IModel<Search> searchModel) {
        super(id, searchModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer configPanel = new WebMarkupContainer(ID_CONFIGURATION_PANEL);
        configPanel.setOutputMarkupId(true);
        add(configPanel);

        initConfigurationPanel(configPanel);

        initButtonsPanel();
    }

    private void initButtonsPanel() {
        WebMarkupContainer buttonsPanel = new WebMarkupContainer(ID_BUTTONS_PANEL);
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);

        AjaxButton applyFilterButton = new AjaxButton(ID_APPLY_FILTER_BUTTON, createStringResource("SearchPropertiesConfigPanel.applyFilterButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                //todo
            }
        };
        applyFilterButton.setOutputMarkupId(true);
        buttonsPanel.add(applyFilterButton);

        AjaxButton saveFilterButton = new AjaxButton(ID_SAVE_FILTER_BUTTON, createStringResource("SearchPropertiesConfigPanel.saveFilterButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                //todo
            }
        };
        saveFilterButton.setOutputMarkupId(true);
        buttonsPanel.add(saveFilterButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("SearchPropertiesConfigPanel.cancelButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                //todo
            }
        };
        cancelButton.setOutputMarkupId(true);
        buttonsPanel.add(cancelButton);
    }

    protected abstract void initConfigurationPanel(WebMarkupContainer configPanel);

    @NotNull
    protected abstract <O extends ObjectType> Class<O> getObjectClass();
}
