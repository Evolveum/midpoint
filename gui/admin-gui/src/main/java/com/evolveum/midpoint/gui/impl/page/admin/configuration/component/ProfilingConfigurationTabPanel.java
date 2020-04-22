/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.ProfilingClassLoggerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;

/**
 * @author skublik
 * @deprecated
 *
 * rework to use special wrapper - smth like profiling wrapper which will prepare all the predefined values, profiling level etc.
 */

public class ProfilingConfigurationTabPanel extends BasePanel<PrismContainerWrapper<ProfilingConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationTabPanel.class);

    private static final String ID_PROFILING_ENABLED_NOTE = "profilingEnabledNote";
    private static final String ID_PROFILING = "profiling";
    private static final String ID_PROFILING_LOGGER = "profilingLogger";
    private static final String ID_PROFILING_LOGGER_APPENDERS = "profilingLoggerAppenders";
    private static final String ID_PROFILING_LOGGER_LEVEL = "profilingLoggerLevel";

    private IModel<PrismContainerWrapper<LoggingConfigurationType>> loggingModel;

    public ProfilingConfigurationTabPanel(String id, IModel<PrismContainerWrapper<ProfilingConfigurationType>> profilingModel,
            IModel<PrismContainerWrapper<LoggingConfigurationType>> loggingModel) {
        super(id, profilingModel);
        this.loggingModel = loggingModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private IModel<PrismContainerWrapper<LoggingConfigurationType>> getLoggingModel() {
        return loggingModel;
    }

    private IModel<PrismContainerWrapper<ProfilingConfigurationType>> getProfilingModel() {
        return getModel();
    }

    protected void initLayout() {

        WebMarkupContainer profilingEnabledNote = new WebMarkupContainer(ID_PROFILING_ENABLED_NOTE);
        profilingEnabledNote.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getPageBase().getMidpointConfiguration().getProfilingMode() == ProfilingMode.OFF;
            }
        });
        add(profilingEnabledNote);

        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().showOnTopLevel(true);
            Panel panel = getPageBase().initItemPanel(ID_PROFILING, ProfilingConfigurationType.COMPLEX_TYPE, getProfilingModel(), builder.build());
            add(panel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create profiling panel. Reason: {}", e.getMessage(), e);
            getSession().error("Cannot create profiling panel.");
        }

        PrismContainerWrapperModel<LoggingConfigurationType, ClassLoggerConfigurationType> profilingLogger = PrismContainerWrapperModel.fromContainerWrapper(getLoggingModel(), ItemPath.create(ProfilingClassLoggerWrapperFactoryImpl.PROFILING_LOGGER_PATH));
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().showOnTopLevel(true);
            Panel logger = getPageBase().initItemPanel(ID_PROFILING_LOGGER, ProfilingClassLoggerWrapperFactoryImpl.PROFILING_LOGGER_PATH, profilingLogger, builder.build());
            add(logger);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create profiling panel. Reason: {}", e.getMessage(), e);
            getSession().error("Cannot create profiling panel.");
        }

    }
}
