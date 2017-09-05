/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.schema.processor;

/**
 * @author semancik
 *
 */
public class SearchHierarchyConstraints {

	ResourceObjectIdentification baseContext;
	SearchHierarchyScope scope;

	public SearchHierarchyConstraints(ResourceObjectIdentification baseContext, SearchHierarchyScope scope) {
		super();
		this.baseContext = baseContext;
		this.scope = scope;
	}

	public ResourceObjectIdentification getBaseContext() {
		return baseContext;
	}

	public SearchHierarchyScope getScope() {
		return scope;
	}


}
