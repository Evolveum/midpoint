/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.factory.wrapper.ProfilingClassLoggerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ClassLoggersMenuLinkCounter extends SimpleCounter<AssignmentHolderDetailsModel<SystemConfigurationType>, SystemConfigurationType> {

    public ClassLoggersMenuLinkCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SystemConfigurationType> model, PageBase pageBase) {
        SystemConfigurationType object = model.getObjectType();
        LoggingConfigurationType lg = object.getLogging();

        if (lg == null) {
            return 0;
        }

        return (int) lg.getClassLogger().stream().filter(l -> !ProfilingClassLoggerWrapperFactoryImpl.LOGGER_PROFILING.equals(l.getPackage())).count();
    }
}

