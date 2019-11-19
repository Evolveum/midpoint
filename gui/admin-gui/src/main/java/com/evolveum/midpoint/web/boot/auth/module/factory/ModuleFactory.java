/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth.module.factory;

import com.evolveum.midpoint.web.boot.auth.module.AuthModule;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * @author skublik
 */

public abstract class ModuleFactory {

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Autowired
    private AuthModuleRegistryImpl registry;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    public AuthModuleRegistryImpl getRegistry() {
        return registry;
    }

    public ObjectPostProcessor<Object> getObjectObjectPostProcessor() {
        return objectObjectPostProcessor;
    }

    public abstract boolean match(AbstractAuthenticationModuleType moduleType);

    public abstract AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                                  ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects) throws Exception;

    protected Integer getOrder(){
        return 0;
    }

    protected void setSharedObjects(HttpSecurity http, Map<Class<? extends Object>, Object> sharedObjects) {
        for (Map.Entry<Class<? extends Object>, Object> sharedObject : sharedObjects.entrySet()) {
            http.setSharedObject((Class<? super Object>) sharedObject.getKey(), sharedObject.getValue());
        }
    }

}
