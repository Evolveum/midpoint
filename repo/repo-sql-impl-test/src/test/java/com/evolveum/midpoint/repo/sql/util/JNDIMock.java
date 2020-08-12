/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import java.util.Map;
import javax.naming.NamingException;

import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Class used to fake JNDI Context in tests.
 *
 * @author lazyman
 */
public class JNDIMock {

    private static final Trace LOGGER = TraceManager.getTrace(JNDIMock.class);

    public void setObjects(Map<String, Object> objects) throws NamingException {
        LOGGER.info("Loading fake JNDI context.");

        // It's recommended to use something like <a href="https://github.com/h-thurow/Simple-JNDI">Simple-JNDI</a>
        // in the future, but it seems a bit more complicated, so we will suffer the deprecation while it lasts.
        if (SimpleNamingContextBuilder.getCurrentContextBuilder() != null) {
            SimpleNamingContextBuilder.getCurrentContextBuilder().deactivate();
        }

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        if (objects != null) {
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                LOGGER.info("Registering '{}' with value '{}'", entry.getKey(), entry.getValue());
                builder.bind(entry.getKey(), entry.getValue());
            }
        }

        builder.activate();
    }
}
