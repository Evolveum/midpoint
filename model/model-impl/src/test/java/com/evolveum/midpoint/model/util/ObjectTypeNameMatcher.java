/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.model.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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
