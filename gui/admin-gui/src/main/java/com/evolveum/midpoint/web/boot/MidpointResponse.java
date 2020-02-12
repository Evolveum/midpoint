/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.model.common.SystemObjectCache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.catalina.connector.OutputBuffer;
import org.apache.catalina.connector.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */

public class MidpointResponse extends Response {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointResponse.class);

    private String servletPath;
    private SystemObjectCache systemObjectCache;

    public MidpointResponse(String serlvetPath, SystemObjectCache systemObjectCache) {
        this(OutputBuffer.DEFAULT_BUFFER_SIZE, serlvetPath, systemObjectCache);
    }

    public MidpointResponse(int outputBufferSize, String serlvetPath, SystemObjectCache systemObjectCache) {
        super(outputBufferSize);

        this.servletPath = serlvetPath;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public void setHeader(String name, String value) {
        String pattern = getPattern();
        if ("Location".equals(name) && pattern != null && StringUtils.isNotBlank(value)) {
            if (value.startsWith(".")) {
                value = pattern + value.substring(1);
            } else if (StringUtils.isBlank(servletPath)) {
                if(value.startsWith("/")) {
                    value = pattern + value;
                } else {
                    String partAfterSchema = value.substring(value.indexOf("://") + 3);
                    value = pattern + partAfterSchema.substring(partAfterSchema.indexOf("/"));
                }
            } else if (value.contains(servletPath + "/")) {
                value = pattern + value.substring(value.indexOf(servletPath) + servletPath.length());
            }
        }
        super.setHeader(name, value);
    }

    private String getPattern() {
        String pattern = null;
        try {
            PrismObject<SystemConfigurationType> systemConfig = systemObjectCache.getSystemConfiguration(new OperationResult("load system configuration"));
            String publicHttpUrlPattern = SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfig.asObjectable());
            if (publicHttpUrlPattern != null) {
                pattern = publicHttpUrlPattern;
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
        }
        return pattern;
    }

}
