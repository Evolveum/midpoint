/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.LoggingConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.SystemConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageSystemConfiguration extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

    private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_SAVE = "save";

    private LoadableModel<SystemConfigurationDto> model;

    public PageSystemConfiguration() {
        model = new LoadableModel<SystemConfigurationDto>(false) {

            @Override
            protected SystemConfigurationDto load() {
                return loadSystemConfiguration();
            }
        };

        initLayout();
    }

    private SystemConfigurationDto loadSystemConfiguration() {
        //todo implement

        return null;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new SystemConfigPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new LoggingConfigPanel(panelId);
            }
        });

        mainForm.add(new TabbedPanel(ID_TAB_PANEL, tabs));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxButton cancel = new AjaxButton(ID_CANCEL, createStringResource("PageBase.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private void savePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        //todo implement, stay on page, refresh models, remove changes...
    }
}
