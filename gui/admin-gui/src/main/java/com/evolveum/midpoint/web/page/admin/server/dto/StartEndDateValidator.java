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

package com.evolveum.midpoint.web.page.admin.server.dto;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

public class StartEndDateValidator extends AbstractFormValidator {

	private DateTimeField notBefore;
	private DateTimeField notAfter;

	public StartEndDateValidator(DateTimeField notBefore, DateTimeField notAfter) {
		this.notBefore = notBefore;
		this.notAfter = notAfter;
	}

	@Override
	public FormComponent<?>[] getDependentFormComponents() {
		return new FormComponent[] { notBefore, notAfter};
	}

	@Override
	public void validate(Form<?> form) {
        if (notBefore.getConvertedInput() == null || notAfter.getConvertedInput() == null)
            return;

		if (notBefore.getConvertedInput().after(notAfter.getConvertedInput())) {
			error(notBefore, "pageTask.notStartBefore.error1");
		}
	}

}
