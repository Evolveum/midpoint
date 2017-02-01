/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.TargetAndFormAcceptor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
public class DefaultAjaxSubmitButton extends AjaxSubmitButton {

	private final PageBase pageBase;
	private final TargetAndFormAcceptor onSubmit;

	public DefaultAjaxSubmitButton(String id, IModel<String> label, PageBase pageBase, TargetAndFormAcceptor onSubmit) {
		super(id, label);
		this.pageBase = pageBase;
		this.onSubmit = onSubmit;
	}

	@Override
	protected void onError(AjaxRequestTarget target, Form<?> form) {
		target.add(pageBase.getFeedbackPanel());
	}

	@Override
	protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
		onSubmit.accept(target, form);
	}
}
