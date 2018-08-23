package com.evolveum.midpoint.web.component.prism;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class PrismContainerHeaderPanel<C extends Containerable> extends PrismHeaderPanel<ContainerWrapper<C>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_EXPAND_COLLAPSE_FRAGMENT = "expandCollapseFragment";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
	
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
	        addButton.add(new VisibleEnableBehaviour() {
				
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isVisible() {
					return isAddButtonVisible();
				}
			});
	        
	        add(addButton);

	}
	
	protected boolean isAddButtonVisible() {
		return true;
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
	
	@Override
	protected WebMarkupContainer initExpandCollapseButton(String contentAreaId) {
		Fragment expandCollapseFragment = new Fragment(contentAreaId, ID_EXPAND_COLLAPSE_FRAGMENT, this);
		
		ToggleIconButton expandCollapseButton = new ToggleIconButton(ID_EXPAND_COLLAPSE_BUTTON,
				GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onExpandClick(target);
			}
						
			@Override
			public boolean isOn() {
				return PrismContainerHeaderPanel.this.getModelObject().isExpanded();
			}
        };
        expandCollapseButton.setOutputMarkupId(true);
        
        expandCollapseFragment.add(expandCollapseButton);
        
        return expandCollapseFragment;
	}
	
	private void onExpandClick(AjaxRequestTarget target) {
		
		ContainerWrapper<C> wrapper = PrismContainerHeaderPanel.this.getModelObject();
		wrapper.setExpanded(!wrapper.isExpanded());
		onButtonClick(target);
	}
	
	@Override
	protected void initHeaderLabel(){
		String displayName = getLabel();
		if (org.apache.commons.lang3.StringUtils.isEmpty(displayName)) {
			displayName = "displayName.not.set";
		}
		
		StringResourceModel headerLabelModel = createStringResource(displayName);
		AjaxButton labelComponent = new AjaxButton(ID_LABEL, headerLabelModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				onExpandClick(target);
			}
		};
		labelComponent.setOutputMarkupId(true);
		labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
		add(labelComponent);

	}
}
