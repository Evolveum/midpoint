/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismProperty;

/**
 *
 */
public interface ResourceAttribute<T> extends PrismProperty<T> {

	ResourceAttributeDefinition<T> getDefinition();

	String getNativeAttributeName();

	@Override
	ResourceAttribute<T> clone();
}
