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
package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class ShadowValuePolicyOriginResolver extends AbstractValuePolicyOriginResolver<ShadowType> {

	public ShadowValuePolicyOriginResolver(PrismObject<ShadowType> object, ObjectResolver objectResolver) {
		super(object, objectResolver);
	}

	@Override
	public ObjectQuery getOwnerQuery() {
		return QueryBuilder
				.queryFor(UserType.class, getObject().getPrismContext())
				.item(UserType.F_LINK_REF).ref(getObject().getOid())
				.build();
	}	
}
