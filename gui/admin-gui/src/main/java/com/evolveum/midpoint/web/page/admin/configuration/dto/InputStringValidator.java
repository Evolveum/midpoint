/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;

public class InputStringValidator implements IValidator<String> {

	@Override
	public void validate(IValidatable<String> validatable) {
		if (validatable.getValue() == null) {
			ValidationError err = new ValidationError();
			err.addKey("message.emptyString");
			validatable.error(err);
			
		}
	}
	
	
//	@Override
//	protected void onValidate(IValidatable<String> item) {
//		if(item.getValue() == null){
//			error(item, "message.emptyString");
//		}
//	}
//	
//	@Override
//	public boolean validateOnNullValue() {
//		return true;
//	}

}
