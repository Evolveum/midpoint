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
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;

public class LoggerValidator<T> implements IValidator<T> {

	@Override
	public void validate(IValidatable<T> validatable) {
		if (validatable.getValue() == null) {
			ValidationError err = new ValidationError();
			err.addKey("logger.emptyLogger");
			validatable.error(err);

		}
	}

	// extends AbstractValidator<LoggingComponentType> {
	//
	// @Override
	// protected void onValidate(IValidatable<LoggingComponentType> item) {
	// if(item.getValue() == null){
	// error(item, "logger.emptyLogger");
	// }
	// }
	//
	// @Override
	// public boolean validateOnNullValue() {
	// return true;
	// }

}
