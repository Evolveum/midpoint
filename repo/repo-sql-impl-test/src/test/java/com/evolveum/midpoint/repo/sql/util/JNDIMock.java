/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
