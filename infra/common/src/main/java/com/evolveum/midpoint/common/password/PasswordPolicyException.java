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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */

package com.evolveum.midpoint.common.password;

import com.evolveum.midpoint.util.MidPointException;
import com.evolveum.midpoint.util.result.OperationResult;
/**
 * This class is used for exceptions which are thrown during operations with 
 * PasswordPolicy. It provides @{link OperationResult} object for better error
 * handling.
 * 
 * @author Mamut
 * 
 */

public class PasswordPolicyException extends MidPointException {
	
	private static final long serialVersionUID = -8740728739980818024L;

	public PasswordPolicyException (String msg, OperationResult or) {
		super (msg, or);
	}
}
