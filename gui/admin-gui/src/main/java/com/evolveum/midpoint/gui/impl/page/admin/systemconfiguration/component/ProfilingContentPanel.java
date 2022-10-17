/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.factory.wrapper.ProfilingClassLoggerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@PanelType(name = "profilingPanel")
@PanelInstance(
        identifier = "profilingPanel",
        applicableForType = ProfilingConfigurationType.class,
        display = @PanelDisplay(
                label = "ProfilingConfiguration.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
public class ProfilingContentPanel extends AbstractObjectMainPanel<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingContentPanel.class);

    private static final String ID_MAIN_PANEL = "mainPanel";

    private static final String ID_PROFILING_LOGGER = "profilingLogger";

    public ProfilingContentPanel(String id, AssignmentHolderDetailsModel<SystemConfigurationType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        if (getPageBase().getMidpointConfiguration().getProfilingMode() == ProfilingMode.OFF) {
            warn(getString("ProfilingConfigPanel.profilingMustBeEnabled"));
        }

        SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(SystemConfigurationType.F_PROFILING_CONFIGURATION)),
                ProfilingConfigurationType.COMPLEX_TYPE) {

            @Override
            protected ItemMandatoryHandler getMandatoryHandler() {
                return (itemWrapper -> false);
            }
        };
        add(panel);

        PrismContainerWrapperModel<SystemConfigurationType, ClassLoggerConfigurationType> profilingLogger =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(),
                        ItemPath.create(SystemConfigurationType.F_LOGGING, ProfilingClassLoggerWrapperFactoryImpl.PROFILING_LOGGER_PATH));
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder();
            Panel logger = getPageBase().initItemPanel(ID_PROFILING_LOGGER, ProfilingClassLoggerWrapperFactoryImpl.PROFILING_LOGGER_PATH, profilingLogger, builder.build());
            add(logger);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create profiling panel. Reason: {}", e.getMessage(), e);
            getSession().error("Cannot create profiling panel.");
        }
    }
}
