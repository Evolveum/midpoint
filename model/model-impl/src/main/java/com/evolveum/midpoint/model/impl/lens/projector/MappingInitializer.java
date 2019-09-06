/*
 * Copyright (c) 2013-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface MappingInitializer<V extends PrismValue,D extends ItemDefinition> {

	MappingImpl.Builder<V,D> initialize(MappingImpl.Builder<V,D> mapping) throws SchemaException;

}
