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

import org.apache.commons.lang.Validate;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.PropertyPlaceholderHelper;
import ro.isdc.wro.model.resource.locator.UrlUriLocator;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PropertyUrlUriLocator extends UrlUriLocator {

    private PropertyResolver propertyResolver;

    public PropertyUrlUriLocator(PropertyResolver propertyResolver) {
        Validate.notNull(propertyResolver, "Property resolver must not be null");

        this.propertyResolver = propertyResolver;
    }

    @Override
    public boolean accept(String uri) {
        String newUri = replaceProperties(uri);

        return super.accept(newUri);
    }

    @Override
    public InputStream locate(String uri) throws IOException {
        String newUri = replaceProperties(uri);

        return super.locate(newUri);
    }

    private String replaceProperties(String uri) {
        if (uri == null) {
            return null;
        }

        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", ":", true);
        return helper.replacePlaceholders(uri, new PropertyPlaceholderHelper.PlaceholderResolver() {

            @Override
            public String resolvePlaceholder(String placeholderName) {
                return propertyResolver.getProperty(placeholderName);
            }
        });
    }
}
