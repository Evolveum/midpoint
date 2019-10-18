/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

/**
 * @author mederly
 */
public class PrismParserImplNoIO extends PrismParserImpl implements PrismParserNoIO {

    public PrismParserImplNoIO(ParserSource source, String language, ParsingContext context, PrismContextImpl prismContext,
            ItemDefinition<?> itemDefinition, QName itemName, QName dataType, Class<?> dataClass) {
        super(source, language, context, prismContext, itemDefinition, itemName, dataType, dataClass);
    }

    @NotNull
    @Override
    public PrismParserNoIO language(@Nullable String language) {
        return (PrismParserNoIO) super.language(language);
    }

    @NotNull
    @Override
    public PrismParserNoIO xml() {
        return (PrismParserNoIO) super.xml();
    }

    @NotNull
    @Override
    public PrismParserNoIO json() {
        return (PrismParserNoIO) super.json();
    }

    @NotNull
    @Override
    public PrismParserNoIO yaml() {
        return (PrismParserNoIO) super.yaml();
    }

    @NotNull
    @Override
    public PrismParserNoIO context(@NotNull ParsingContext context) {
        return (PrismParserNoIO) super.context(context);
    }

    @NotNull
    @Override
    public PrismParserNoIO strict() {
        return (PrismParserNoIO) super.strict();
    }

    @NotNull
    @Override
    public PrismParserNoIO compat() {
        return (PrismParserNoIO) super.compat();
    }

    @NotNull
    @Override
    public PrismParserNoIO definition(ItemDefinition<?> itemDefinition) {
        return (PrismParserNoIO) super.definition(itemDefinition);
    }

    @NotNull
    @Override
    public PrismParserNoIO name(QName itemName) {
        return (PrismParserNoIO) super.name(itemName);
    }

    @NotNull
    @Override
    public PrismParserNoIO type(QName typeName) {
        return (PrismParserNoIO) super.type(typeName);
    }

    @NotNull
    @Override
    public PrismParserNoIO type(Class<?> typeClass) {
        return (PrismParserNoIO) super.type(typeClass);
    }

    @NotNull
    @Override
    public <O extends Objectable> PrismObject<O> parse() throws SchemaException {
        try {
            return doParse();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> parseItem() throws SchemaException {
        try {
            return doParseItem();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <IV extends PrismValue> IV parseItemValue() throws SchemaException {
        try {
            return doParseItemValue();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T parseRealValue(Class<T> clazz) throws SchemaException {
        try {
            return doParseRealValue(clazz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T parseRealValue() throws SchemaException {
        try {
            return doParseRealValue();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> JAXBElement<T> parseRealValueToJaxbElement() throws SchemaException {
        try {
            return doParseAnyValueAsJAXBElement();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public RootXNodeImpl parseToXNode() throws SchemaException {
        try {
            return doParseToXNode();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException {
        try {
            return doParseObjects();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void parseObjectsIteratively(@NotNull ObjectHandler handler) throws SchemaException {
        try {
            doParseObjectsIteratively(handler);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object parseItemOrRealValue() throws SchemaException {
        try {
            return doParseItemOrRealValue();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
