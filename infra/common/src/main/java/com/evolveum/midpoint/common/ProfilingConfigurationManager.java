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

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *  On this place, we take care about profilingConfiguration part of midPoint systemConfiguration
 *
 *  @author shood
 * */
public class ProfilingConfigurationManager {

    /* Class Attributes */
    private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationManager.class);
    private static final String DOT_CLASS = ProfilingConfigurationManager.class + ".";

    private static final String REQUEST_FILTER_LOGGER_CLASS_NAME = "com.evolveum.midpoint.web.util.MidPointProfilingServletFilter";
    private static final String APPENDER_IDM_PROFILE = "IDM-PROFILE_LOG";

    /* Object Attributes */

    /* Methods - STATIC */

    /**
     *  In this method, we perform the check of systemConfiguration object, searching for any changes
     *  related to profilingConfiguration
     * */
    public static LoggingConfigurationType checkSystemProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism){
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();
        ProfilingConfigurationType profilingConfig = systemConfig.getProfilingConfiguration();

        if(profilingConfig == null || !profilingConfig.isEnabled())
            return systemConfig.getLogging();
        else
            return applyProfilingConfiguration(systemConfigurationPrism, profilingConfig);
    }   //checkSystemProfilingConfiguration

    /**
     *  Checks systemConfig profiling configuration and performs necessary actions
     * */
    private static LoggingConfigurationType applyProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism, ProfilingConfigurationType profilingConfig){
        //LOGGER.info("Entering applyProfilingConfiguration()");
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();

        LoggingConfigurationType loggingConfig = systemConfig.getLogging();

        if(loggingConfig != null){
            //LOGGER.info("entering profiling config applyProfilingConfiguration()");
            if(profilingConfig.isRequestFilter()){
                ClassLoggerConfigurationType requestFilterLogger = new ClassLoggerConfigurationType();
                requestFilterLogger.setPackage(REQUEST_FILTER_LOGGER_CLASS_NAME);
                requestFilterLogger.setLevel(LoggingLevelType.TRACE);
                requestFilterLogger.getAppender().clear();
                requestFilterLogger.getAppender().add(APPENDER_IDM_PROFILE);
                LOGGER.info("Number of Appenders for ServletFilter Logger: " + requestFilterLogger.getAppender().size());

                loggingConfig.getClassLogger().add(requestFilterLogger);
            }
        }

        LOGGER.info("Applying profiling configuration.");
        return  loggingConfig;
    }   //applyProfilingConfiguration end


}   //ProfilingConfigurationManager end
