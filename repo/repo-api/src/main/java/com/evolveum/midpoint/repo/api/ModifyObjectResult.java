/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *  Contains information about object modification result; primarily needed by repository caching algorithms.
 *  Because it is bound to the current (SQL) implementation of the repository, avoid using this information
 *  for any other purposes.
 *
 *  Note that objectBefore and objectAfter might be null if the object XML representation was not changed.
 *  It is currently the case for lookup tables (when rows are modified) and certification campaigns (when cases are modified).
 *  In all other cases these are non-null.
 *
 *  EXPERIMENTAL.
 */
public class ModifyObjectResult<T extends ObjectType> {

	private final PrismObject<T> objectBefore;
	private final PrismObject<T> objectAfter;

	public ModifyObjectResult() {
		this(null, null);
	}

	public ModifyObjectResult(PrismObject<T> objectBefore, PrismObject<T> objectAfter) {
		this.objectBefore = objectBefore;
		this.objectAfter = objectAfter;
	}

	public PrismObject<T> getObjectBefore() {
		return objectBefore;
	}

	public PrismObject<T> getObjectAfter() {
		return objectAfter;
	}
}
