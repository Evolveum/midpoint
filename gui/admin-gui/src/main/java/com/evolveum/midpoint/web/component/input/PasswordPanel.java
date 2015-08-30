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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.users.PageAdminUsers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {

    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_PASSWORD_TWO = "password2";

    public PasswordPanel(String id, IModel<String> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(IModel<String> model) {
        final PasswordTextField password1 = new PasswordTextField(ID_PASSWORD_ONE, model);
        password1.setRequired(false);
        password1.setResetPassword(false);
        password1.setOutputMarkupId(true);
        password1.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(password1);

        final PasswordTextField password2 = new PasswordTextField(ID_PASSWORD_TWO, new Model<String>());
        password2.setRequired(false);
        password2.setResetPassword(false);
        password2.setOutputMarkupId(true);
        password2.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(password2);
        
        password1.add(new AjaxFormComponentUpdatingBehavior("onChange") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				boolean required = !StringUtils.isEmpty(password1.getModel().getObject());
				password2.setRequired(required);
                //fix of MID-2463
//				target.add(password2);
//				target.appendJavaScript("$(\"#"+ password2.getMarkupId() +"\").focus()");
			}
		});
        password2.add(new PasswordValidator(password1, password2));
        
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<FormComponent>();
        list.add((FormComponent) get(ID_PASSWORD_ONE));
        list.add((FormComponent) get(ID_PASSWORD_TWO));

        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_PASSWORD_ONE);
    }

    private static class PasswordValidator extends AbstractValidator {

        private PasswordTextField p1;
        private PasswordTextField p2;

        private PasswordValidator(PasswordTextField p1, PasswordTextField p2) {
            Validate.notNull(p1, "Password field one must not be null.");
            Validate.notNull(p2, "Password field two must not be null.");
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        protected void onValidate(IValidatable validatable) {
            String s1 = p1.getValue();
            String s2 = p2.getValue();

            if (StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
                return;
            }
            
            boolean equal = s1 != null ? s1.equals(s2) : s2 == null;
            if (!equal) {
                error(p1.newValidatable(), "passwordPanel.error");
            }
        }
    }
    
    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }
}
