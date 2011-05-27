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
import com.evolveum.midpoint.common.password.PasswordPolicyException;

public class PolicyValidator {
/**
 * Validate if provided PaswordPolicyType is logically consistent 
 * 
 * @param pp - Password policy for validation
 * @throws PasswordPolicyException
 */
	public static void validate(PasswordPolicyType pp) throws PasswordPolicyException {

		if (null == pp) {
			throw new PasswordPolicyException("provided policy is NULL", new OperationResult(
					"Password Policy Validation", "PPV-0001", "Provided policy is NULL"));
		}

		// Check complexity initial status
		if (null == pp.getComplexity()) {
			throw new PasswordPolicyException("No complexity defined in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0002",
							"No complexity defined in password policy :" + pp.getName()));
		}

		if (null == pp.getComplexity().getLowers()) {
			throw new PasswordPolicyException(
					"No complexity defined for lower class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0003",
							"No complexity defined for lower class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getUppers()) {
			throw new PasswordPolicyException(
					"No complexity defined for upper class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0004",
							"No complexity defined for upper class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getNumbers()) {
			throw new PasswordPolicyException(
					"No complexity defined for number class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0005",
							"No complexity defined for number class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getSpecial()) {
			throw new PasswordPolicyException(
					"No complexity defined for special class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0006",
							"No complexity defined for special class characters in password policy :"
									+ pp.getName()));
		}

		// Min/max tests
		if (pp.getComplexity().getMinSize() > pp.getComplexity().getMaxSize()) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Requested minimal characters are bigger then maximal in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0007",
							"Requested minimal characters are bigger then maximal in password policy :"
									+ pp.getName()));
		}

		// sum(min) >= max
		if (pp.getComplexity().getMaxSize() < (pp.getComplexity().getLowers().getMinOccurence()
				+ pp.getComplexity().getUppers().getMinOccurence()
				+ pp.getComplexity().getNumbers().getMinOccurence() + pp.getComplexity().getSpecial()
				.getMinOccurence())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Requested minimal occurences for character classes exceed maximal length of password in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0008",
							"Requested minimal occurences for character classes exceed maximal length of password in password policy :"
									+ pp.getName()));
		}
		// sum(max) <= min
		if ( pp.getComplexity().getMinSize() > ( pp.getComplexity().getLowers().getMaxOccurence()
				+ pp.getComplexity().getUppers().getMaxOccurence()
				+ pp.getComplexity().getNumbers().getMaxOccurence() 
				+ pp.getComplexity().getSpecial().getMaxOccurence() ) ) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Requested maximal occurences exceed min length of password in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0009",
							"Requested maximal occurences exceed maximal length of password in password policy :"
									+ pp.getName()));
		}
		
		// at least one need to be first
		if (!(pp.getComplexity().getNumbers().isCanBeFirst() || pp.getComplexity().getLowers().isCanBeFirst()
				|| pp.getComplexity().getUppers().isCanBeFirst() || pp.getComplexity().getSpecial()
				.isCanBeFirst())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one of character class need to be marked as can be first in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0010",
							"At least one of character class need to be marked as can be first in password policy :"
									+ pp.getName()));
		}

		// sum(max) <= min
		if ( 0 == ( pp.getComplexity().getLowers().getMaxOccurence()
				+ pp.getComplexity().getUppers().getMaxOccurence()
				+ pp.getComplexity().getNumbers().getMaxOccurence() 
				+ pp.getComplexity().getSpecial().getMaxOccurence() ) ) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one character class need to be enabled in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0011",
							"At least one character class need to be enabled in password policy :"
									+ pp.getName()));
		}
		
		// Can not have only one first and in same time disabled this class 
		if ( ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().getMaxOccurence() == 0)) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Character class can not be disabled and be single marked as can be first:" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0012",
							"Character class can not be disabled and be single marked as can be first: :"
									+ pp.getName()));
		}
		
		// Can not be two first and both disabled 
		if ( (pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 ) 
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getSpecial().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0
					&& pp.getComplexity().getSpecial().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0
					&& pp.getComplexity().getSpecial().getMaxOccurence() == 0 )) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Two character class can not be disabled and same time only marked as can be first :" 
					+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0013",
							"Two character class can not be disabled and same time only marked as can be first :"
									+ pp.getName()));
		}
		
		// Can not be three first and same three disabled disabled 
		if ( ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				||  ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getUppers().isCanBeFirst() == false
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )
				||( pp.getComplexity().getLowers().isCanBeFirst() == false
					&& pp.getComplexity().getUppers().isCanBeFirst() == true
					&& pp.getComplexity().getNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getNumbers().getMaxOccurence() == 0 )) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Two character class can not be disabled and same time only marked as can be first :" 
					+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0014",
							"Two character class can not be disabled and same time only marked as can be first :"
									+ pp.getName()));
		}
		
		
		
		// everything is OK use default classes
		if (null == pp.getValidCharClasses()) {
			return;
		}

		// test if classes are not empty
		if (StringUtils.isEmpty(pp.getValidCharClasses().getLowerChars())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one character need to be defined in class lowerChars in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0015",
							"At least one character need to be defined in class lowerChars in password policy :"
									+ pp.getName()));
		}

		if (StringUtils.isEmpty(pp.getValidCharClasses().getUpperChars())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one character need to be defined in class upperChars in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0016",
							"At least one character need to be defined in class upperChars in password policy :"
									+ pp.getName()));
		}

		if (StringUtils.isEmpty(pp.getValidCharClasses().getNumberChars())) {
			// TODO - FIX exception handling
			
			throw new PasswordPolicyException(
					"At least one character need to be defined in class numberChars in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0017",
							"At least one character need to be defined in class numberChars in password policy :"
									+ pp.getName()));
		}

		if (StringUtils.isEmpty(pp.getValidCharClasses().getSpecialChars())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one character need to be defined in class specialChars in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0018",
							"At least one character need to be defined in class specialChars in password policy :"
									+ pp.getName()));
		}
		// Looks OK
		return;
	}

	public static boolean validatePolicy(PasswordPolicyType pp) {
		try {
			validate(pp);
		} catch (PasswordPolicyException e) {
			return false;
		}
		return true;
	}
}
