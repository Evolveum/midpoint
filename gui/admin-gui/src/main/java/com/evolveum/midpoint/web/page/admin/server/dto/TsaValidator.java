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

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;

public class TsaValidator extends AbstractFormValidator {

	private CheckBox runUntilNodeDown;
	private DropDownChoice threadStop;

	public TsaValidator(CheckBox runUntilNodeDown, DropDownChoice threadStop) {
		this.runUntilNodeDown = runUntilNodeDown;
		this.threadStop = threadStop;
	}

	@Override
	public FormComponent<?>[] getDependentFormComponents() {
		return new FormComponent[] { runUntilNodeDown, threadStop };
	}

	@Override
	public void validate(Form<?> form) {

		if (runUntilNodeDown.getConvertedInput()) {
			if (ThreadStopActionType.RESTART.equals(threadStop.getConvertedInput())
					|| ThreadStopActionType.RESCHEDULE.equals(threadStop.getConvertedInput())){
				error(runUntilNodeDown, "pageTask.runUntilNodeDown.error1");
			}
				
		} else {
			if (ThreadStopActionType.CLOSE.equals(threadStop.getConvertedInput())
					|| ThreadStopActionType.SUSPEND.equals(threadStop.getConvertedInput())){
				error(runUntilNodeDown, "pageTask.runUntilNodeDown.error2");
			}
		}

	}

}
