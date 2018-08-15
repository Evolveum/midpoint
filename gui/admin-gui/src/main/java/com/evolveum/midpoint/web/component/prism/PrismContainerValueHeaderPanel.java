package com.evolveum.midpoint.web.component.prism;

import java.util.List;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ObjectPolicyConfigurationTabPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;

public class PrismContainerValueHeaderPanel<C extends Containerable> extends PrismHeaderPanel<ContainerValueWrapper<C>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_SHOW_METADATA = "showMetadata";
    private static final String ID_SHOW_EMPTY_FIELDS = "showEmptyFields";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";
    private static final String ID_REMOVE_CONTAINER = "removeContainer";
    private static final String ID_CHILD_CONTAINERS_SELECTOR_PANEL = "childContainersSelectorPanel";
    private static final String ID_CHILD_CONTAINERS_LIST = "childContainersList";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseContainer";

    private boolean isChildContainersSelectorPanelVisible = false;
    
    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValueHeaderPanel.class);
    private ItemVisibilityHandler isPanelVisible;
	
	public PrismContainerValueHeaderPanel(String id, IModel<ContainerValueWrapper<C>> model, ItemVisibilityHandler isPanelVisible) {
		super(id, model);
		this.isPanelVisible = isPanelVisible;
	}

	@Override
	protected void initButtons() {
		VisibleEnableBehaviour buttonsVisibleBehaviour = new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return PrismContainerValueHeaderPanel.this.isButtonsVisible();
			}
		};

		ToggleIconButton showMetadataButton = new ToggleIconButton(ID_SHOW_METADATA,
				GuiStyleConstants.CLASS_ICON_SHOW_METADATA, GuiStyleConstants.CLASS_ICON_SHOW_METADATA) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
				wrapper.setShowMetadata(!wrapper.isShowMetadata());
				onButtonClick(target);
			}
			
			@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata();
			}
        };
		showMetadataButton.add(new AttributeModifier("title", new AbstractReadOnlyModel() {

			@Override
			public Object getObject() {
				return PrismContainerValueHeaderPanel.this.getModelObject() == null ? "" : (PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata() ?
						createStringResource("PrismObjectPanel.hideMetadata").getString() :
						createStringResource("PrismObjectPanel.showMetadata").getString());
			}
		}));
		showMetadataButton.add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public boolean isVisible() {
				for (ItemWrapper wrapper : getModelObject().getItems()) {
					if (MetadataType.COMPLEX_TYPE.equals(wrapper.getItemDefinition().getTypeName())) {
						return true;
					}
				}
				return false;
			}
			
		});
		add(showMetadataButton);

		ToggleIconButton showEmptyFieldsButton = new ToggleIconButton(ID_SHOW_EMPTY_FIELDS,
				GuiStyleConstants.CLASS_ICON_SHOW_EMPTY_FIELDS, GuiStyleConstants.CLASS_ICON_NOT_SHOW_EMPTY_FIELDS) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onShowEmptyClick(target);
			}
						
			@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isShowEmpty();
			}
        };
		showEmptyFieldsButton.setOutputMarkupId(true);

		showEmptyFieldsButton.add(buttonsVisibleBehaviour);
        add(showEmptyFieldsButton);

        ToggleIconButton sortPropertiesButton = new ToggleIconButton(ID_SORT_PROPERTIES,
        		GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {
        	
        	private static final long serialVersionUID = 1L;

        	@Override
        	public void onClick(AjaxRequestTarget target) {
        		ContainerValueWrapper<C> containerValueWrapper = PrismContainerValueHeaderPanel.this.getModelObject();
        		containerValueWrapper.setSorted(!containerValueWrapper.isSorted());
        		containerValueWrapper.sort();
        		containerValueWrapper.computeStripes();

        		onButtonClick(target);
        	}
        	
        	@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isSorted();
			}
        };
        sortPropertiesButton.add(buttonsVisibleBehaviour);
        add(sortPropertiesButton);
		
        AjaxLink addChildContainerButton = new AjaxLink(ID_ADD_CHILD_CONTAINER) {
        	private static final long serialVersionUID = 1L;

        	@Override
        	public void onClick(AjaxRequestTarget target) {
        		isChildContainersSelectorPanelVisible = true;
				target.add(PrismContainerValueHeaderPanel.this);
        	}
        };
		addChildContainerButton.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().containsMultipleMultivalueContainer() && getModelObject().getContainer() != null
						&& getModelObject().getDefinition().canModify()
						&& !getModelObject().getChildMultivalueContainersToBeAdded(isPanelVisible).isEmpty();
			}
		});
        add(addChildContainerButton);

        List<QName> pathsList = getModelObject().getChildMultivalueContainersToBeAdded(isPanelVisible);
        
       	WebMarkupContainer childContainersSelectorPanel = new WebMarkupContainer(ID_CHILD_CONTAINERS_SELECTOR_PANEL);
		childContainersSelectorPanel.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return pathsList.size() > 1 && isChildContainersSelectorPanelVisible;
			}
		});
		childContainersSelectorPanel.setOutputMarkupId(true);
		add(childContainersSelectorPanel);
		 
		DropDownChoicePanel multivalueContainersList = new DropDownChoicePanel<>(ID_CHILD_CONTAINERS_LIST,
				Model.of(pathsList.size() > 0 ? pathsList.get(0) : null), Model.ofList(pathsList),
				new QNameIChoiceRenderer(getModelObject().getDefinition().getDefaultNamespace()));
		multivalueContainersList.setOutputMarkupId(true);
		multivalueContainersList.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		childContainersSelectorPanel.add(multivalueContainersList);
		childContainersSelectorPanel.add(new AjaxButton(ID_ADD_BUTTON, createStringResource("prismValuePanel.add")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				addNewContainerValuePerformed(ajaxRequestTarget);
			}
		});
		
		AjaxLink removeContainerButton = new AjaxLink(ID_REMOVE_CONTAINER) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				ContainerValueWrapper containerValueWrapper = PrismContainerValueHeaderPanel.this.getModelObject();
				containerValueWrapper.setStatus(ValueStatus.DELETED);
				target.add(PrismContainerValueHeaderPanel.this);
				PrismContainerValueHeaderPanel.this.reloadParentContainerPanel(target);
				onButtonClick(target);
			}
		};
		removeContainerButton.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().getContainer() != null && !getModelObject().getContainer().isShowOnTopLevel() ? getModelObject().getContainer().getItemDefinition().isMultiValue() : false;
			}
		});
		add(removeContainerButton);

	}

	private String getPathDisplayName(QName qName) {
		return getModelObject().getDefinition().getCompileTimeClass().getSimpleName() + "." + qName.getLocalPart();
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
				onShowEmptyClick(target);
			}
		};
		labelComponent.setOutputMarkupId(true);
		labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
		add(labelComponent);

	}
	
	protected void reloadParentContainerPanel(AjaxRequestTarget target){
	}

	protected void addNewContainerValuePerformed(AjaxRequestTarget ajaxRequestTarget){
		isChildContainersSelectorPanelVisible = false;
		getModelObject().setShowEmpty(true, false);
		createNewContainerValue(getModelObject(), getSelectedContainerQName());
		onButtonClick(ajaxRequestTarget);
		ajaxRequestTarget.add(getChildContainersSelectorPanel().getParent());
	}

	private QName getSelectedContainerQName(){
		List<QName> pathsList = getModelObject().getChildMultivalueContainersToBeAdded(isPanelVisible);
		if(pathsList.size() == 1) {
			return pathsList.get(0);
		}
		DropDownChoicePanel<QName> panel = (DropDownChoicePanel)getChildContainersSelectorPanel().get(ID_CHILD_CONTAINERS_LIST);
		return panel.getModel().getObject();
	}

	private WebMarkupContainer getChildContainersSelectorPanel(){
		return (WebMarkupContainer) get(ID_CHILD_CONTAINERS_SELECTOR_PANEL);
	}

	@Override
	public String getLabel() {
		return getModel().getObject().getDisplayName();
	}
	
	private void onShowEmptyClick(AjaxRequestTarget target) {
		
		ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
		wrapper.setShowEmpty(!wrapper.isShowEmpty(), false);
			
		wrapper.computeStripes();
		onButtonClick(target);
		
	}
	
	private void onExpandClick(AjaxRequestTarget target) {
		
		ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
		wrapper.setExpanded(!wrapper.isExpanded());
		onButtonClick(target);
	}
	
	public void createNewContainerValue(ContainerValueWrapper<C> containerValueWrapper, QName path){
		ItemPath newPath = new ItemPath(containerValueWrapper.getPath(), path);
		ContainerWrapper<C> childContainerWrapper = containerValueWrapper.getContainer().findContainerWrapper(newPath);
		
		if (childContainerWrapper == null){
			return;
		}
		boolean isSingleValue = childContainerWrapper.getItemDefinition().isSingleValue();
		if (isSingleValue){
			return;
		}
		PrismContainerValue<C> newContainerValue = childContainerWrapper.getItem().createNewValue();
		
		Task task = getPageBase().createSimpleTask("Creating new container value wrapper");
		
		ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
		ContainerValueWrapper<C> newValueWrapper = factory.createContainerValueWrapper(childContainerWrapper,
				newContainerValue, containerValueWrapper.getObjectStatus(),
				ValueStatus.ADDED, newPath, task);
		newValueWrapper.setShowEmpty(true, false);
		newValueWrapper.computeStripes();
		childContainerWrapper.getValues().add(newValueWrapper);

	}

	@Override
	protected void initExpandCollapseButtons() {
		ToggleIconButton showEmptyFieldsButton = new ToggleIconButton(ID_EXPAND_COLLAPSE_BUTTON,
				GuiStyleConstants.CLASS_BUTTON_TOGGLE_COLLAPSE, GuiStyleConstants.CLASS_BUTTON_TOGGLE_EXPAND) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onExpandClick(target);
			}
						
			@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isExpanded();
			}
        };
		showEmptyFieldsButton.setOutputMarkupId(true);
	}
}
