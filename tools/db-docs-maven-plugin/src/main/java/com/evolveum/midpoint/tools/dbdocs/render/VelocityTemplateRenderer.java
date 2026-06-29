/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Renders classpath Velocity templates with a simple map-based context.
 */
class VelocityTemplateRenderer {

    private static final String TEMPLATE_ROOT = "dbdocs/templates/";

    private final VelocityEngine velocityEngine;

    /**
     * Creates a renderer configured for UTF-8 classpath templates.
     */
    VelocityTemplateRenderer() {
        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        properties.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        properties.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.name());
        properties.setProperty("output.encoding", StandardCharsets.UTF_8.name());

        velocityEngine = new VelocityEngine(properties);
        velocityEngine.init();
    }

    /**
     * Renders a template from the dbdocs template directory.
     */
    String render(String templateName, Map<String, Object> context) {
        VelocityContext velocityContext = new VelocityContext();
        context.forEach(velocityContext::put);

        StringWriter writer = new StringWriter();
        velocityEngine.getTemplate(TEMPLATE_ROOT + templateName, StandardCharsets.UTF_8.name())
                .merge(velocityContext, writer);
        return writer.toString();
    }
}
