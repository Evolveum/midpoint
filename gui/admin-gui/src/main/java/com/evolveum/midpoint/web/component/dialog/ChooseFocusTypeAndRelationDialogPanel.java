package com.evolveum.midpoint.web.component.dialog;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ChooseFocusTypeAndRelationDialogPanel extends BasePanel implements Popupable{

	private static final String ID_OBJECT_TYPE = "type";
	private static final String ID_RELATION = "relation";
	private static final String ID_BUTTON_OK = "ok";

	public ChooseFocusTypeAndRelationDialogPanel(String id) {
		super(id);
		
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout(){
		
		DropDownFormGroup<QName> type = new DropDownFormGroup<QName>(ID_OBJECT_TYPE, Model.of(UserType.COMPLEX_TYPE), new ListModel<>(WebComponentUtil.createFocusTypeList()), 
				new QNameObjectTypeChoiceRenderer(), createStringResource("chooseFocusTypeAndRelationDialogPanel.type"), 
				"chooseFocusTypeAndRelationDialogPanel.tooltip.type", true, "col-md-4", "col-md-8", false);
		type.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
	    type.setOutputMarkupId(true);
	    add(type);
	    
	    	IModel<Map<String, String>> options = new Model(null);
            Map<String, String> optionsMap = new HashMap<>();
//            optionsMap.put("nonSelectedText", createStringResource("LoggingConfigPanel.appenders.Inherit").getString());
            options.setObject(optionsMap);
		ListMultipleChoicePanel<QName> relation = new ListMultipleChoicePanel<QName>(ID_RELATION, new ListModel<>(),
				new ListModel<QName>(getSupportedRelations()), new QNameObjectTypeChoiceRenderer(), options);
	    relation.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
	    relation.setOutputMarkupId(true);
	    add(relation);

		AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				DropDownFormGroup<QName> type = (DropDownFormGroup<QName>) getParent().get(ID_OBJECT_TYPE);
				QName typeChosen = type.getModelObject();

				ListMultipleChoicePanel<QName> relation = (ListMultipleChoicePanel<QName>) getParent().get(ID_RELATION);
				Collection<QName> relationChosen = relation.getModelObject();
				ChooseFocusTypeAndRelationDialogPanel.this.okPerformed(typeChosen, relationChosen, target);
				getPageBase().hideMainPopup(target);

			}
		};

		add(confirmButton);

		

	}

	protected void okPerformed(QName type, Collection<QName> relation, AjaxRequestTarget target) {
		// TODO Auto-generated method stub

	}
	
	protected List<QName> getSupportedRelations() {
		return WebComponentUtil.getAllRelations(getPageBase());
	}

	@Override
	public int getWidth() {
		return 400;
	}

	@Override
	public int getHeight() {
		return 300;
	}

	@Override
	public String getWidthUnit(){
		return "px";
	}

	@Override
	public String getHeightUnit(){
		return "px";
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
