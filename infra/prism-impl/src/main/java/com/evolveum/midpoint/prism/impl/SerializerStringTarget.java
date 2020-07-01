/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author mederly
 */
public class SerializerStringTarget extends SerializerTarget<String> {

    @NotNull private final String language;

    SerializerStringTarget(@NotNull PrismContextImpl prismContext, @NotNull String language) {
        super(prismContext);
        this.language = language;
    }

    @NotNull
    @Override
    public String write(@NotNull RootXNodeImpl xroot, SerializationContext context) throws SchemaException {
        LexicalProcessor<String> lexicalProcessor = prismContext.getLexicalProcessorRegistry().processorFor(language);
        return lexicalProcessor.write(xroot, context);
    }

    @NotNull
    @Override
    public String write(@NotNull List<RootXNodeImpl> roots, @Nullable SerializationContext context)
            throws SchemaException {
        LexicalProcessor<String> lexicalProcessor = prismContext.getLexicalProcessorRegistry().processorFor(language);
        return lexicalProcessor.write(roots, context);
    }
}
