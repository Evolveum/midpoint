/*
 * Copyright (c) 2014 Evolveum
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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Takes care of converting between XNode tree and specific lexical representation (XML, JSON, YAML). As a special case,
 * NullLexicalProcessor uses XNode tree itself as a lexical representation.
 *
 * @author semancik
 *
 */
public interface LexicalProcessor<T> {

	@NotNull
	RootXNode read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException;

	@NotNull
	List<RootXNode> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException;

	/**
	 * Checks if the processor can read from a given file. (Guessed by file extension, for now.)
	 * Used for autodetection of language.
	 */
	boolean canRead(@NotNull File file) throws IOException;

	/**
	 * Checks if the processor can read from a given string. Note this is only an approximative information (for now).
	 * Used for autodetection of language.
	 */
	boolean canRead(@NotNull String dataString);

	/**
	 * Serializes a root node into XNode tree.
	 */
	@NotNull
	T write(@NotNull RootXNode xnode, @Nullable SerializationContext serializationContext) throws SchemaException;

	/**
	 * Serializes a non-root node into XNode tree.
	 * So, xnode SHOULD NOT be a root node (at least for now).
	 *
	 * TODO consider removing - replacing by the previous form.
	 */
	@NotNull
	T write(@NotNull XNode xnode, @NotNull QName rootElementName, @Nullable SerializationContext serializationContext) throws SchemaException;

	/**
	 * TODO
	 *
	 * Not supported for NullLexicalProcessor, though.
	 * @param roots
	 * @param aggregateElementName
	 * @param context
	 * @return
	 * @throws SchemaException
	 */
	@NotNull
	T write(@NotNull List<RootXNode> roots, @NotNull QName aggregateElementName, @Nullable SerializationContext context) throws SchemaException;
}
