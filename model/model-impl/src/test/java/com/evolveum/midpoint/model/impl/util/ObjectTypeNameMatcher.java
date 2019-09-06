/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 *
 * @author lazyman
 */
public class ObjectTypeNameMatcher extends BaseMatcher<PrismObject<ObjectType>> {

	private String name;

	public ObjectTypeNameMatcher(PolyStringType name) {
		this.name = name.getOrig();
	}

	public ObjectTypeNameMatcher(String name) {
		this.name = name;
	}

	@Override
	public boolean matches(Object item) {
		PrismObject<ObjectType> object = (PrismObject<ObjectType>) item;

		return object.asObjectable().getName().getOrig().equals(name);
	}

	@Override
	public void describeTo(Description description) {
	}
}
