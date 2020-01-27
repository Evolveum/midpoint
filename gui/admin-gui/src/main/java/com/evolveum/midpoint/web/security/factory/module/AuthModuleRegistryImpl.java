/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.module;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lskublik
 */
@Component
public class AuthModuleRegistryImpl {

    private static final Trace LOGGER = TraceManager.getTrace(AuthModuleRegistryImpl.class);

    List<AbstractModuleFactory> moduleFactories = new ArrayList<>();

    public void addToRegistry(AbstractModuleFactory factory) {
        moduleFactories.add(factory);

        Comparator<? super AbstractModuleFactory> comparator =
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
                        if (f1Order != null) {
                            return -1;
                        }
                    }

                    return Integer.compare(f1Order, f2Order);

                };

        moduleFactories.sort(comparator);

    }

    public AbstractModuleFactory findModelFactory(AbstractAuthenticationModuleType configuration) {

        Optional<AbstractModuleFactory> opt = moduleFactories.stream().filter(f -> f.match(configuration)).findFirst();
        if (!opt.isPresent()) {
            LOGGER.trace("No factory found for {}", configuration);
            return null;
        }
        AbstractModuleFactory factory = opt.get();
        LOGGER.trace("Found component factory {} for {}", factory, configuration);
        return factory;
    }

}
