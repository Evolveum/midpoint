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
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.util.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;

public class PasswordValidator {
	
	public static void validate( PasswordPolicyType pp) throws PasswordPolicyException {
		Boolean valid = false;
		
		if ( null == pp ) {
			throw new PasswordPolicyException("provided policy is NULL",null);
		}
		
		// If there is no complexity then everything is OK and use defaults.
		if ( null == pp.getComplexity()) {
			valid=true;
			return;
		}
		
		if ( pp.getComplexity().getMinSize() > pp.getComplexity().getMaxSize()) {
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("Requested minimal characters are bigger then maximal in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0001", "Requested minimal characters are bigger then maximal in password policy :" + pp.getName()));
		}
		
		if ( pp.getComplexity().getMaxSize() <  ( 
				pp.getComplexity().getLowers().getMinOccurence() +
				pp.getComplexity().getUppers().getMinOccurence() +
				pp.getComplexity().getNumbers().getMinOccurence() +
				pp.getComplexity().getSpecial().getMinOccurence())) {
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("Requested minimal occurences exceed maximal length of password in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0002", "Requested minimal occurences exceed maximal length of password in password policy :" + pp.getName()));
		}
		if ( ! ( pp.getComplexity().getNumbers().isCanBeFirst() ||
				pp.getComplexity().getLowers().isCanBeFirst() ||
				pp.getComplexity().getUppers().isCanBeFirst() ||
				pp.getComplexity().getSpecial().isCanBeFirst() ) ) {
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("At least one of character class need to be marked as can be first in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0003", "At least one of character class need to be marked as can be first in password policy :" + pp.getName()));
		}
		
		//everything is OK use default classes
		if ( null == pp.getValidCharClasses()) {
			return;
		}
		
		if ( StringUtils.isEmpty(pp.getValidCharClasses().getLowerChars())){
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("At least one character need to be defined in class lowerChars in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0004", "At least one character need to be defined in class lowerChars in password policy :" + pp.getName()));
		}
		
		if ( StringUtils.isEmpty(pp.getValidCharClasses().getUpperChars())){
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("At least one character need to be defined in class upperChars in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0005", "At least one character need to be defined in class upperChars in password policy :" + pp.getName()));
		}
		
		if ( StringUtils.isEmpty(pp.getValidCharClasses().getNumberChars())){
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("At least one character need to be defined in class numberChars in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0006", "At least one character need to be defined in class numberChars in password policy :" + pp.getName()));
		}
		
		if ( StringUtils.isEmpty(pp.getValidCharClasses().getSpecialChars())){
			valid=false;
			// TODO - FIX exception handling
			throw new PasswordPolicyException("At least one character need to be defined in class specialChars in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0007", "At least one character need to be defined in class specialChars in password policy :" + pp.getName()));
		}
		
		valid = true;
		return;
	}
}
