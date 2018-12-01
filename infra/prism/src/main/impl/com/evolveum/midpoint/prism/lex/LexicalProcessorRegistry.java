/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.lex.json.JsonLexicalProcessor;
import com.evolveum.midpoint.prism.lex.json.NullLexicalProcessor;
import com.evolveum.midpoint.prism.lex.json.YamlLexicalProcessor;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.prism.PrismContext.LANG_JSON;
import static com.evolveum.midpoint.prism.PrismContext.LANG_XML;
import static com.evolveum.midpoint.prism.PrismContext.LANG_YAML;

/**
 * @author mederly
 */
public class LexicalProcessorRegistry {

	private final Map<String, LexicalProcessor> parserMap;

	private final DomLexicalProcessor domLexicalProcessor;
	private final NullLexicalProcessor nullLexicalProcessor;

	public LexicalProcessorRegistry(@NotNull SchemaRegistry schemaRegistry) {
		domLexicalProcessor = new DomLexicalProcessor(schemaRegistry);
		nullLexicalProcessor = new NullLexicalProcessor();

		parserMap = new HashMap<>();
		parserMap.put(LANG_XML, domLexicalProcessor);
		parserMap.put(LANG_JSON, new JsonLexicalProcessor(schemaRegistry));
		parserMap.put(LANG_YAML, new YamlLexicalProcessor(schemaRegistry));
	}

	@NotNull
	public String detectLanguage(File file) throws IOException {
		for (Map.Entry<String,LexicalProcessor> entry: parserMap.entrySet()) {
			LexicalProcessor aLexicalProcessor = entry.getValue();
			if (aLexicalProcessor.canRead(file)) {
				return entry.getKey();
			}
		}
		throw new SystemException("Data language couldn't be auto-detected for file '"+file+"'");
	}

	@NotNull
	private LexicalProcessor findProcessor(File file) throws IOException {
		for (Map.Entry<String,LexicalProcessor> entry: parserMap.entrySet()) {
			LexicalProcessor aLexicalProcessor = entry.getValue();
			if (aLexicalProcessor.canRead(file)) {
				return aLexicalProcessor;
			}
		}
		throw new SystemException("No lexical processor for file '"+file+"' (autodetect)");
	}

	@NotNull
	private LexicalProcessor findProcessor(@NotNull String data){
        for (Map.Entry<String,LexicalProcessor> entry: parserMap.entrySet()) {
            LexicalProcessor aLexicalProcessor = entry.getValue();
            if (aLexicalProcessor.canRead(data)) {
                return aLexicalProcessor;
            }
        }
		throw new SystemException("No lexical processor for data '"+ DebugUtil.excerpt(data,16)+"' (autodetect)");
    }

	@NotNull
	public DomLexicalProcessor domProcessor() {
		return domLexicalProcessor;
	}

	@NotNull
	public <T> LexicalProcessor<T> processorFor(String language) {
		LexicalProcessor<?> lexicalProcessor = parserMap.get(language);
		if (lexicalProcessor == null) {
			throw new SystemException("No lexical processor for language '"+language+"'");
		}
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
