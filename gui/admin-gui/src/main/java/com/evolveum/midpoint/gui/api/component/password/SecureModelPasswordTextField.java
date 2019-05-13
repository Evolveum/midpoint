/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.gui.api.component.password;

import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;

/**
 * PasswordTextField that assumes its underlying model is secure enough to be serialized.
 *
 * Therefore we can disable "reset password" security feature and - when detaching - clear only our input.
 * The model is preserved, because it's considered secure enough.
 */
public class SecureModelPasswordTextField extends PasswordTextField {

	public SecureModelPasswordTextField(String id, IModel<String> model) {
		super(id, model);
		setResetPassword(false);
	}

	@Override
	protected void onDetach() {
		clearInput();
		super.onDetach();
	}
}
