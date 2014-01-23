/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.xnode;

import java.io.File;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;

/**
 * @author semancik
 *
 */
public abstract class XNode implements Dumpable, DebugDumpable {
	
	// Common fields
	private XNode parent;

	// These are set when parsing a file
	private File originFile;
	private String originDescription;
	private int lineNumber;
	
	// These are used for serialization
	private ItemDefinition definition;

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String dump() {
		return debugDump();
	}
	
}
