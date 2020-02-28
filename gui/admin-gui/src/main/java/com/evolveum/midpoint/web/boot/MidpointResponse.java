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

    public MidpointResponse(String servletPath, SystemObjectCache systemObjectCache) {
        this(OutputBuffer.DEFAULT_BUFFER_SIZE, servletPath, systemObjectCache);
    }

    public MidpointResponse(int outputBufferSize, String servletPath, SystemObjectCache systemObjectCache) {
        super(outputBufferSize);

        this.servletPath = servletPath;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public void setHeader(String name, String value) {
        if ("Location".equals(name)) {
            String publicUrlPrefix = getPublicUrlPrefix();
            if (publicUrlPrefix != null && StringUtils.isNotBlank(value)) {
                if (value.startsWith(".")) {
                    value = publicUrlPrefix + value.substring(1);
                } else if (StringUtils.isBlank(servletPath)) {
                    if (value.startsWith("/")) {
                        value = publicUrlPrefix + value;
                    } else {
                        String partAfterSchema = value.substring(value.indexOf("://") + 3);
                        value = publicUrlPrefix + partAfterSchema.substring(partAfterSchema.indexOf("/"));
                    }
                } else if (value.contains(servletPath + "/")) {
                    value = publicUrlPrefix + value.substring(value.indexOf(servletPath) + servletPath.length());
                }
            }
        }
        super.setHeader(name, value);
    }

    private String getPublicUrlPrefix() {
        try {
            PrismObject<SystemConfigurationType> systemConfig = systemObjectCache.getSystemConfiguration(new OperationResult("load system configuration"));
            return SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfig.asObjectable(), getRequest().getServerName());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
            return null;
        }
    }
}
