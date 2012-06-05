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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.resource.IPropertiesFactory;
import org.apache.wicket.resource.Properties;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.util.resource.locator.ResourceNameIterator;

import java.util.Locale;

/**
 * @author lazyman
 */
public class MidPointStringResourceLoader extends ComponentStringResourceLoader {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointStringResourceLoader.class);

    @Override
    public String loadStringResource(Class<?> clazz, final String key, final Locale locale,
            final String style, final String variation) {
        if (clazz == null) {
            return null;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("key: '{}'; class: '{}'; locale: '{}'; Style: '{}'; Variation: '{}'",
                    new Object[]{key, clazz.getName(), locale, style, variation});
        }

        // Load the properties associated with the path
        IPropertiesFactory propertiesFactory = getPropertiesFactory();
        while (true) {
            String path = clazz.getName().replace('.', '/');

            // Iterator over all the combinations
            ResourceNameIterator iter = newResourceNameIterator(path, locale, style, variation);
            while (iter.hasNext()) {
                String newPath = iter.next();

                Properties props = propertiesFactory.load(clazz, newPath);
                if (props != null) {
                    // Lookup the value
                    String value = props.getString(key);
                    if (value != null) {
                        return value;
                    }
                }
            }

            //try to load file from classpath (for custom packed locales)
            //file name equals class full qualified name + locale
            Properties props = propertiesFactory.load(clazz, clazz.getName());
            if (props != null) {
                // Lookup the value
                String value = props.getString(key);
                if (value != null) {
                    return value;
                }
            }

            // Didn't find the key yet, continue searching if possible
            if (isStopResourceSearch(clazz)) {
                break;
            }

            // Move to the next superclass
            clazz = clazz.getSuperclass();

            if (clazz == null) {
                // nothing more to search, done
                break;
            }
        }

        // not found
        return null;
    }
}
