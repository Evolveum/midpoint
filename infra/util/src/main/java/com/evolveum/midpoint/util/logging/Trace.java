/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner] 
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util.logging;

import org.slf4j.Logger;

/**
 * 
 * 
 The six logging levels used by Log are (in order):
 * 
 * 1. trace (the least serious) 2. debug 3. info 4. warn 5. error 6. fatal (the
 * most serious)
 * 
 * The mapping of these log levels to the concepts used by the underlying
 * logging system is implementation dependent. The implemention should ensure,
 * though, that this ordering behaves as expected.
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface Trace extends Logger {

	public static final String code_id = "$Id$";

}
