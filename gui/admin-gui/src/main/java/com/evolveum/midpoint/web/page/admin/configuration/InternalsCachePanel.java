package com.evolveum.midpoint.web.page.admin.configuration;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

public class InternalsCachePanel extends BasePanel<Void>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_CLEAR_CACHES_BUTTON = "clearCaches";

	public InternalsCachePanel(String id) {
		super(id);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		AjaxButton clearCustomFunctionCache = new AjaxButton(ID_CLEAR_CACHES_BUTTON, createStringResource("InternalsCachePanel.button.clearCaches")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				getPageBase().getModelInteractionService().clearCaches();
			}
		};
		
		add(clearCustomFunctionCache);
	}
	

}
