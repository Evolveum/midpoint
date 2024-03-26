/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public class ConnectorSchemaImpl extends PrismSchemaImpl implements ConnectorSchema {

    /** Please use only from {@link ConnectorSchemaFactory}. */
    ConnectorSchemaImpl(String namespace) {
        super(namespace);
    }

    /** Please use only from {@link ConnectorSchemaFactory}. */
    ConnectorSchemaImpl(Element element, String shortDesc) throws SchemaException {
        this(DOMUtil.getSchemaTargetNamespace(element));
        SchemaParsingUtil.parse(this, element, true, shortDesc, false);
        fixConnectorConfigurationDefinition();
    }

    /**
     * Make sure that the connector configuration definition has a correct compile-time class name and maxOccurs setting:
     *
     * . For the compile-time class: the type is {@link #CONNECTOR_CONFIGURATION_TYPE_LOCAL_NAME} (ConnectorConfigurationType),
     * but the standard schema parser does not know what compile time class to use. So we need to fix it here.
     *
     * . For the maxOccurs, it is currently not being serialized for the top-level schema items. So we must fix that here.
     */
    private void fixConnectorConfigurationDefinition() throws SchemaException {
        var configurationContainerDefinition = getConnectorConfigurationContainerDefinition();
        configurationContainerDefinition.mutator().setCompileTimeClass(ConnectorConfigurationType.class);
        configurationContainerDefinition.mutator().setMaxOccurs(1);
    }

    @Override
    public String getUsualNamespacePrefix() {
        // This functionality was either never present or was disabled at some point in time.
        // If needed, the original (unused) code can be recovered from the commit that introduced this comment.
        return null;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ConnectorSchemaImpl clone() {
        ConnectorSchemaImpl clone = new ConnectorSchemaImpl(namespace);
        super.copyContent(clone);
        return clone;
    }
}
