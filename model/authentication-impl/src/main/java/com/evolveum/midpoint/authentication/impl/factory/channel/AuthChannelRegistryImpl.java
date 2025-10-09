/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.channel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lskublik
 */
@Component
public class AuthChannelRegistryImpl {

    private static final Trace LOGGER = TraceManager.getTrace(AuthChannelRegistryImpl.class);

    List<AbstractChannelFactory> moduleFactories = new ArrayList<>();

    public void addToRegistry(AbstractChannelFactory factory) {
        moduleFactories.add(factory);

        Comparator<? super AbstractChannelFactory> comparator =
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

    public AbstractChannelFactory findModelFactory(String channelId) {

        Optional<AbstractChannelFactory> opt = moduleFactories.stream().filter(f -> f.match(channelId)).findFirst();
        if (opt.isEmpty()) {
            LOGGER.trace("No factory found for {}", channelId);
            return null;
        }
        AbstractChannelFactory factory = opt.get();
        LOGGER.trace("Found component factory {} for {}", factory, channelId);
        return factory;
    }

}
