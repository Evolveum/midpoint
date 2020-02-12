/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 *
 */
public class TaskMainPanel extends Panel {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_BUTTON_PANEL = "buttonPanel";

    private final LoadableModel<PrismObjectWrapper<TaskType>> objectModel;
    private final IModel<TaskDto> taskDtoModel;
    private final IModel<Boolean> showAdvancedFeaturesModel;
    private final PageTaskEdit parentPage;

    public TaskMainPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> objectModel, IModel<TaskDto> taskDtoModel,
            IModel<Boolean> showAdvancedFeaturesModel, PageTaskEdit parentPage) {
        super(id, objectModel);
        this.objectModel = objectModel;
        this.taskDtoModel = taskDtoModel;
        this.showAdvancedFeaturesModel = showAdvancedFeaturesModel;
        this.parentPage = parentPage;
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form<>(ID_MAIN_FORM, true);
        add(mainForm);
        initButtons(mainForm);
    }

    protected List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
        final TaskTabsVisibility visibility = new TaskTabsVisibility();



        tabs.add(
                new AbstractTab(parentPage.createStringResource("pageTaskEdit.performance")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskPerformanceTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
                    }
                    @Override
                    public boolean isVisible() {
                        return visibility.computeEnvironmentalPerformanceVisible(parentPage);
                    }
                });

        tabs.add(
                new AbstractTab(parentPage.createStringResource("pageTaskEdit.operation")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskOperationTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
                    }
                    @Override
                    public boolean isVisible() {
                        return visibility.computeOperationVisible(parentPage);
                    }
                });

        return tabs;
    }

    public Form getMainForm() {
        return (Form) get(ID_MAIN_FORM);
    }

    public TabbedPanel<ITab> getTabPanel() {
        return (TabbedPanel<ITab>) getMainForm().get(ID_TAB_PANEL);
    }

    public WebMarkupContainer getButtonPanel() {
        return (WebMarkupContainer) getMainForm().get(ID_BUTTON_PANEL);
    }

    private void initButtons(Form mainForm) {
        WebMarkupContainer buttonPanel = new WebMarkupContainer(ID_BUTTON_PANEL);
        buttonPanel.setOutputMarkupId(true);
        mainForm.add(buttonPanel);


    }
}
