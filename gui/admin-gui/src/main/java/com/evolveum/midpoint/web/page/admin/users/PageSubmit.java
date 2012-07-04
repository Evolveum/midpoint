package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.delta.ObjectDeltaPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;

public class PageSubmit extends PageAdmin {
	private ObjectDeltaPanel objectDelta;

	public PageSubmit() {
		objectDelta = (ObjectDeltaPanel) getSession().getAttribute("deltaPanel");
		
		if(objectDelta == null) {
			getSession().error(getString("pageSubmit.message.cantLoadData"));
        	throw new RestartResponseException(PageUsers.class);
        } else {
        	getSession().setAttribute("deltaPanel", null);
        }
		
		initLayout();
	}

	private void initLayout() {
		
		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("pageSubmit.confirmText", new AbstractReadOnlyModel<String>(){

			@Override
			public String getObject() {
				return objectDelta.getOldObject().getDisplayName();
			}
			
		})));

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);
		mainForm.add(accordion);

		AccordionItem accountsList = new AccordionItem("accountsList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.accountsList");
			}
		});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);
		
		List<IColumn<PrismReferenceValue>> columns = new ArrayList<IColumn<PrismReferenceValue>>();
		columns.add(new PropertyColumn(createStringResource("aaaaa"), "object"));
		/*columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.token"), "token"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.status"), "status"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));*/
		SortableDataProvider<PrismReferenceValue> provider = new ListDataProvider<PrismReferenceValue>(this, new AbstractReadOnlyModel<List<PrismReferenceValue>>() {

			@Override
			public List<PrismReferenceValue> getObject() {
				return loadAccountsList();
			}
		});
		TablePanel accountsTable = new TablePanel<PrismReferenceValue>("accountsTable", provider, columns);
		accountsTable.setShowPaging(false);
		accountsTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(accountsTable);
		
		AccordionItem changesList = new AccordionItem("changesList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.changesList");
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
		initAssignments(changeType);

		initButtons(mainForm);

	}
	
	private void initUserInfo(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.userInfoAccordion");
			}
		});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);
	}
	
	private void initAccounts(Accordion changeType) {
		AccordionItem accountsAccordion = new AccordionItem("accountsAccordion", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.accountsAccordion");
			}
		});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);
	}
	
	private void initAssignments(Accordion changeType) {
		AccordionItem assignmentsAccordion = new AccordionItem("assignmentsAccordion", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.assignmentsAccordion");
			}
		});
		assignmentsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(assignmentsAccordion);
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton", ButtonType.POSITIVE,
				createStringResource("pageSubmit.button.save")) {
			
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
				createStringResource("pageSubmit.button.return")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO setResponsePage(PageUser.class);
			}
		};
		mainForm.add(returnButton);
		
		AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
				createStringResource("pageSubmit.button.cancel")) {
			
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
	
	private List<PrismReferenceValue> loadAccountsList() {
		ReferenceDelta accounts = objectDelta.getAccountsDelta();
		List<PrismReferenceValue> list = new ArrayList<PrismReferenceValue>();
		
		if(!accounts.getValuesToAdd().isEmpty()) {
			for (PrismReferenceValue value : accounts.getValuesToAdd()) {
				list.add(value);
			}
			return list;
		}
		return new ArrayList<PrismReferenceValue>();
	}
	
	private List<ItemDelta> loadAssignmentsList() {
		ContainerDelta assignments = objectDelta.getAssignmentsDelta();
		List<ItemDelta> list = new ArrayList<ItemDelta>();
		
		if(!assignments.getValuesToAdd().isEmpty()) {
			for (Object value : assignments.getValuesToAdd()) {
				//list.add(value);
			}
			return list;
		}
		return new ArrayList<ItemDelta>();
	}
	
	private List<ItemDelta> loadPersonalInformation() {
		PrismObject oldObject = objectDelta.getOldObject();
		ObjectDelta newObject = objectDelta.getDelta();
		return new ArrayList<ItemDelta>();
	}
}
