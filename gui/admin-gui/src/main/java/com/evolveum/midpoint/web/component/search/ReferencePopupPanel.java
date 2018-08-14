package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;

public class ReferencePopupPanel<T extends Serializable> extends SearchPopupPanel<T>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_OID = "oid";
	private static final String ID_TYPE = "type";
	private static final String ID_RELATION = "relation";
	
	
	public ReferencePopupPanel(String id, IModel<T> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		
		TextField<String> oidField = new TextField<String>(ID_OID, new PropertyModel<>(getModel(), SearchValue.F_VALUE + ".oid"));
		
		oidField.add(new AjaxFormComponentUpdatingBehavior("blur") {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				
			}
		});
		oidField.setOutputMarkupId(true);
		add(oidField);
		
		DropDownChoice<QName> type = new DropDownChoice<>(ID_TYPE, new PropertyModel<QName>(getModel(), SearchValue.F_VALUE + ".type"),
				WebComponentUtil.createFocusTypeList(), new QNameObjectTypeChoiceRenderer()); 
        type.setNullValid(true);
        type.setOutputMarkupId(true);
        add(type);
        
        DropDownChoice<QName> relation = new DropDownChoice<>(ID_RELATION, new PropertyModel<QName>(getModel(), SearchValue.F_VALUE + ".relation"),
				WebComponentUtil.getAllRelations(getPageBase()), new QNameObjectTypeChoiceRenderer()); 
        relation.setNullValid(true);
        relation.setOutputMarkupId(true);
        add(relation);
	}


}
