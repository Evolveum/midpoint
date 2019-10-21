/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.general;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class GcpConfigurationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(GcpConfigurationHelper.class);

    @Autowired
    private ConfigurationHelper configurationHelper;

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    @Autowired
    private PrismContext prismContext;

}
