package com.evolveum.midpoint.web.page.admin.users;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;

public class SubmitPage extends PageAdmin {

	public SubmitPage() {

		initLayout();
	}

	private void initLayout() {
		
		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("submitPage.confirmText", "test01")));

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);
		mainForm.add(accordion);

		AccordionItem accountsList = new AccordionItem("accountsList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("submitPage.accountsList");
			}
		});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);
		
		AccordionItem changesList = new AccordionItem("changesList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("submitPage.changesList");
			}
		});
		changesList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(changesList);
		
		Accordion changeType = new Accordion("changeType");
		changeType.setMultipleSelect(true);
		changeType.setOpenedPanel(-1);
		changesList.getBodyContainer().add(changeType);
		
		initUserInfo(changeType);
		initAccounts(changeType);

		initButtons(mainForm);

	}
	
	private void initUserInfo(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("submitPage.userInfoAccordion");
			}
		});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);
	}
	
	private void initAccounts(Accordion changeType) {
		AccordionItem accountsAccordion = new AccordionItem("accountsAccordion", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("submitPage.accountsAccordion");
			}
		});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton", ButtonType.POSITIVE,
				createStringResource("submitPage.button.save")) {
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}
			
			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);
		
		AjaxLinkButton returnButton = new AjaxLinkButton("returnButton",
				createStringResource("submitPage.button.return")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO setResponsePage(PageUser.class);
			}
		};
		mainForm.add(returnButton);
		
		AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
				createStringResource("submitPage.button.cancel")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageUsers.class);
			}
		};
		mainForm.add(cancelButton);
	}
	
	private void savePerformed(AjaxRequestTarget target) {
		// TODO
	}
}
