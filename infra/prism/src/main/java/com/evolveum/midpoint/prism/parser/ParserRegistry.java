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

package com.evolveum.midpoint.prism.parser;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.parser.dom.DomParser;
import com.evolveum.midpoint.prism.parser.json.JsonParser;
import com.evolveum.midpoint.prism.parser.json.YamlParser;
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
public class ParserRegistry {

	private final Map<String, Parser> parserMap;

	public ParserRegistry(SchemaRegistry schemaRegistry) {
		parserMap = new HashMap<>();
		DomParser parserDom = new DomParser(schemaRegistry);
		parserMap.put(LANG_XML, parserDom);
		JsonParser parserJson = new JsonParser();
		parserMap.put(LANG_JSON, parserJson);
		YamlParser parserYaml = new YamlParser();
		parserMap.put(LANG_YAML, parserYaml);
	}

	@NotNull
	public Parser findParser(File file) throws IOException {
		for (Map.Entry<String,Parser> entry: parserMap.entrySet()) {
			Parser aParser = entry.getValue();
			if (aParser.canParse(file)) {
				return aParser;
			}
		}
		throw new SystemException("No parser for file '"+file+"' (autodetect)");
	}

	@NotNull
	public Parser findParser(String data){
        for (Map.Entry<String,Parser> entry: parserMap.entrySet()) {
            Parser aParser = entry.getValue();
            if (aParser.canParse(data)) {
                return aParser;
            }
        }
		throw new SystemException("No parser for data '"+ DebugUtil.excerpt(data,16)+"' (autodetect)");
    }

	@NotNull
	public DomParser domParser() {
		return (DomParser) parserFor(PrismContext.LANG_XML);
	}

	public Parser parserFor(String language) {
		Parser parser = parserMap.get(language);
		if (parser == null) {
			throw new SystemException("No parser for language '"+language+"'");
		}
		return parser;
	}

	public Parser findParser(@NotNull ParserSource source) throws IOException {
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
