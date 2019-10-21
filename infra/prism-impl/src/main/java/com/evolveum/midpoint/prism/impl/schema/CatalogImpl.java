/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.schema;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.resolver.Catalog;

import java.net.URI;

/**
 *
 */

@SuppressWarnings("unused")     // Referenced in MidPointSpringApplication by name.
public class CatalogImpl extends Catalog {

    private static final Trace LOGGER = TraceManager.getTrace(CatalogImpl.class);

    /**
     * This fixes catalog items. When launched as spring boot fat jar, catalog by default resolve URIs like
     * <p>
     * jar:file:/SOME_ABSOLUTE_PATH/midpoint.war!/WEB-INF/lib/schema-3.7-SNAPSHOT.jar!/META-INF/../xml/ns/public/common/common-core-3.xsd
     * <p>
     * which looks at first sight, but correct working version is:
     * <p>
     * jar:file:/SOME_ABSOLUTE_PATH/midpoint.war!/WEB-INF/lib/schema-3.7-SNAPSHOT.jar!/xml/ns/public/common/common-core-3.xsd
     * <p>
     * This catalog impl is enabled only when in spring boot fat jar is launched through main() using:
     * <p>
     * System.setProperty("xml.catalog.className", CatalogImpl.class.getName());
     */
    @Override
    protected String makeAbsolute(String sysid) {
        String absolute = super.makeAbsolute(sysid);

        if (absolute == null) {
            return null;
        }

        String[] array = absolute.split("!/");
        if (array.length <= 1) {
            return absolute;
        }

        String[] normalized = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            String part = array[i];

            URI uri = java.net.URI.create(part);
            uri = uri.normalize();

            normalized[i] = uri.toString();
        }

        String newAbsolute = StringUtils.join(normalized, "!/");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Normalized absolute path from '{}' to '{}'", absolute, newAbsolute);
        }

        return newAbsolute;
    }
}
