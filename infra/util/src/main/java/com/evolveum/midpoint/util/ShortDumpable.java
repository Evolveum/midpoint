/*
 * Copyright (c) 2017 Evolveum
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
 */
@FunctionalInterface
public interface ShortDumpable {
	
	/**
	 * Show the content of the object intended for diagnostics. This method is supposed
	 * to return a compact, human-readable output in a single line. Unlike toString() method,
	 * there is no requirement to identify the actual class or type of the object.
	 * It is assumed that the class/type will be obvious from the context in which the
	 * output is used.
	 * 
	 * @return compact one-line content of the object intended for diagnostics by system administrator.
	 */
	void shortDump(StringBuilder sb);

	// convenience version
	default String shortDump() {
		StringBuilder sb = new StringBuilder();
		shortDump(sb);
		return sb.toString();
	}
	
	default Object shortDumpLazily() {
		return DebugUtil.shortDumpLazily(this);
	}
}
