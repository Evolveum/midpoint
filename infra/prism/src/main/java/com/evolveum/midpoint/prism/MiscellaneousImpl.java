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

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;

/**
 * TEMPORARY
 */
public class MiscellaneousImpl implements Miscellaneous {

	@NotNull private final PrismContextImpl prismContext;

	MiscellaneousImpl(@NotNull PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	/**
	 * TODO rewrite this method using Prism API
	 */
	@Override
	public void serializeFaultMessage(Detail detail, Object faultInfo, QName faultMessageElementName, Trace logger) {
		try {
			XNode faultMessageXnode = prismContext.getBeanMarshaller().marshall(faultInfo);
			RootXNode xroot = new RootXNode(faultMessageElementName, faultMessageXnode);
			xroot.setExplicitTypeDeclaration(true);
			QName faultType = prismContext.getSchemaRegistry().determineTypeForClass(faultInfo.getClass());
			xroot.setTypeQName(faultType);
			prismContext.getParserDom().serializeUnderElement(xroot, faultMessageElementName, detail);
		} catch (SchemaException e) {
			logger.error("Error serializing fault message (SOAP fault detail): {}", e.getMessage(), e);
		}
	}

}
