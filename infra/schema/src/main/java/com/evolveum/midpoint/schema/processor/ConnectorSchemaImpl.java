/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public class ConnectorSchemaImpl extends PrismSchemaImpl implements ConnectorSchema {

    private ConnectorSchemaImpl(String namespace) {
        super(namespace);
    }

    private ConnectorSchemaImpl(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
        super(DOMUtil.getSchemaTargetNamespace(element));
        parseThis(element, true, shortDesc, prismContext);
    }

    public static ConnectorSchemaImpl parse(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
        return new ConnectorSchemaImpl(element, shortDesc, prismContext);
    }

    @Override
    public Collection<ResourceObjectClassDefinition> getObjectClassDefinitions() {
        return getDefinitions(ResourceObjectClassDefinition.class);
    }

    @Override
    public ResourceObjectClassDefinition findObjectClassDefinition(QName qName) {
        ComplexTypeDefinition complexTypeDefinition = findComplexTypeDefinitionByType(qName);
        if (complexTypeDefinition == null) {
            return null;
        }
        if (complexTypeDefinition instanceof ResourceObjectClassDefinition resourceObjectClassDefinition) {
            return resourceObjectClassDefinition;
        } else {
            throw new IllegalStateException("Expected the definition "+qName+" to be of type "+
                    ResourceObjectClassDefinition.class+" but it was "+complexTypeDefinition.getClass());
        }
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
