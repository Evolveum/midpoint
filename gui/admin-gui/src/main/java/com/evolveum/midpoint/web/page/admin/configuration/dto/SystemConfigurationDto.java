package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {

    private PrismObject<SystemConfigurationType> config;

    public SystemConfigurationDto(PrismObject<SystemConfigurationType> config) {
        this.config = config;
    }
}
