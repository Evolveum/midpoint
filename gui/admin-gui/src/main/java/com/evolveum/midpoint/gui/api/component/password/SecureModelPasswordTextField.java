/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
