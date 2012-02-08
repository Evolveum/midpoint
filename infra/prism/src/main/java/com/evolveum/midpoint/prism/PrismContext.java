/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

/**
 * @author semancik
 *
 */
public class PrismContext {
	
	private SchemaRegistry schemaRegistry;
	private PrismJaxbProcessor prismJaxbProcessor;
	
	public static PrismContext create(SchemaRegistry schemaRegistry) {
		PrismContext prismContext = new PrismContext();
		PrismJaxbProcessor prismJaxbProcessor = new PrismJaxbProcessor(schemaRegistry);
		prismJaxbProcessor.initialize();
		prismContext.prismJaxbProcessor = prismJaxbProcessor;
		return prismContext;
	}

	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public PrismJaxbProcessor getPrismJaxbProcessor() {
		return prismJaxbProcessor;
	}

	public void setPrismJaxbProcessor(PrismJaxbProcessor prismJaxbProcessor) {
		this.prismJaxbProcessor = prismJaxbProcessor;
	}
	

}
