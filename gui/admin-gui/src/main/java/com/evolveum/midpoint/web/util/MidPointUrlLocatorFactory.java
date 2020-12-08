/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.springframework.core.env.PropertyResolver;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import ro.isdc.wro.extensions.locator.WebjarUriLocator;
import ro.isdc.wro.extensions.locator.WebjarsUriLocator;
import ro.isdc.wro.model.resource.locator.ClasspathUriLocator;
import ro.isdc.wro.model.resource.locator.ServletContextUriLocator;
import ro.isdc.wro.model.resource.locator.StandaloneServletContextUriLocator;
import ro.isdc.wro.model.resource.locator.factory.SimpleUriLocatorFactory;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointUrlLocatorFactory extends SimpleUriLocatorFactory {

    public MidPointUrlLocatorFactory(PropertyResolver propertyResolver) {
        addLocator(new ClasspathUriLocator());
        addLocator(new WebjarsUriLocator());
        addLocator(new ServletContextUriLocator());
        addLocator(new PropertyUrlUriLocator(propertyResolver));
    }
}
