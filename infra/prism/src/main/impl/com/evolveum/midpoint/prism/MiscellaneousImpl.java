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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import java.io.Serializable;

/**
 * TEMPORARY
 */
public class MiscellaneousImpl implements Miscellaneous {

	@NotNull private final PrismContextImpl prismContext;

	MiscellaneousImpl(@NotNull PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	/**
	 * Obscure method. TODO specify the functionality and decide what to do with this.
	 */
	@Override
	@Nullable
	public Serializable guessFormattedValue(Serializable value) throws SchemaException {
		if (value instanceof RawType) {
			XNode xnode = ((RawType) value).getXnode();
			if (xnode instanceof PrimitiveXNodeImpl) {
				return ((PrimitiveXNodeImpl) xnode).getGuessedFormattedValue();
			} else {
				return null;
			}
		} else {
			return value;
		}
	}

	/**
	 * TODO rewrite this method using Prism API
	 */
	@Override
	public void serializeFaultMessage(Detail detail, Object faultInfo, QName faultMessageElementName, Trace logger) {
		try {
			XNodeImpl faultMessageXnode = prismContext.getBeanMarshaller().marshall(faultInfo);
			RootXNodeImpl xroot = new RootXNodeImpl(faultMessageElementName, faultMessageXnode);
			xroot.setExplicitTypeDeclaration(true);
			QName faultType = prismContext.getSchemaRegistry().determineTypeForClass(faultInfo.getClass());
			xroot.setTypeQName(faultType);
			prismContext.getParserDom().serializeUnderElement(xroot, faultMessageElementName, detail);
		} catch (SchemaException e) {
			logger.error("Error serializing fault message (SOAP fault detail): {}", e.getMessage(), e);
		}
	}

	@Override
	public <T> void setPrimitiveXNodeValue(PrimitiveXNode<T> node, T value, QName typeName) {
		((PrimitiveXNodeImpl<T>) node).setValue(value, typeName);
	}

	@Override
	public void putToMapXNode(MapXNode map, QName key, XNode value) {
		((MapXNodeImpl) map).put(key, value);
	}

	@Override
	public <T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNode xmap, PrismContext prismContext, ParsingContext pc) throws SchemaException {
		XNodeProcessorUtil.parseProtectedType(protectedType, (MapXNodeImpl) xmap, prismContext, pc);
	}

	@Override
	public Element serializeSingleElementMapToElement(MapXNode filterClauseXNode) throws SchemaException {
		DomLexicalProcessor domParser = getDomParser(prismContext);
		return domParser.serializeSingleElementMapToElement(filterClauseXNode);
	}

	private static DomLexicalProcessor getDomParser(@NotNull PrismContext prismContext) {
		return ((PrismContextImpl) prismContext).getParserDom();
	}

	@Override
	public void setXNodeType(XNode node, QName explicitTypeName, boolean explicitTypeDeclaration) {
		((XNodeImpl) node).setTypeQName(explicitTypeName);
		((XNodeImpl) node).setExplicitTypeDeclaration(explicitTypeDeclaration);
	}

	@Override
	public void addToDefinition(ComplexTypeDefinition ctd, ItemDefinition other) {
		((ComplexTypeDefinitionImpl) ctd).add(other);
	}

	@Override
	public void replaceDefinition(ComplexTypeDefinition ctd, ItemName name, ItemDefinition other) {
		((ComplexTypeDefinitionImpl) ctd).replaceDefinition(name, other);
	}
}
