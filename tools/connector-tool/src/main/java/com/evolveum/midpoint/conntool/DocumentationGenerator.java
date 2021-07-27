/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.conntool;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.identityconnectors.framework.api.*;

import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Generates a documentation for selected connector JAR file(s).
 */
public class DocumentationGenerator {

    private static final String VAR_INFO = "info";
    private static final String VAR_PROPERTY_NAMES = "propertyNames";
    private static final String VAR_PROPERTIES = "properties";

    private final Context context;

    @SuppressWarnings({ "FieldCanBeLocal", "unused" }) // currently unused
    private final GenerateDocumentationOptions commandOptions;

    DocumentationGenerator(Context context, GenerateDocumentationOptions commandOptions) {
        this.context = context;
        this.commandOptions = commandOptions;
    }

    void generate() throws IOException {
        File connectorFile = context.getCommonOptions().getConnectorFile();
        if (connectorFile == null) {
            System.err.println("Connector file must be specified");
            return;
        }

        URL connectorFileUrl;
        try {
            connectorFileUrl = connectorFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        }

        ConnectorInfoManagerFactory connectorInfoManagerFactory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager localManager = connectorInfoManagerFactory.getLocalManager(connectorFileUrl);
        List<ConnectorInfo> connectorInfos = localManager.getConnectorInfos();

        // TODO output to a place other than System.out?
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
            for (ConnectorInfo info : connectorInfos)
                processConnectorInfo(info, writer);
        }
    }

    private void processConnectorInfo(ConnectorInfo info, OutputStreamWriter writer) {
        // TODO template from file
        // TODO dump the parameters more nicely
        String template =
                "Found connector info:\n"
                        + " - display name: $info.connectorDisplayName\n"
                        + " - key: $info.connectorKey\n"
                        + " - category: $info.connectorCategory\n"
                        + " - configuration property names: $propertyNames\n"
                        + " - configuration properties: $properties\n";
        VelocityContext context = createVelocityContext(info);
        Velocity.evaluate(context, writer, "", template);
    }

    private VelocityContext createVelocityContext(ConnectorInfo info) {
        VelocityContext velocityCtx = new VelocityContext();
        velocityCtx.put(VAR_INFO, info);

        APIConfiguration configuration = info.createDefaultAPIConfiguration();
        ConfigurationProperties configurationProperties = configuration.getConfigurationProperties();
        List<String> propertyNames = configurationProperties != null ? configurationProperties.getPropertyNames() : List.of();
        velocityCtx.put(VAR_PROPERTY_NAMES, propertyNames);

        List<ConnectorConfigurationProperty> properties = propertyNames.stream()
                .map(Objects.requireNonNull(configurationProperties)::getProperty)
                .map(ConnectorConfigurationProperty::new)
                .collect(Collectors.toList());
        velocityCtx.put(VAR_PROPERTIES, properties);

        return velocityCtx;
    }
}
