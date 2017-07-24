/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.web.security;

import java.util.List;

import org.apache.wicket.core.util.objects.checker.IObjectChecker;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class MidPointObjectChecker implements IObjectChecker {

	/* (non-Javadoc)
	 * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#check(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Result check(Object object) {
		if (object instanceof PrismObject<?>) {
			return checkObject((PrismObject<? extends ObjectType>)object);
		} else if (object instanceof ObjectType) {
			return checkObject(((ObjectType)object).asPrismObject());
		}
		
		return Result.SUCCESS;
	}

	
	
	private <O extends ObjectType> Result checkObject(PrismObject<O> object) {
		
		if (object.canRepresent(ResourceType.class)) {
			return new Result( Result.Status.FAILURE, "Storage of ResourceType objects not allowed: "+object);
		}
		
		return Result.SUCCESS;
	}



	/* (non-Javadoc)
	 * @see org.apache.wicket.core.util.objects.checker.IObjectChecker#getExclusions()
	 */
	@Override
	public List<Class<?>> getExclusions() {
		return null;
	}

}
