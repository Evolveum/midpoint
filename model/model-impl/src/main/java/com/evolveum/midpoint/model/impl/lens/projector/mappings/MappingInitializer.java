/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface MappingInitializer<V extends PrismValue,D extends ItemDefinition<?>> {

    MappingBuilder<V,D> initialize(MappingBuilder<V,D> mapping) throws SchemaException, ConfigurationException;

}
