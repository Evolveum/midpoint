/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
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