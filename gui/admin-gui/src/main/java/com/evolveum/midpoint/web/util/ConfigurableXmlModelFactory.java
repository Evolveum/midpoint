/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.springframework.core.io.Resource;
import ro.isdc.wro.model.factory.XmlModelFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConfigurableXmlModelFactory extends XmlModelFactory {

    private Resource resource;

    public ConfigurableXmlModelFactory(Resource resource) {
        this.resource = resource;
    }

    @Override
    protected InputStream getModelResourceAsStream() throws IOException {
        return resource.getInputStream();
    }
}
