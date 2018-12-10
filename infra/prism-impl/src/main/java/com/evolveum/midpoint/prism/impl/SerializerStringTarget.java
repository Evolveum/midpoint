/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public class SerializerStringTarget extends SerializerTarget<String> {

    @NotNull private final String language;

    public SerializerStringTarget(@NotNull PrismContextImpl prismContext, @NotNull String language) {
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
    public String write(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName, @Nullable SerializationContext context)
            throws SchemaException {
        LexicalProcessor<String> lexicalProcessor = prismContext.getLexicalProcessorRegistry().processorFor(language);
        return lexicalProcessor.write(roots, aggregateElementName, context);
    }
}
