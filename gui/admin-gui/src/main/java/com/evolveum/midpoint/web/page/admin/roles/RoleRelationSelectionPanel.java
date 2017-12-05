package com.evolveum.midpoint.web.page.admin.roles;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.roles.RoleGovernanceRelationsPanel.RoleRelationSelectionDto;

public abstract class RoleRelationSelectionPanel extends BasePanel<RoleRelationSelectionDto> implements Popupable{

	private static final long serialVersionUID = 1L;
	
	IModel<RoleRelationSelectionDto> modelObject;
	
	private static final String ID_MANAGER = "manager";
	private static final String ID_OWNER = "owner";
	private static final String ID_APPROVER = "approver";
	
	private static final String ID_OK = "okButton";
	private static final String ID_CANCEL = "cancelButton";

	public RoleRelationSelectionPanel(String id, RoleRelationSelectionDto selectionConfig) {
		super(id, Model.of(selectionConfig));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		CheckBoxPanel manager = new CheckBoxPanel(ID_MANAGER, new PropertyModel<>(getModel(), "manager"));
		manager.setOutputMarkupId(true);
		add(manager);
		
		CheckBoxPanel owner = new CheckBoxPanel(ID_OWNER, new PropertyModel<>(getModel(), "owner"));
		owner.setOutputMarkupId(true);
		add(owner);
		
		CheckBoxPanel approver = new CheckBoxPanel(ID_APPROVER, new PropertyModel<>(getModel(), "approver"));
		approver.setOutputMarkupId(true);
		add(approver);
		
		AjaxButton ok = new AjaxButton(ID_OK, createStringResource("Button.ok")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onConfirmPerformed(RoleRelationSelectionPanel.this.getModel(), target);
			}

		};
		
		add(ok);
		
		AjaxButton cancel = new AjaxButton(ID_CANCEL, createStringResource("Button.cancel")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancelPerformed(target);
			}
		};
		
		add(cancel);
		
	}
	
	protected abstract void onConfirmPerformed(IModel<RoleRelationSelectionDto> model, AjaxRequestTarget target);


	private void onCancelPerformed(AjaxRequestTarget target) {
		getPageBase().hideMainPopup(target);		
	}
	
	@Override
	public int getWidth() {
		return 300;
	}

	@Override
	public int getHeight() {
		return 400;
	}

	@Override
	public StringResourceModel getTitle() {
		return createStringResource("RoleRelationSelectionPanel.select.relation");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
