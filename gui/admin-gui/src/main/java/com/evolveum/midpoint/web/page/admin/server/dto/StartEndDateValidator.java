/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
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
