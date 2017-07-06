/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO better name?
 * @author mederly
 */
public class ObjectInfo {
	private String oid;
	private String name;

	public ObjectInfo(ObjectType object) {
		oid = object.getOid();
		name = PolyString.getOrig(object.getName());
	}

	public String getOid() {
		return oid;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name + "(" + oid + ")";
	}
}
