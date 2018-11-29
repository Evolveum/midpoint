/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public interface CanonicalItemPath extends Serializable {

	static CanonicalItemPathImpl create(ItemPath itemPath, Class<? extends Containerable> clazz, PrismContext prismContext) {
		return new CanonicalItemPathImpl(itemPath, clazz, prismContext);
	}

	static CanonicalItemPath create(ItemPath itemPath) {
		return new CanonicalItemPathImpl(itemPath, null, null);
	}

	List<CanonicalItemPathImpl.Segment> getSegments();

	int size();

	CanonicalItemPathImpl allUpToIncluding(int n);

	String asString();

}
