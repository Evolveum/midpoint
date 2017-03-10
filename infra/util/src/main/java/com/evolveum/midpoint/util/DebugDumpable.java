/*
 * Copyright (c) 2010-2016 Evolveum
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
public interface DebugDumpable {
	
	String INDENT_STRING = "  ";
	
	/**
	 * Show the content of the object intended for diagnostics by system administrator. The out
	 * put should be suitable to use in system logs at "debug" level. It may be multi-line, but in
	 * that case it should be well indented and quite terse.
	 * 
	 * As it is intended to be used by system administrator, it should not use any developer terms
	 * such as class names, exceptions or stack traces.
	 * 
	 * @return content of the object intended for diagnostics by system administrator.
	 */
	default String debugDump() {
		return debugDump(0);
	}
	
	String debugDump(int indent);

	default Object debugDumpLazily() {
		return DebugUtil.debugDumpLazily(this);
	}
}
