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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.result;

import java.io.Serializable;

/**
 * A class that marks presence of various values in a specific OperationResult map. E.g. if an instance of this class is present
 * as a "foo" parameter in OperationResult parameter map then such parameter contains many different values.
 * 
 * It only makes sense for OperationResult entries that are summarized (has count > 1) 
 * 
 * @author Radovan Semancik
 *
 */
public class VariousValues implements Serializable {

	@Override
	public String toString() {
		return "[various values]";
	}

}
