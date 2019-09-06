/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.webjars.urlprotocols.JarUrlProtocolHandler;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Workaround class for wro4j resources loading until https://github.com/webjars/webjars-locator/issues/104 is fixed.
 * <p>
 * Created by Viliam Repan (lazyman).
 */
public class CustomJarUrlProtocolHandler extends JarUrlProtocolHandler {

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        if (url.toString().startsWith("jar:war:file:")) {
            return Collections.emptySet();
        }

        return super.getAssetPaths(url, filterExpr, classLoaders);
    }
}
