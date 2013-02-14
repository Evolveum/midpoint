/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
