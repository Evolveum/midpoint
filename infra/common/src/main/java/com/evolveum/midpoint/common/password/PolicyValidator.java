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

		if (null == pp.getComplexity().getLimitLowers()) {
			throw new PasswordPolicyException(
					"No complexity defined for lower class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0003",
							"No complexity defined for lower class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getLimitUppers()) {
			throw new PasswordPolicyException(
					"No complexity defined for upper class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0004",
							"No complexity defined for upper class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getLimitNumbers()) {
			throw new PasswordPolicyException(
					"No complexity defined for number class characters in password policy :" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0005",
							"No complexity defined for number class characters in password policy :"
									+ pp.getName()));
		}

		if (null == pp.getComplexity().getLimitSpecial()) {
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
		if (pp.getComplexity().getMaxSize() < (pp.getComplexity().getLimitLowers().getMinOccurence()
				+ pp.getComplexity().getLimitUppers().getMinOccurence()
				+ pp.getComplexity().getLimitNumbers().getMinOccurence() + pp.getComplexity().getLimitSpecial()
				.getMinOccurence())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Requested minimal occurences for character classes exceed maximal length of password in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0008",
							"Requested minimal occurences for character classes exceed maximal length of password in password policy :"
									+ pp.getName()));
		}
		// sum(max) <= min
		if ( pp.getComplexity().getMinSize() > ( pp.getComplexity().getLimitLowers().getMaxOccurence()
				+ pp.getComplexity().getLimitUppers().getMaxOccurence()
				+ pp.getComplexity().getLimitNumbers().getMaxOccurence() 
				+ pp.getComplexity().getLimitSpecial().getMaxOccurence() ) ) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Requested maximal occurences exceed min length of password in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0009",
							"Requested maximal occurences exceed maximal length of password in password policy :"
									+ pp.getName()));
		}
		
		// at least one need to be first
		if (!(pp.getComplexity().getLimitNumbers().isCanBeFirst() 
				|| pp.getComplexity().getLimitLowers().isCanBeFirst()
				|| pp.getComplexity().getLimitUppers().isCanBeFirst()
				|| pp.getComplexity().getLimitSpecial().isCanBeFirst())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one of character class need to be marked as can be first in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0010",
							"At least one of character class need to be marked as can be first in password policy :"
									+ pp.getName()));
		}

		// sum(max) <= min
		if ( 0 == ( pp.getComplexity().getLimitLowers().getMaxOccurence()
				+ pp.getComplexity().getLimitUppers().getMaxOccurence()
				+ pp.getComplexity().getLimitNumbers().getMaxOccurence() 
				+ pp.getComplexity().getLimitSpecial().getMaxOccurence() ) ) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"At least one character class need to be enabled in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0011",
							"At least one character class need to be enabled in password policy :"
									+ pp.getName()));
		}
		
		// Can not have only one first and in same time disabled this class 
		if ( ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().getMaxOccurence() == 0)) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Character class can not be disabled and be single marked as can be first:" + pp.getName(),
					new OperationResult("Password Policy Validation", "PPV-0012",
							"Character class can not be disabled and be single marked as can be first: :"
									+ pp.getName()));
		}
		
		// Can not be two first and both disabled 
		if ( (pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 ) 
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitSpecial().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitSpecial().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == false 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitSpecial().getMaxOccurence() == 0 )) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Two character class can not be disabled and same time only marked as can be first :" 
					+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0013",
							"Two character class can not be disabled and same time only marked as can be first :"
									+ pp.getName()));
		}
		
		// Can not be three first and same three disabled disabled 
		if ( ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == false
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				||  ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				|| ( pp.getComplexity().getLimitLowers().isCanBeFirst() == true 
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )
				||( pp.getComplexity().getLimitLowers().isCanBeFirst() == false
					&& pp.getComplexity().getLimitUppers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitNumbers().isCanBeFirst() == true
					&& pp.getComplexity().getLimitSpecial().isCanBeFirst() == true
					&& pp.getComplexity().getLimitLowers().getMaxOccurence() == 0
					&& pp.getComplexity().getLimitUppers().getMaxOccurence() == 0 
					&& pp.getComplexity().getLimitNumbers().getMaxOccurence() == 0 )) {
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
		
		// test if there is enough characters to apply unique
		if (pp.getComplexity().getMinimumUniqueChars() > ( 
				pp.getValidCharClasses().getLowerChars().length() 
				+ pp.getValidCharClasses().getUpperChars().length()
				+ pp.getValidCharClasses().getNumberChars().length()
				+ pp.getValidCharClasses().getSpecialChars().length())) {
			// TODO - FIX exception handling
			throw new PasswordPolicyException(
					"Number of of available characters no meet requested uniqueness in password policy :"
							+ pp.getName(), new OperationResult("Password Policy Validation", "PPV-0019",
							"Number of of available characters no meet requested uniqueness in password policy :"
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
