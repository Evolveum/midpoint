/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AppenderConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author katkav
 *
 */
public class ProfilingConfigPanel extends BasePanel<ProfilingDto> {

    private static final String DOT_CLASS = LoggingConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";

    private static final String ID_PROFILING_ENABLED_NOTE = "profilingEnabledNote";
    private static final String ID_LOGGERS_TABLE = "loggersTable";
    private static final String ID_ROOT_LEVEL = "rootLevel";
    private static final String ID_ROOT_APPENDER = "rootAppender";
    private static final String ID_TABLE_APPENDERS = "appendersTable";
    private static final String ID_BUTTON_ADD_CONSOLE_APPENDER = "addConsoleAppender";
    private static final String ID_BUTTON_ADD_FILE_APPENDER = "addFileAppender";
    private static final String ID_BUTTON_DELETE_APPENDER = "deleteAppender";
    private static final String ID_BUTTON_ADD_STANDARD_LOGGER = "addStandardLogger";
    private static final String ID_DUMP_INTERVAL_TOOLTIP = "dumpIntervalTooltip";

    public ProfilingConfigPanel(String id, IModel<ProfilingDto> model, PageSystemConfiguration parentPage) {
        super(id, model);
		initLayout(parentPage);
    }

    private IModel<List<String>> createAppendersListModel() {
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<>();

                ProfilingDto dto = getModel().getObject();
                for (AppenderConfiguration appender : dto.getAppenders()) {
                    list.add(appender.getName());
                }

                return list;
            }
        };
    }

    private void initLayout(PageSystemConfiguration parentPage) {

		WebMarkupContainer profilingEnabledNote = new WebMarkupContainer(ID_PROFILING_ENABLED_NOTE);
		profilingEnabledNote.setVisible(!parentPage.getMidpointConfiguration().isProfilingEnabled());
		add(profilingEnabledNote);

        //Entry-Exit profiling init
        DropDownChoice<ProfilingLevel> profilingLevel = new DropDownChoice<>("profilingLevel",
            new PropertyModel<>(getModel(), "profilingLevel"),
                WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
            new EnumChoiceRenderer<>(this));
        profilingLevel.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(profilingLevel);

        DropDownChoice<String> profilingAppender = new DropDownChoice<>("profilingAppender",
            new PropertyModel<>(getModel(), "profilingAppender"), createAppendersListModel());
        profilingAppender.setNullValid(true);
        profilingAppender.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(profilingAppender);

        //Subsystem and general profiling init
        CheckBox requestFilter = WebComponentUtil.createAjaxCheckBox("requestFilter", new PropertyModel<>(getModel(), "requestFilter"));

        CheckBox performanceStatistics = WebComponentUtil.createAjaxCheckBox("performanceStatistics", new PropertyModel<>(getModel(), "performanceStatistics"));
        CheckBox subsystemModel = WebComponentUtil.createAjaxCheckBox("subsystemModel", new PropertyModel<>(getModel(), "subsystemModel"));
        CheckBox subsystemRepository = WebComponentUtil.createAjaxCheckBox("subsystemRepository", new PropertyModel<>(getModel(), "subsystemRepository"));
        CheckBox subsystemProvisioning = WebComponentUtil.createAjaxCheckBox("subsystemProvisioning", new PropertyModel<>(getModel(), "subsystemProvisioning"));
        //CheckBox subsystemUcf = WebComponentUtil.createAjaxCheckBox("subsystemUcf", new PropertyModel<Boolean>(getModel(), "subsystemUcf"));
        CheckBox subsystemResourceObjectChangeListener = WebComponentUtil.createAjaxCheckBox("subsystemSynchronizationService", new PropertyModel<>(getModel(), "subsystemSynchronizationService"));
        CheckBox subsystemTaskManager = WebComponentUtil.createAjaxCheckBox("subsystemTaskManager", new PropertyModel<>(getModel(), "subsystemTaskManager"));
        CheckBox subsystemWorkflow = WebComponentUtil.createAjaxCheckBox("subsystemWorkflow", new PropertyModel<>(getModel(), "subsystemWorkflow"));
        add(requestFilter);
        add(performanceStatistics);
        add(subsystemModel);
        add(subsystemRepository);
        add(subsystemProvisioning);
        //add(subsystemUcf);
        add(subsystemResourceObjectChangeListener);
        add(subsystemTaskManager);
        add(subsystemWorkflow);

        TextField<Integer> dumpInterval = WebComponentUtil.createAjaxTextField("dumpInterval", new PropertyModel<Integer>(getModel(),
                "dumpInterval"));
        add(dumpInterval);

        Label dumpIntervalTooltip = new Label(ID_DUMP_INTERVAL_TOOLTIP);
        dumpIntervalTooltip.add(new InfoTooltipBehavior());
        add(dumpIntervalTooltip);
    }



}
