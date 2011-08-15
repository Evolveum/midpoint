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
public interface Dumpable {
	
	/**
	 * Show the content of the object intended for diagnostics.
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
