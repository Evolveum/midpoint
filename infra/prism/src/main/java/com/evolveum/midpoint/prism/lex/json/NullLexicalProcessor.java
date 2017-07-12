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

package com.evolveum.midpoint.prism.lex.json;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParserXNodeSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author mederly
 */
public class NullLexicalProcessor implements LexicalProcessor<XNode> {

	@NotNull
	@Override
	public RootXNode read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
		if (!(source instanceof ParserXNodeSource)) {
			throw new IllegalStateException("Unsupported parser source: " + source.getClass().getName());
		}
		return ((ParserXNodeSource) source).getXNode();
	}

	@NotNull
	@Override
	public List<RootXNode> readObjects(ParserSource source, ParsingContext parsingContext) throws SchemaException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canRead(@NotNull File file) throws IOException {
		return false;
	}

	@Override
	public boolean canRead(@NotNull String dataString) {
		return false;
	}

	@NotNull
	@Override
	public XNode write(@NotNull RootXNode xnode, @Nullable SerializationContext serializationContext) throws SchemaException {
		return xnode;
	}

	@NotNull
	@Override
	public XNode write(@NotNull XNode xnode, @NotNull QName rootElementName, @Nullable SerializationContext serializationContext)
			throws SchemaException {
		return xnode;
	}
}
