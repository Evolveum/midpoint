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
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Interface for properties that have inner structur, such as PolyString.
 * This was created due to a limitation that we cannot make every structured
 * data into a container (yet).
 * 
 * This is a temporary solution in 3.x and 4.x. It should be gone in 5.x.
 * Do not realy on this with any new development.
 * 
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface Structured {

	Object resolve(ItemPath subpath);

}
