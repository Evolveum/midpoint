package com.evolveum.midpoint.web.component.dialog;

	
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


	import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
	import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
	import org.apache.wicket.markup.html.WebMarkupContainer;
	import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
	import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

	/**
	 * @author katkav
	 */
	public class ChooseResourceDefinitionDialog extends ModalWindow {

//	    private static final String ID_CONFIRM_TEXT = "confirmText";
	    private static final String ID_YES = "yes";

	    
//	   private Model resourcesToChoose;

	    public ChooseResourceDefinitionDialog(String id) {
	        this(id, null, null);
	    }

	    public ChooseResourceDefinitionDialog(String id, IModel<String> title, Model model) {
	        super(id);
	        if (title != null) {
	            setTitle(title);
	        }
	        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
	        setCookieName(ConfirmationDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
	        showUnloadConfirmation(false);
	        setResizable(false);
	        setInitialWidth(350);
	        setInitialHeight(150);
	        setWidthUnit("px");


	        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

	            @Override
	            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
	                return true;
	            }
	        });

	        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

	            @Override
	            public void onClose(AjaxRequestTarget target) {
	                ChooseResourceDefinitionDialog.this.close(target);
	            }
	        });

	        WebMarkupContainer content = new WebMarkupContainer(getContentId());
	        setContent(content);

	        initLayout(content, model);
	    }

	    public boolean getLabelEscapeModelStrings(){
	        return true;
	    }

	  

	    private void initLayout(WebMarkupContainer content, final IModel model) {
	      
	    	
	    	CheckBox ldapCheckBox = new CheckBox("ldap", new PropertyModel(model, "ldap"));
	    	ldapCheckBox.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
	    	content.add(ldapCheckBox);
	    	
	    	CheckBox unixCheckBox = new CheckBox("unix", new PropertyModel(model, "unix"));
	    	unixCheckBox.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
	    	content.add(unixCheckBox);
	    	
	        AjaxButton yesButton = new AjaxButton(ID_YES, new StringResourceModel("confirmationDialog.ok",
	                this, null)) {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                yesPerformed(target, model);
	            }
	        };
	        content.add(yesButton);

	       
	    }

	    public void yesPerformed(AjaxRequestTarget target, IModel model) {

	    }

	    public void noPerformed(AjaxRequestTarget target) {
	        close(target);
	    }

	    /**
	     * @return confirmation type identifier
	     */
//	    public int getConfirmType() {
//	        return confirmType;
//	    }

	    /**
	     * This method provides solution for reusing one confirmation dialog for more messages/actions
	     * by using confirmType identifier. See for example {@link com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel}
	     *
	     * @param confirmType
	     */
//	    public void setConfirmType(int confirmType) {
//	        this.confirmType = confirmType;
//	    }
//	}

}
