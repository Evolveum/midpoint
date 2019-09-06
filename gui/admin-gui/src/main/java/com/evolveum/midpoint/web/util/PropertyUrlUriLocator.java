/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
