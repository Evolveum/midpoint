/*
 * Copyright (c) 2010-2013 Evolveum
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
 * @author Radovan Semancik
 *
 */
public interface Dumpable {
	
	/**
	 * Show the content of the object intended for diagnostics by developer.
	 * 
	 * The content may be multi-line, in case of hierarchical objects it may be intended.
	 * 
	 * The use of this method may not be efficient. It is not supposed to be used in normal operation.
	 * However, it is very useful in tests or in case of dumping objects in severe error situations.
	 * 
	 * @return content of the object intended for diagnostics.
	 */
	public String dump();

}
