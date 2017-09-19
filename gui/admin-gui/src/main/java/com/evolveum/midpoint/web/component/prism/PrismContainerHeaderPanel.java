package com.evolveum.midpoint.web.component.prism;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class PrismContainerHeaderPanel<C extends Containerable> extends PrismHeaderPanel<ContainerWrapper<C>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_ADD_BUTTON = "addButton";
	
	public PrismContainerHeaderPanel(String id, IModel<ContainerWrapper<C>> model) {
		super(id, model);
	
	}

	@Override
	protected void initButtons() {
		
		add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isContainerMultivalue();
			}
		});
		
		 AjaxLink addButton = new AjaxLink(ID_ADD_BUTTON) {
				private static final long serialVersionUID = 1L;

				@Override
	            public void onClick(AjaxRequestTarget target) {
	                addValue(target);
	            }
	        };
	        
	        add(addButton);

	}
	
	private void addValue(AjaxRequestTarget target) {
		getModelObject().addValue(true);
		onButtonClick(target);
	}
	
	private boolean isContainerMultivalue(){
		return getModelObject().isVisible() && getModelObject().getItemDefinition().isMultiValue();
	}
	
	@Override
	protected String getLabel() {
		return getModelObject().getDisplayName();
	}
		

	

	
	
}
