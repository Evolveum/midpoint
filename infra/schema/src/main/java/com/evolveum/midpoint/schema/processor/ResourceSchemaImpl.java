/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.util.DOMUtil;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author semancik
 *
 */
public class ResourceSchemaImpl extends PrismSchemaImpl implements MutableResourceSchema {

    public ResourceSchemaImpl(String namespace, PrismContext prismContext) {
        super(namespace, prismContext);
    }

    private ResourceSchemaImpl(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
        super(DOMUtil.getSchemaTargetNamespace(element), prismContext);
        parseThis(element, shortDesc, prismContext);
    }

    private ResourceSchemaImpl(Element element, String namespace, String shortDesc, PrismContext prismContext) throws SchemaException {
        super(namespace, prismContext);
        parseThis(element, shortDesc, prismContext);
    }

    public static ResourceSchemaImpl parse(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
        return new ResourceSchemaImpl(element, shortDesc, prismContext);
    }

    public static ResourceSchemaImpl parse(Element element, String namespace, String shortDesc, PrismContext prismContext) throws SchemaException {
        return new ResourceSchemaImpl(element, namespace, shortDesc, prismContext);
    }

    @Override
    public void parseThis(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
        parseThis(element, true, shortDesc, prismContext);
    }

    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName) {
        ComplexTypeDefinition complexTypeDefinition = findComplexTypeDefinitionByType(qName);
        if (complexTypeDefinition == null) {
            return null;
        }
        if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
            return (ObjectClassComplexTypeDefinition)complexTypeDefinition;
        } else {
            throw new IllegalStateException("Expected the definition "+qName+" to be of type "+
                    ObjectClassComplexTypeDefinition.class+" but it was "+complexTypeDefinition.getClass());
        }
    }

    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent) {
        if (intent == null) {
            return findDefaultObjectClassDefinition(kind);
        }
        for (ObjectClassComplexTypeDefinition ocDef: getObjectClassDefinitions()) {
            if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && MiscSchemaUtil.equalsIntent(intent, ocDef.getIntent())) {
                return ocDef;
            }
        }
        return null;
    }

    @Override
    public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind) {
        for (ObjectClassComplexTypeDefinition ocDef: getObjectClassDefinitions()) {
            if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && ocDef.isDefaultInAKind()) {
                return ocDef;
            }
        }
        return null;
    }


    /**
     * Creates a new resource object definition and adds it to the schema.
     *
     * This is a preferred way how to create definition in the schema.
     *
     * @param localTypeName
     *            type name "relative" to schema namespace
     * @return new resource object definition
     */
    @Override
    public MutableObjectClassComplexTypeDefinition createObjectClassDefinition(String localTypeName) {
        QName typeName = new QName(getNamespace(), localTypeName);
        return createObjectClassDefinition(typeName);
    }

    /**
     * Creates a new resource object definition and adds it to the schema.
     *
     * This is a preferred way how to create definition in the schema.
     *
     * @param typeName
     *            type QName
     * @return new resource object definition
     */
    @Override
    public MutableObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName) {
        ObjectClassComplexTypeDefinitionImpl cTypeDef = new ObjectClassComplexTypeDefinitionImpl(typeName, getPrismContext());
        add(cTypeDef);
        return cTypeDef;
    }

    @Override
    public MutableResourceSchema toMutable() {
        return this;
    }

}
