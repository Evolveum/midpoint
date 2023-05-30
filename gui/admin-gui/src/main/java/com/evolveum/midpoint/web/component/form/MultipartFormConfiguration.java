/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class MultipartFormConfiguration implements SystemConfigurationChangeListener {


    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private static final int DEFAULT_MAX_MULTIPART_COUNT = 100;
    private static Integer maxMultipartLimit = DEFAULT_MAX_MULTIPART_COUNT;

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        if (value == null) {
            return;
        }
        AdminGuiConfigurationType adminGuiConfig = value.getAdminGuiConfiguration();
        Integer configValue = null;
        if (adminGuiConfig != null) {
            configValue = adminGuiConfig.getFormMaxMultiparts();
        }
        if (configValue != null && configValue > 0) {
            maxMultipartLimit = configValue;
        } else {
            maxMultipartLimit = DEFAULT_MAX_MULTIPART_COUNT;
        }
    }

    public static int getMaxMultipartsLimit() {
        return maxMultipartLimit != null ? maxMultipartLimit : DEFAULT_MAX_MULTIPART_COUNT;
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
