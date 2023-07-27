/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

/**
 * @author lskublik
 */
@Component
public class AuthModuleRegistryImpl {

    private static final Trace LOGGER = TraceManager.getTrace(AuthModuleRegistryImpl.class);

    List<ModuleFactory> moduleFactories = new ArrayList<>();

    public void addToRegistry(ModuleFactory factory) {
        moduleFactories.add(factory);

        Comparator<? super ModuleFactory> comparator =
                (f1,f2) -> {

                    Integer f1Order = f1.getOrder();
                    Integer f2Order = f2.getOrder();

                    if (f1Order == null) {
                        if (f2Order != null) {
                            return 1;
                        }
                        return 0;
                    }

                    if (f2Order == null) {
                        return -1;
                    }

                    return Integer.compare(f1Order, f2Order);

                };

        moduleFactories.sort(comparator);

    }

    public <MT extends AbstractAuthenticationModuleType, MA extends ModuleAuthentication> ModuleFactory<MT, MA> findModuleFactory(
            AbstractAuthenticationModuleType configuration, AuthenticationChannel authenticationChannel) {

        Optional<ModuleFactory> opt = moduleFactories.stream().filter(f -> f.match(configuration, authenticationChannel)).findFirst();
        if (opt.isEmpty()) {
            LOGGER.trace("No factory found for {}", configuration);
            return null;
        }
        ModuleFactory factory = opt.get();
        LOGGER.trace("Found component factory {} for {}", factory, configuration);
        return factory;
    }

    public <T extends ModuleFactory> T findModuleFactoryByClass(Class<T> clazz) {

        T factory = (T) moduleFactories.stream()
                .filter(f -> f.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
//        if (opt.isEmpty()) {
//            LOGGER.trace("No factory found for class {}", clazz);
//            return null;
//        }
//        T factory = opt.get();
        LOGGER.trace("Found component factory {} for class {}", factory, clazz);
        return factory;
    }

}
