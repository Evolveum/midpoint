/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Resolves the maximum number of multipart parts accepted in a submitted form.
 *
 * Explicit adminGuiConfiguration/formMaxMultiparts wins. Otherwise the Tomcat
 * server.tomcat.max-part-count is used, so the form limit never silently undercuts
 * the container limit, which Tomcat enforces before the form is processed.
 */
@Component
public class MultipartFormConfiguration implements SystemConfigurationChangeListener {

    private static final int DEFAULT_MAX_MULTIPART_COUNT = 100;

    private static int defaultMaxMultipartLimit = DEFAULT_MAX_MULTIPART_COUNT;
    private static Integer configuredMaxMultipartLimit;

    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    public MultipartFormConfiguration(@Value("${server.tomcat.max-part-count:}") Integer tomcatMaxPartCount) {
        defaultMaxMultipartLimit = tomcatMaxPartCount != null ? tomcatMaxPartCount : DEFAULT_MAX_MULTIPART_COUNT;
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        if (value == null) {
            return;
        }
        AdminGuiConfigurationType adminGuiConfig = value.getAdminGuiConfiguration();
        Integer configValue = adminGuiConfig != null ? adminGuiConfig.getFormMaxMultiparts() : null;
        configuredMaxMultipartLimit = configValue != null && configValue > 0 ? configValue : null;
    }

    public static int getMaxMultipartLimit() {
        return configuredMaxMultipartLimit != null ? configuredMaxMultipartLimit : defaultMaxMultipartLimit;
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }
}
