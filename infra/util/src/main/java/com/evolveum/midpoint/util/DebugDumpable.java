/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.util;

/**
 * @author Radovan Semancik
 *
 */
public interface DebugDumpable {
	
	public static final Object INDENT_STRING = "  ";
	
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
	public String debugDump();
	
	public String debugDump(int indent);

}
