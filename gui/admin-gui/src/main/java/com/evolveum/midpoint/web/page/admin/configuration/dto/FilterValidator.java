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

import java.util.Set;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.validation.IValidatable;
//import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;

public class FilterValidator implements IValidator<LoggingComponentType> {

	@Override
	public void validate(IValidatable<LoggingComponentType> validatable) {
		if (validatable.getValue() == null) {
			ValidationError err = new ValidationError();
			err.addKey("filter.emptyFilter");
			validatable.error(err);
			
		}
	}
	
	
	
	//extends AbstractValidator<LoggingComponentType> {
//
//	@Override
//	protected void onValidate(IValidatable<LoggingComponentType> item) {
//		if (item.getValue() == null) {
//			error(item, "filter.emptyFilter");
//		}
//	}
//
//	@Override
//	public boolean validateOnNullValue() {
//		return true;
//	}

}
