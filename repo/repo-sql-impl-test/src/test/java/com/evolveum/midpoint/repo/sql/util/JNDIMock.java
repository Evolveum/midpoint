/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import javax.naming.NamingException;
import java.util.Map;

/**
 * Class used to fake JNDI Context in tests.
 *
 * @author lazyman
 */
public class JNDIMock {

    private static final Trace LOGGER = TraceManager.getTrace(JNDIMock.class);

    public Map<String, Object> getObjects() {
        return null;
    }

    public void setObjects(Map<String, Object> objects) throws NamingException {
        LOGGER.info("Loading fake JNDI context.");

        if (SimpleNamingContextBuilder.getCurrentContextBuilder() != null) {
            SimpleNamingContextBuilder.getCurrentContextBuilder().deactivate();
        }

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        if (objects != null) {
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                LOGGER.info("Registering '{}' with value '{}'", new Object[]{entry.getKey(), entry.getValue()});
                builder.bind(entry.getKey(), entry.getValue());
            }
        }

        builder.activate();
    }
}
