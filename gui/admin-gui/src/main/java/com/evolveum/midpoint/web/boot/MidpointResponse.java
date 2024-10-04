/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class MidpointResponse extends Response {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointResponse.class);

    private String contextPath;
    private SystemObjectCache systemObjectCache;

    public MidpointResponse(String servletPath, SystemObjectCache systemObjectCache) {
        this(OutputBuffer.DEFAULT_BUFFER_SIZE, servletPath, systemObjectCache);
    }

    public MidpointResponse(int outputBufferSize, String contextPath, SystemObjectCache systemObjectCache) {
        super(outputBufferSize);

        this.contextPath = contextPath;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public void setHeader(String name, String value) {
        if ("Location".equals(name)) {
            String publicUrlPrefix = getPublicUrlPrefix();
            if (publicUrlPrefix != null && StringUtils.isNotBlank(value)) {
                if (value.startsWith("..")) {
                    String path = getRequest().getServletPath().substring(0, getRequest().getServletPath().lastIndexOf("/"));
                    while (value.startsWith("..")) {
                        if (!StringUtils.isEmpty(path)) {
                            path = path.substring(0, path.lastIndexOf("/"));
                        }
                        value = value.substring(3);
                    }
                    value = publicUrlPrefix + path + "/" + value;
                } else if (value.startsWith(".")) {
                    List<String> segments = Arrays.asList(getRequest().getServletPath().substring(1).split("/"));
                    if (segments.size() <= 1) {
                        value = publicUrlPrefix + value.substring(1);
                    } else {
                        value = publicUrlPrefix + getRequest().getServletPath().substring(0, getRequest().getServletPath().lastIndexOf("/")) + value.substring(1);
                    }
                } else if (StringUtils.isBlank(contextPath)) {
                    if (value.startsWith("/")) {
                        value = publicUrlPrefix + value;
                    } else if (isUrlThisApplication(value)){
                        String partAfterSchema = value.substring(value.indexOf("://") + 3);
                        value = publicUrlPrefix + partAfterSchema.substring(partAfterSchema.indexOf("/"));
                    }
                } else if (value.contains(contextPath + "/")) {
                    if (value.startsWith(contextPath)) {
                        value = publicUrlPrefix + value.substring(contextPath.length());
                    } else if (value.startsWith("/")) {
                        value = publicUrlPrefix + value;
                    } else if (isUrlThisApplication(value)) {
                        String partAfterHostname = value.substring(value.indexOf("://") + 3);
                        partAfterHostname = partAfterHostname.substring(partAfterHostname.indexOf("/"));
                        value = publicUrlPrefix +
                                partAfterHostname.substring(
                                        partAfterHostname.indexOf(contextPath) + contextPath.length());
                    }
                }
            }
        }
        super.setHeader(name, value);
    }

    private boolean isUrlThisApplication(String url) {
        if (StringUtils.isEmpty(url)) {
            return true;
        }
        String applicationUrl = ServletUriComponentsBuilder.fromContextPath(this.getRequest()).build().toUriString();
        if (StringUtils.isEmpty(applicationUrl)) {
            return true;
        }
        return url.startsWith(applicationUrl);
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
