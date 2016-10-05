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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * TODO eliminate code duplication within three implementations of PrismSerializer
 *
 * @author mederly
 */
public class PrismSerializerImpl implements PrismSerializer<String> {

	@NotNull private final ParserHelpers parserHelpers;
	@NotNull private final String language;
	private final QName elementName;
	private final SerializationContext context;

	public PrismSerializerImpl(@NotNull ParserHelpers parserHelpers, @NotNull String language, QName elementName, SerializationContext context) {
		this.parserHelpers = parserHelpers;
		this.language = language;
		this.elementName = elementName;
		this.context = context;
	}

	@NotNull
	public String getLanguage() {
		return language;
	}

	@Override
	public PrismSerializerImpl context(SerializationContext context) {
		return new PrismSerializerImpl(this.parserHelpers, this.language, elementName, context);
	}

	@Override
	public PrismSerializerImpl root(QName elementName) {
		return new PrismSerializerImpl(this.parserHelpers, this.language, elementName, this.context);
	}

	@Override
	public PrismSerializerImpl options(SerializationOptions options) {
		SerializationContext context;
		if (this.context != null) {
			context = this.context.clone();
			context.setOptions(options);
		} else {
			context = new SerializationContext(options);
		}
		return new PrismSerializerImpl(this.parserHelpers, this.language, this.elementName, context);
	}

	@Override
	public <O extends Objectable> String serialize(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = parserHelpers.xnodeProcessor.serializeObject(object, false, context);			// TODO serialize composite objects?
		if (elementName != null) {
			xroot.setRootElementName(elementName);		// TODO what about the type?
		}
		return getParser().serializeToString(xroot, context);
	}

	private Parser getParser() {
		return parserHelpers.parserRegistry.parserFor(language);
	}

	@Override
	public <C extends Containerable> String serialize(PrismContainerValue<C> cval) throws SchemaException {
		RootXNode xroot = parserHelpers.xnodeProcessor.serializeItemValueAsRoot(cval, elementName);	// TODO context
		return getParser().serializeToString(xroot, context);
	}

	@Override
	public String serializeAtomicValue(Object value) throws SchemaException {
		RootXNode xroot = parserHelpers.xnodeProcessor.serializeAtomicValue(value, elementName, context);
		return getParser().serializeToString(xroot, context);
	}


}
