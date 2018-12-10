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

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.ItemPathParser;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ItemPathParserImpl implements ItemPathParser {

	@NotNull private final PrismContextImpl prismContext;

	ItemPathParserImpl(@NotNull PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public ItemPathType asItemPathType(String value) {
		return new ItemPathType(asItemPath(value));
	}

	@Override
	public UniformItemPath asItemPath(String value) {
		return ItemPathParserTemp.parseFromString(value);
	}
}
