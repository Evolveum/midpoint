/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex;

import static com.evolveum.midpoint.prism.PrismContext.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.impl.lex.json.*;

import com.evolveum.midpoint.prism.impl.lex.json.reader.JsonReader;
import com.evolveum.midpoint.prism.impl.lex.json.reader.YamlReader;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ParserFileSource;
import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParserStringSource;
import com.evolveum.midpoint.prism.ParserXNodeSource;
import com.evolveum.midpoint.prism.impl.ParserElementSource;
import com.evolveum.midpoint.prism.impl.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;

public class LexicalProcessorRegistry {

    private final Map<String, LexicalProcessor<?>> parserMap;

    private final DomLexicalProcessor domLexicalProcessor;
    private final NullLexicalProcessor nullLexicalProcessor;

    public LexicalProcessorRegistry(@NotNull SchemaRegistry schemaRegistry) {
        domLexicalProcessor = new DomLexicalProcessor(schemaRegistry);
        nullLexicalProcessor = new NullLexicalProcessor();

        parserMap = new HashMap<>();
        parserMap.put(LANG_XML, domLexicalProcessor);
        parserMap.put(LANG_JSON, new DelegatingLexicalProcessor(new JsonReader(schemaRegistry), new JsonWriter()));
        parserMap.put(LANG_YAML, new DelegatingLexicalProcessor(new YamlReader(schemaRegistry), new YamlWriter()));
    }

    @NotNull
    public String detectLanguage(File file) throws IOException {
        for (Map.Entry<String, LexicalProcessor<?>> entry : parserMap.entrySet()) {
            LexicalProcessor<?> aLexicalProcessor = entry.getValue();
            if (aLexicalProcessor.canRead(file)) {
                return entry.getKey();
            }
        }
        throw new SystemException("Data language couldn't be auto-detected for file '" + file + "'");
    }

    @NotNull
    private LexicalProcessor<?> findProcessor(File file) throws IOException {
        for (LexicalProcessor<?> lexicalProcessor : parserMap.values()) {
            if (lexicalProcessor.canRead(file)) {
                return lexicalProcessor;
            }
        }
        throw new SystemException("No lexical processor for file '" + file + "' (autodetect)");
    }

    @NotNull
    private LexicalProcessor<?> findProcessor(@NotNull String data) {
        for (LexicalProcessor<?> lexicalProcessor : parserMap.values()) {
            if (lexicalProcessor.canRead(data)) {
                return lexicalProcessor;
            }
        }
        throw new SystemException("No lexical processor for data '" + DebugUtil.excerpt(data, 16) + "' (autodetect)");
    }

    @NotNull
    public DomLexicalProcessor domProcessor() {
        return domLexicalProcessor;
    }

    @NotNull
    public <T> LexicalProcessor<T> processorFor(String language) {
        LexicalProcessor<?> lexicalProcessor = parserMap.get(language);
        if (lexicalProcessor == null) {
            throw new SystemException("No lexical processor for language '" + language + "'");
        }
        //noinspection unchecked
        return (LexicalProcessor<T>) lexicalProcessor;
    }

    @NotNull
    public LexicalProcessor<?> findProcessor(@NotNull ParserSource source) throws IOException {
        if (source instanceof ParserXNodeSource) {
            return nullLexicalProcessor;
        } else if (source instanceof ParserElementSource) {
            return processorFor(LANG_XML);
        } else if (source instanceof ParserFileSource) {
            return findProcessor(((ParserFileSource) source).getFile());
        } else if (source instanceof ParserStringSource) {
            return findProcessor(((ParserStringSource) source).getData());
        } else {
            throw new IllegalArgumentException("Cannot determine lexical processor from " + source.getClass());
        }
    }
}
