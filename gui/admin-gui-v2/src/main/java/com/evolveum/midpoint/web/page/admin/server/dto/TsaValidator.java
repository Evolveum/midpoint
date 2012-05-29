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

package com.evolveum.midpoint.web.page.admin.server.dto;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ThreadStopActionType;

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
				error(runUntilNodeDown, "runUntilNodeDown.error1");
			}
				
		} else {
			if (ThreadStopActionType.CLOSE.equals(threadStop.getConvertedInput())
					|| ThreadStopActionType.SUSPEND.equals(threadStop.getConvertedInput())){
				error(runUntilNodeDown, "runUntilNodeDown.error2");
			}
		}

	}

}
