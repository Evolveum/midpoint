package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.web.component.util.BasePanel;

public class CapabilitiesPanel extends BasePanel<CapabilitiesDto>{
	
	private static final String ID_ACTIVATION = "activation";
	private static final String ID_CREDENTIALS = "credentials";
	private static final String ID_LIVE_SYNC = "liveSync";
	private static final String ID_TEST = "test";
	private static final String ID_SCHEMA = "schema";
	private static final String ID_CREATE = "create";
	private static final String ID_UPDATE = "update";
	
	private static final String ID_ADD_ATTRIBUE_VALUES = "addAttributeValues";
	private static final String ID_REMOVE_ATTRIBUTE_VALUES = "removeAttributeValues";
	private static final String ID_DELETE = "delete";
	private static final String ID_READ = "read";
	private static final String ID_AUXILIARY_OBJECT_CLASS = "auxiliaryObjectClass";
	private static final String ID_CONNECTOR_SCRIPT = "connectorScript";
	private static final String ID_HOST_SCRIPT = "hostScript";
	


	
	private static final long serialVersionUID = 1L;

	public CapabilitiesPanel(String id, IModel<CapabilitiesDto> model) {
		super(id, model);
		
		initLayout();
		
	}

	    
	    private void initLayout() {
	    	
	    	createCapabilityButton(ID_ACTIVATION);
	    	createCapabilityButton(ID_CREDENTIALS);
	    	createCapabilityButton(ID_LIVE_SYNC);
	    	createCapabilityButton(ID_TEST);
	    	createCapabilityButton(ID_SCHEMA);
	    	createCapabilityButton(ID_CREATE);
	    	createCapabilityButton(ID_UPDATE);
	    	createCapabilityButton(ID_ADD_ATTRIBUE_VALUES);
	    	createCapabilityButton(ID_REMOVE_ATTRIBUTE_VALUES);
	    	createCapabilityButton(ID_DELETE);
	    	createCapabilityButton(ID_READ);
	    	createCapabilityButton(ID_AUXILIARY_OBJECT_CLASS);
	    	createCapabilityButton(ID_CONNECTOR_SCRIPT);
	    	createCapabilityButton(ID_HOST_SCRIPT);
	    	
	    	    }

	    private void createCapabilityButton(String id){
	    	AjaxLink<Boolean> button = new AjaxLink<Boolean>(id, new PropertyModel<Boolean>(getModel(), id)){
	    		 
	    		@Override
	    		public void onClick(AjaxRequestTarget target) {
	    			//TODO: 
	    		}
	    		
	    	};
	    	
	    	button.add(new AttributeModifier("class", button.getModelObject() ? "btn btn-app bg-green" : "btn btn-app bg-red"));
	    	
	    	button.add(new Label("label", getString("CapabilitiesType."+id)));
	    	
	    	add(button);
	    }
	    
	    
}
