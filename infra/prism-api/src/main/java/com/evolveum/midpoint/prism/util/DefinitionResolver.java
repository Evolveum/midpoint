/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Resolves a definition in a parent. Used in cases when there are non-standard resolution exceptions,
 * e.g. default string definitions for items in dynamic schema.
 * 
 * @param <PD> parent definition
 * @param <ID> subitem definition
 * 
 * @author semancik
 */
@FunctionalInterface
public interface DefinitionResolver<PD extends ItemDefinition, ID extends ItemDefinition> {
	
	ID resolve(PD parentDefinition, ItemPath path) throws SchemaException;
	
}
