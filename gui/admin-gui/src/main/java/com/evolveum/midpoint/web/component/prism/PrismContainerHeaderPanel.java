package com.evolveum.midpoint.web.component.prism;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.task.api.Task;
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
		ContainerWrapperFactory cwf = new ContainerWrapperFactory(getPageBase());
		ContainerWrapper<C> containerWrapper = getModelObject();
		Task task = getPageBase().createSimpleTask("Creating new container");
		ContainerValueWrapper<C> newContainerValue = cwf.createContainerValueWrapper(containerWrapper,
				containerWrapper.getItem().createNewValue(), containerWrapper.getObjectStatus(), ValueStatus.ADDED,
				containerWrapper.getPath(), task);
		newContainerValue.setShowEmpty(true, false);
		getModelObject().addValue(newContainerValue);
		onButtonClick(target);
	}
	
	private boolean isContainerMultivalue(){
		return getModelObject().isVisible() && getModelObject().getItemDefinition().isMultiValue();
	}
	
	@Override
	public String getLabel() {
		return getModelObject() != null ? getModelObject().getDisplayName() : "";
	}
		

	

	
	
}
