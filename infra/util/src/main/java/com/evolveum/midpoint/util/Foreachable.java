/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.util;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface Foreachable<T> {
	
	/**
	 * Will call processor for every element in the instance.
	 * This is NOT recursive. E.g. in case of collection of collections
	 * the processor will NOT be called for elements of the inner collections.
	 * If you need recursion please have a look at Visitor.
	 */
	void foreach(Processor<T> processor);

}
