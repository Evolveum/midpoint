/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;

public class SchemaPropertyWrapperImpl extends PrismPropertyValueWrapper<SchemaDefinitionType> {

    public static final String F_NAMESPACE = "namespace";

    private String oldValue;

    private String namespace;
    private Collection<? extends Definition> definitions;

    public SchemaPropertyWrapperImpl(PrismPropertyWrapper parent, PrismPropertyValue<SchemaDefinitionType> value, ValueStatus status) {
        super(parent, null, status);
        //String oldValue, PrismSchema newValue
        SchemaDefinitionType schemaDefinitionType = value.getValue();
        if (schemaDefinitionType == null) {
            return;
        }
        Element schemaElement = schemaDefinitionType.getSchema();
        try {
            PrismSchemaImpl newValue = SchemaParsingUtil.createAndParse(
                    schemaElement, true, "schema", false);
            this.namespace = newValue.getNamespace();
            this.definitions = newValue.getDefinitions();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        this.oldValue = DOMUtil.serializeDOMToString(schemaElement);

    }

    @Override
    public PrismPropertyValue<SchemaDefinitionType> getOldValue() {
        Element parsedSchema = DOMUtil.getFirstChildElement(DOMUtil.parseDocument(oldValue));
        SchemaDefinitionType schemaDefinitionType = new SchemaDefinitionType();
        schemaDefinitionType.setSchema(parsedSchema);
        return PrismContext.get().itemFactory().createPropertyValue(schemaDefinitionType);
    }

    @Override
    public PrismPropertyValue<SchemaDefinitionType> getNewValue() {
        PrismSchema prismSchema = getParsedSchema();
        if (prismSchema == null) {
            return PrismContext.get().itemFactory().createPropertyValue();
        }

        Document doc;
        try {
            doc = prismSchema.serializeToXsd();
        } catch (SchemaException e) {
            // TODO proper error handling
            System.out.println("error while serializing schema");
            throw new RuntimeException(e);
        }
        Element parsedSchema = DOMUtil.getFirstChildElement(doc);
        SchemaDefinitionType schemaDefinitionType = new SchemaDefinitionType();
        schemaDefinitionType.setSchema(parsedSchema);
        return PrismContext.get().itemFactory().createPropertyValue(schemaDefinitionType);
    }


    public PrismSchema getParsedSchema() {
        if (StringUtils.isBlank(namespace)) {
            return null;
        }
        PrismSchemaImpl parsedSchema = new PrismSchemaImpl(namespace);
        definitions.forEach(parsedSchema::add);
        return parsedSchema;
    }

    @Override
    protected boolean isChanged() {
        return !getOldValue().equals(getNewValue(), EquivalenceStrategy.LITERAL);
    }
}
