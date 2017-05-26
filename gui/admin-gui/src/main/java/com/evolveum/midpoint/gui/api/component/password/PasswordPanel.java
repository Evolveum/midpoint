/*
 * Copyright (c) 2010-2016 Evolveum
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

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.self.PageSelfProfile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_LINK_CONTAINER = "linkContainer";
	private static final String ID_PASSWORD_SET = "passwordSet";
	private static final String ID_PASSWORD_REMOVE = "passwordRemove";
	private static final String ID_CHANGE_PASSWORD_LINK = "changePasswordLink";
	private static final String ID_REMOVE_PASSWORD_LINK = "removePasswordLink";
	private static final String ID_REMOVE_BUTTON_CONTAINER = "removeButtonContainer";
	private static final String ID_INPUT_CONTAINER = "inputContainer";
    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_PASSWORD_TWO = "password2";
    
    private static final Trace LOGGER = TraceManager.getTrace(PasswordPanel.class);
    
    private boolean passwordInputVisble;

    public PasswordPanel(String id, IModel<ProtectedStringType> model) {
        this(id, model, false);
    }

    public PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly) {
        super(id);
        this.passwordInputVisble = model.getObject() == null;
        initLayout(model, isReadOnly);
    }
    
    public PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible) {
        super(id);
        this.passwordInputVisble = isInputVisible;
        initLayout(model, isReadOnly);
    }

    private void initLayout(final IModel<ProtectedStringType> model, final boolean isReadOnly) {
    	setOutputMarkupId(true);
		final WebMarkupContainer inputContainer = new WebMarkupContainer(ID_INPUT_CONTAINER) {
			@Override
			public boolean isVisible() {
				return passwordInputVisble;
			}
		};
		inputContainer.setOutputMarkupId(true);
		add(inputContainer);
    	
        final PasswordTextField password1 = new PasswordTextField(ID_PASSWORD_ONE, new PasswordModel(model));
        password1.setRequired(false);
        password1.setResetPassword(false);
        password1.setOutputMarkupId(true);
        password1.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        inputContainer.add(password1);

        final PasswordTextField password2 = new PasswordTextField(ID_PASSWORD_TWO, new Model<String>());
        password2.setRequired(false);
        password2.setResetPassword(false);
        password2.setOutputMarkupId(true);
        password2.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        inputContainer.add(password2);
        
        password1.add(new AjaxFormComponentUpdatingBehavior("change") {
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
        
        final WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LINK_CONTAINER) {
			@Override
			public boolean isVisible() {
				return !passwordInputVisble;
			}
		};
		inputContainer.setOutputMarkupId(true);
        linkContainer.setOutputMarkupId(true);
		add(linkContainer);
		
		final Label passwordSetLabel = new Label(ID_PASSWORD_SET, new ResourceModel("passwordPanel.passwordSet"));
		linkContainer.add(passwordSetLabel);
        
		final Label passwordRemoveLabel = new Label(ID_PASSWORD_REMOVE, new ResourceModel("passwordPanel.passwordRemoveLabel"));
        passwordRemoveLabel.setVisible(false);
		linkContainer.add(passwordRemoveLabel);

        AjaxLink link = new AjaxLink(ID_CHANGE_PASSWORD_LINK) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				onLinkClick(target);
			}
			@Override
			public boolean isVisible() {
				return !passwordInputVisble;
			}
		};
        link.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }
        });
        link.setBody(new ResourceModel("passwordPanel.passwordChange"));
		link.setOutputMarkupId(true);
		linkContainer.add(link);

        final WebMarkupContainer removeButtonContainer = new WebMarkupContainer(ID_REMOVE_BUTTON_CONTAINER);
        AjaxLink removePassword = new AjaxLink(ID_REMOVE_PASSWORD_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemovePassword(model, target);
            }

        };
        removePassword.add(new VisibleEnableBehaviour() {
        	
        	@Override
        	public boolean isVisible() {
        		PageBase pageBase = (PageBase)getPage();
                if (pageBase == null){
                    return false;
                }
                if (pageBase instanceof PageSelfProfile){
                    return false;
                }
                if (pageBase instanceof PageUser
                        && model.getObject() != null && !model.getObject().isEmpty()){
                    return true;
                }
                return false;
        	}
        });
        removePassword.setBody(new ResourceModel("passwordPanel.passwordRemove"));
        removePassword.setOutputMarkupId(true);
        removeButtonContainer.add(removePassword);
        add(removeButtonContainer);
    }
    
	private void onLinkClick(AjaxRequestTarget target) {
    	passwordInputVisble = true;
    	target.add(this);
    }

	private void onRemovePassword(IModel<ProtectedStringType> model, AjaxRequestTarget target) {
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_SET).setVisible(false);
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_REMOVE).setVisible(true);
        passwordInputVisble = false;
        target.add(this);
        model.setObject(null);
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<FormComponent>();
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE));
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_TWO));
        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
		return (FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE);
    }

    private static class PasswordValidator implements IValidator<String> {

        private PasswordTextField p1;
        private PasswordTextField p2;

        private PasswordValidator(PasswordTextField p1, PasswordTextField p2) {
            Validate.notNull(p1, "Password field one must not be null.");
            Validate.notNull(p2, "Password field two must not be null.");
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            String s1 = p1.getValue();
            String s2 = p2.getValue();

            if (StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
                return;
            }
            
            boolean equal = s1 != null ? s1.equals(s2) : s2 == null;
            if (!equal) {
            	validatable = p1.newValidatable();
            	ValidationError err = new ValidationError();
    			err.addKey("passwordPanel.error");
    			validatable.error(err);
            }
        }
    }
    
    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }
    
    private class PasswordModel implements IModel<String> {

    	IModel<ProtectedStringType> psModel;
    	
    	PasswordModel(IModel<ProtectedStringType> psModel) {
    		this.psModel = psModel;
    	}
    	
		@Override
		public void detach() {
			// Nothing to do
		}

		@Override
		public String getObject() {
			if (psModel.getObject() == null) {
				return null;
			} else {
				return psModel.getObject().getClearValue();
			}
		}

		@Override
		public void setObject(String object) {
			if (object == null) {
				psModel.setObject(null);
			} else {
				if (psModel.getObject() == null) {
					psModel.setObject(new ProtectedStringType());
				}
				psModel.getObject().setClearValue(object);
			}
		}
    	
    }
}
