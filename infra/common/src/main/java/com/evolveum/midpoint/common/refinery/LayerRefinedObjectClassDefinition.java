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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author mederly
 */
public interface LayerRefinedObjectClassDefinition extends RefinedObjectClassDefinition {

	LayerType getLayer();

	@NotNull
	@Override
	Collection<? extends LayerRefinedAttributeDefinition<?>> getAttributeDefinitions();

	@NotNull
	@Override
	LayerRefinedObjectClassDefinition clone();

}
