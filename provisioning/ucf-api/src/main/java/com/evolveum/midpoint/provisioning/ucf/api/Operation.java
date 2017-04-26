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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Abstract operation for a connector. Subclasses of this class
 * represent specific operations such as attribute modification,
 * script execution and so on.
 * 
 * This class is created primarily for type safety, but it may be
 * extended later on.
 * 
 * @author Radovan Semancik
 *
 */
public abstract class Operation implements DebugDumpable {

	@Override
	public String debugDump() {
		return debugDump(0);
	}
	
}