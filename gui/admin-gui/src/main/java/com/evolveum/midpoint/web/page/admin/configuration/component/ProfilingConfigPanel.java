package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EditableCheckboxColumn;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;

import com.evolveum.midpoint.web.page.admin.configuration.dto.AppenderConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ClassLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ComponentLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.FileAppenderConfig;
import com.evolveum.midpoint.web.page.admin.configuration.dto.FilterConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InputStringValidator;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LevelValidator;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerValidator;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.StandardLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.StandardLoggerType;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

public class ProfilingConfigPanel extends SimplePanel<ProfilingDto> {

    private static final String DOT_CLASS = LoggingConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";

    private static final String ID_LOGGERS_TABLE = "loggersTable";
    private static final String ID_ROOT_LEVEL = "rootLevel";
    private static final String ID_ROOT_APPENDER = "rootAppender";
    private static final String ID_TABLE_APPENDERS = "appendersTable";
    private static final String ID_BUTTON_ADD_CONSOLE_APPENDER = "addConsoleAppender";
    private static final String ID_BUTTON_ADD_FILE_APPENDER = "addFileAppender";
    private static final String ID_BUTTON_DELETE_APPENDER = "deleteAppender";
    private static final String ID_BUTTON_ADD_STANDARD_LOGGER = "addStandardLogger";
    private static final String ID_DUMP_INTERVAL_TOOLTIP = "dumpIntervalTooltip";

    public ProfilingConfigPanel(String id, IModel<ProfilingDto> model) {
        super(id, model);
    }

//    @Override
//    public IModel<LoggingDto> createModel() {
//        return new LoadableModel<LoggingDto>(false) {
//
//            @Override
//            protected LoggingDto load() {
//                return initLoggingModel();
//            }
//        };
//    }

//    private LoggingDto initLoggingModel() {
//        LoggingDto dto = null;
//        OperationResult result = new OperationResult(OPERATION_LOAD_LOGGING_CONFIGURATION);
//        try {
//            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOGGING_CONFIGURATION);
//
//            PrismObject<SystemConfigurationType> config = getPageBase().getModelService().getObject(
//                    SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
//                    task, result);
//            SystemConfigurationType systemConfiguration = config.asObjectable();
//            LoggingConfigurationType logging = systemConfiguration.getLogging();
//            dto = new LoggingDto(config, logging);
//
//            result.recordSuccess();
//        } catch (Exception ex) {
//            result.recordFatalError("Couldn't load logging configuration.", ex);
//        }
//
//        if (!result.isSuccess()) {
//            getPageBase().showResult(result);
//        }
//
//        if (dto == null) {
//            dto = new LoggingDto();
//        }
//
//        return dto;
//    }

    @Override
    protected void initLayout() {
       initProfiling();
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



    private void initProfiling(){
        //Entry-Exit profiling init
        DropDownChoice<ProfilingLevel> profilingLevel = new DropDownChoice<>("profilingLevel",
                new PropertyModel<ProfilingLevel>(getModel(), "profilingLevel"),
                WebMiscUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
                new EnumChoiceRenderer<ProfilingLevel>(this));
        profilingLevel.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(profilingLevel);

        DropDownChoice<String> profilingAppender = new DropDownChoice<>("profilingAppender",
                new PropertyModel<String>(getModel(), "profilingAppender"), createAppendersListModel());
        profilingAppender.setNullValid(true);
        profilingAppender.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(profilingAppender);

        //Subsystem and general profiling init
        CheckBox requestFilter = WebMiscUtil.createAjaxCheckBox("requestFilter", new PropertyModel<Boolean>(getModel(), "requestFilter"));
        
        CheckBox performanceStatistics = WebMiscUtil.createAjaxCheckBox("performanceStatistics", new PropertyModel<Boolean>(getModel(), "performanceStatistics"));
        CheckBox subsystemModel = WebMiscUtil.createAjaxCheckBox("subsystemModel", new PropertyModel<Boolean>(getModel(), "subsystemModel"));
        CheckBox subsystemRepository = WebMiscUtil.createAjaxCheckBox("subsystemRepository", new PropertyModel<Boolean>(getModel(), "subsystemRepository"));
        CheckBox subsystemProvisioning = WebMiscUtil.createAjaxCheckBox("subsystemProvisioning", new PropertyModel<Boolean>(getModel(), "subsystemProvisioning"));
        CheckBox subsystemUcf = WebMiscUtil.createAjaxCheckBox("subsystemUcf", new PropertyModel<Boolean>(getModel(), "subsystemUcf"));
        CheckBox subsystemResourceObjectChangeListener = WebMiscUtil.createAjaxCheckBox("subsystemResourceObjectChangeListener", new PropertyModel<Boolean>(getModel(), "subsystemResourceObjectChangeListener"));
        CheckBox subsystemTaskManager = WebMiscUtil.createAjaxCheckBox("subsystemTaskManager", new PropertyModel<Boolean>(getModel(), "subsystemTaskManager"));
        CheckBox subsystemWorkflow = WebMiscUtil.createAjaxCheckBox("subsystemWorkflow", new PropertyModel<Boolean>(getModel(), "subsystemWorkflow"));
        add(requestFilter);
        add(performanceStatistics);
        add(subsystemModel);
        add(subsystemRepository);
        add(subsystemProvisioning);
        add(subsystemUcf);
        add(subsystemResourceObjectChangeListener);
        add(subsystemTaskManager);
        add(subsystemWorkflow);

        TextField<Integer> dumpInterval = WebMiscUtil.createAjaxTextField("dumpInterval", new PropertyModel<Integer>(getModel(),
                "dumpInterval"));
        add(dumpInterval);

        Label dumpIntervalTooltip = new Label(ID_DUMP_INTERVAL_TOOLTIP);
        dumpIntervalTooltip.add(new InfoTooltipBehavior());
        add(dumpIntervalTooltip);
    }


  
}
