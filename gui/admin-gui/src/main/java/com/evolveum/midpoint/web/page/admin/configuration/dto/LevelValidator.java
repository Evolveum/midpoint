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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingLevelType;

public class LevelValidator extends AbstractValidator<LoggingLevelType> {

	@Override
	protected void onValidate(IValidatable<LoggingLevelType> item) {
		if(item.getValue() == null){
			error(item, "message.emptyLevel");
		}
	}
	
	@Override
	public boolean validateOnNullValue() {
		return true;
	}

}
