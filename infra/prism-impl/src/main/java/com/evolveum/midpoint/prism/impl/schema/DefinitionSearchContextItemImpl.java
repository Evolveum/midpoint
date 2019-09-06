/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.GlobalDefinitionSearchContext;

/**
 * @author mederly
 */
public class DefinitionSearchContextItemImpl<ID extends ItemDefinition> implements GlobalDefinitionSearchContext<ID> {

//	private final DefinitionSearchImplementation provider;
//	private final Class<? extends ID> definitionClass;
//
//	public DefinitionSearchContextItemImpl(DefinitionSearchImplementation provider, Class<? extends ID> definitionClass) {
//		this.provider = provider;
//		this.definitionClass = definitionClass;
//	}
//
//	@Override
//	public ID byElementName(@NotNull QName elementName) {
//		return provider.findItemDefinition(elementName, definitionClass);
//	}
//
//	@Override
//	public ID byType(@NotNull QName type) {
//		return provider.findItemDefinitionByType(type, definitionClass);
//	}
//
//	@Override
//	public <C extends Containerable> ID byCompileTimeClass(@NotNull Class<C> clazz) {
//		if (PrismObjectDefinition.class.isAssignableFrom(definitionClass)) {
//			return (ID) provider.findObjectDefinitionByCompileTimeClass((Class<Objectable>) clazz);
//		} else if (PrismContainerDefinition.class.isAssignableFrom(definitionClass)) {
//			return (ID) provider.findItemDefinitionByCompileTimeClass(clazz);
//		} else {
//			throw new UnsupportedOperationException("Only containers and prism objects can be searched by compile-time class. Not "
//				+ definitionClass + " by " + clazz);
//		}
//	}
}
