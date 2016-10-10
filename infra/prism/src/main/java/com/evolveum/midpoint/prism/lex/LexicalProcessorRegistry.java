/*
 * Copyright (c) 2010-2016 Evolveum
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

	public LexicalProcessorRegistry(SchemaRegistry schemaRegistry) {
		parserMap = new HashMap<>();
		DomLexicalProcessor parserDom = new DomLexicalProcessor(schemaRegistry);
		parserMap.put(LANG_XML, parserDom);
		JsonLexicalProcessor parserJson = new JsonLexicalProcessor();
		parserMap.put(LANG_JSON, parserJson);
		YamlLexicalProcessor parserYaml = new YamlLexicalProcessor();
		parserMap.put(LANG_YAML, parserYaml);
	}

	@NotNull
	public LexicalProcessor findParser(File file) throws IOException {
		for (Map.Entry<String,LexicalProcessor> entry: parserMap.entrySet()) {
			LexicalProcessor aLexicalProcessor = entry.getValue();
			if (aLexicalProcessor.canParse(file)) {
				return aLexicalProcessor;
			}
		}
		throw new SystemException("No parser for file '"+file+"' (autodetect)");
	}

	@NotNull
	public LexicalProcessor findParser(String data){
        for (Map.Entry<String,LexicalProcessor> entry: parserMap.entrySet()) {
            LexicalProcessor aLexicalProcessor = entry.getValue();
            if (aLexicalProcessor.canParse(data)) {
                return aLexicalProcessor;
            }
        }
		throw new SystemException("No parser for data '"+ DebugUtil.excerpt(data,16)+"' (autodetect)");
    }

	@NotNull
	public DomLexicalProcessor domParser() {
		return (DomLexicalProcessor) parserFor(PrismContext.LANG_XML);
	}

	public LexicalProcessor parserFor(String language) {
		LexicalProcessor lexicalProcessor = parserMap.get(language);
		if (lexicalProcessor == null) {
			throw new SystemException("No parser for language '"+language+"'");
		}
		return lexicalProcessor;
	}

	public LexicalProcessor findParser(@NotNull ParserSource source) throws IOException {
		if (source instanceof ParserElementSource) {
			return parserFor(LANG_XML);
		} else if (source instanceof ParserFileSource) {
			return findParser(((ParserFileSource) source).getFile());
		} else if (source instanceof ParserStringSource) {
			return findParser(((ParserStringSource) source).getData());
		} else {
			throw new IllegalArgumentException("Cannot determine source from " + source.getClass());
		}
	}
}
