/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author mederly
 */
public interface LayerRefinedObjectClassDefinition extends RefinedObjectClassDefinition {

	LayerType getLayer();

	@NotNull
	@Override
	Collection<? extends LayerRefinedAttributeDefinition<?>> getAttributeDefinitions();

	@NotNull
	@Override
	LayerRefinedObjectClassDefinition clone();

}
