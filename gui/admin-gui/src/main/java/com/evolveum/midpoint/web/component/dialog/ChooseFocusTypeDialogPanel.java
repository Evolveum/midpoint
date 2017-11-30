package com.evolveum.midpoint.web.component.dialog;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.List;

public class ChooseFocusTypeDialogPanel extends BasePanel implements Popupable{

	private static final String ID_OBJECT_TYPE = "type";
	private static final String ID_BUTTON_OK = "ok";

	public ChooseFocusTypeDialogPanel(String id) {
		super(id);
		initLayout();
	}

	private void initLayout(){
		DropDownChoice<QName> type = new DropDownChoice<QName>(ID_OBJECT_TYPE, Model.of(UserType.COMPLEX_TYPE),
				WebComponentUtil.createFocusTypeList(), new QNameObjectTypeChoiceRenderer());
		type.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
	        type.setOutputMarkupId(true);
	        add(type);

	        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {


				@Override
				public void onClick(AjaxRequestTarget target) {
					DropDownChoice<QName> type = (DropDownChoice<QName>) getParent().get(ID_OBJECT_TYPE);
					QName typeChosen = type.getModelObject();
					ChooseFocusTypeDialogPanel.this.okPerformed(typeChosen, target);

				}
			};

			add(confirmButton);
	}

	protected void okPerformed(QName type, AjaxRequestTarget target) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getWidth() {
		return 300;
	}

	@Override
	public int getHeight() {
		return 100;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("ChooseFocusTypeDialogPanel.chooseType");
	}

	@Override
	public Component getComponent() {
		return this;
	}


}
