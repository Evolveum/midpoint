/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
class AddObjectResult<T extends ObjectType> {

	@NotNull private final PrismObject<T> object;

	public AddObjectResult(PrismObject<T> object) {
		this.object = object;
	}

	@NotNull
	public PrismObject<T> getObject() {
		return object;
	}

	@Override
	public String toString() {
		return object.toString();
	}
}
