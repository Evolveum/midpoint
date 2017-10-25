/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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