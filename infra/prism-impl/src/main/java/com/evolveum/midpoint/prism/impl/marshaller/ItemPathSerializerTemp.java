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

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_4.ItemPathType;

/**
 *  TEMPORARY. WILL BE RESOLVED SOMEHOW.
 */
public class ItemPathSerializerTemp {
	public static String serializeWithDeclarations(ItemPath path) {
		return ItemPathHolder.serializeWithDeclarations(path);
	}

	public static String serializeWithForcedDeclarations(ItemPath path) {
		return ItemPathHolder.serializeWithForcedDeclarations(path);
	}

	public static String serializeWithForcedDeclarations(ItemPathType value) {
		return ItemPathHolder.serializeWithForcedDeclarations(value.getItemPath());
	}
}
