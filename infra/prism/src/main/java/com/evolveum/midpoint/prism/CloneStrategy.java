/**
 * Copyright (c) 2018 Evolveum
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

/**
 * @author semancik
 *
 */
public enum CloneStrategy {
	
	/**
	 * Literal clone. All properties of the clone are the same as those of the original.
	 */
	LITERAL,
	
	/**
	 * Clone for reuse.
	 * Create clone of the object that is suitable to be reused
     * in a different object or delta. The cloned object will
     * have the same values, but it will not be presented as the
     * same object as was the source of cloning.
     * 
     * E.g. in case of containers it will create a container
     * with the same values but with not identifiers.
     * References will not have full object inside them.
	 */
	REUSE;

}
