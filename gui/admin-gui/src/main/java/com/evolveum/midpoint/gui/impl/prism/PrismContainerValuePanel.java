/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandlerOld;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 *
 */
public class PrismContainerValuePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends BasePanel<CVW>{

	private static final long serialVersionUID = 1L;
	
	protected static final String ID_LABEL = "label";
	protected static final String ID_LABEL_CONTAINER = "labelContainer";
	protected static final String ID_HELP = "help";
	
	private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_SHOW_METADATA = "showMetadata";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";
    private static final String ID_REMOVE_CONTAINER = "removeContainer";
    
    private static final String ID_PROPERTIES_LABEL = "properties";
    
	 
	public PrismContainerValuePanel(String id, IModel<CVW> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		
		initHeader();
		initValues();
		
	}
	
	private void initHeader() {
		WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        
        add(labelContainer);

        LoadableDetachableModel<String> headerLabelModel = StringResourceModel.of(getModel().getObject()::getDisplayName);
		AjaxButton labelComponent = new AjaxButton(ID_LABEL, headerLabelModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				onExpandClick(target);
			}
		};
		labelComponent.setOutputMarkupId(true);
		labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelContainer.add(new Label(ID_LABEL, LambdaModel.of(getModel(), CVW::getDisplayName)));
        
        labelContainer.add(getHelpLabel());
        
        initButtons();
	}
	
	private  <PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>, IW extends ItemWrapper> void initValues() {
		
		WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
    	propertiesLabel.setOutputMarkupId(true);
    	
    	ListView<IW> properties = new ListView<IW>("properties",
            new PropertyModel<>(getModel(), "nonContainers")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(final ListItem<IW> item) {
				item.setOutputMarkupId(true);
				IW itemWrapper = item.getModelObject();
				Class<?> panelClass = getPageBase().getRegistry().getPanelClass(itemWrapper.getClass());
				
				Constructor<?> constructor;
				try {
					constructor = panelClass.getConstructor(String.class, IModel.class);
					Panel panel = (Panel) constructor.newInstance("property", item.getModel());
					item.add(panel);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SystemException("Cannot instantiate " + panelClass);
				}
				
				
				
	            item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
            }
        };
        properties.setReuseItems(true);
        properties.setOutputMarkupId(true);
        add(propertiesLabel);
       	propertiesLabel.add(properties);
       	
       	
       	
		ListView<IW> containers = new ListView<IW>("containers", new PropertyModel<>(getModel(), "containers")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<IW> item) {
				item.setOutputMarkupId(true);

				item.setOutputMarkupId(true);
				IW itemWrapper = item.getModelObject();
				Class<?> panelClass = getPageBase().getRegistry().getPanelClass(itemWrapper.getClass());

				Constructor<?> constructor;
				try {
					constructor = panelClass.getConstructor(String.class, IModel.class);
					Panel panel = (Panel) constructor.newInstance("container", item.getModel());
					item.add(panel);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SystemException("Cannot instantiate " + panelClass);
				}
				

				
				// containerPanel.add(new VisibleEnableBehaviour() {
				//
				//
				// @Override
				// public boolean isVisible() {
				// if(!model.getObject().isExpanded() &&
				// !model.getObject().getContainer().isShowOnTopLevel()) {
				// return false;
				// }
				//
				//// if( ((ContainerWrapper)item.getModelObject() != null &&
				// ((ContainerWrapper)item.getModelObject()).getItemDefinition()
				// != null
				//// &&
				// ((ContainerWrapper)item.getModelObject()).getItemDefinition().getTypeName()
				// != null
				//// &&
				// ((ContainerWrapper)item.getModelObject()).getItemDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)
				// )
				//// && ( ((ContainerWrapper)item.getModelObject()).getValues()
				// != null &&
				// ((ContainerWrapper)item.getModelObject()).getValues().get(0)
				// != null
				//// &&
				// !((ContainerWrapper<MetadataType>)item.getModelObject()).getValues().get(0).isVisible()
				// ) ){
				//// return false;
				//// }
				//
				// if
				// (model.getObject().containsMultipleMultivalueContainer(isPanalVisible)
				// && item.getModelObject().getItemDefinition().isMultiValue()
				// &&
				// CollectionUtils.isEmpty(item.getModelObject().getValues())) {
				// return false;
				// }
				//
				// return containerPanel.isPanelVisible(isPanalVisible,
				// (IModel<ContainerWrapperImpl<C>>) item.getModel());
				//
				// }
				// });
				// return;
			}
		};

		containers.setReuseItems(true);
		containers.setOutputMarkupId(true);
	}
	
	private <IW extends ItemWrapper<?,?,?,?>> IModel<String> createStyleClassModel(final IModel<IW> wrapper) {
        return new IModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	ItemWrapper<?, ?, ?,?> property = wrapper.getObject();
                return property.isStripe() ? "stripe" : null;
            }
        };
    }
	
	private void initButtons() {
		initMetadataButton();
		initSortButton();
		initAddMoreButton();
		initRemoveButton();
	}
	
	private void onExpandClick(AjaxRequestTarget target) {

		CVW wrapper = getModelObject();
		wrapper.setExpanded(!wrapper.isExpanded());
		refreshPanel(target);
	}
	
	private void refreshPanel(AjaxRequestTarget target) {
		target.add(this);
		target.add(getPageBase().getFeedbackPanel());
	}
	
	protected Label getHelpLabel() {
		
        Label help = new Label(ID_HELP);
        help.add(AttributeModifier.replace("title", LambdaModel.of(getModel(), CVW::getHelpText)));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getHelpText())));
        return help;
	}
	
	private void initMetadataButton() {
		ToggleIconButton<String> showMetadataButton = new ToggleIconButton<String>(ID_SHOW_METADATA,
				GuiStyleConstants.CLASS_ICON_SHOW_METADATA, GuiStyleConstants.CLASS_ICON_SHOW_METADATA) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				onShowMetadataClicked(target);
			}

			@Override
			public boolean isOn() {
				return PrismContainerValuePanel.this.getModelObject().isShowMetadata();
			}
			

        };
		showMetadataButton.add(new AttributeModifier("title", new StringResourceModel("PrismContainerValuePanel.showMetadata.${showMetadata}", getModel()))); 
//				return PrismContainerValueHeaderPanel.this.getModelObject() == null ? "" : (PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata() ?
//						createStringResource("PrismObjectPanel.hideMetadata").getString() :
//						createStringResource("PrismObjectPanel.showMetadata").getString());
		showMetadataButton.add(new VisibleBehaviour(() -> getModelObject().hasMetadata() && shouldBeButtonsShown()));
				
		add(showMetadataButton);

	}
	
	private void initSortButton() {
		ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<String>(ID_SORT_PROPERTIES,
        		GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {
        	
        	private static final long serialVersionUID = 1L;

        	@Override
        	public void onClick(AjaxRequestTarget target) {
        		onSortClicked(target);
        	}
        	
        	@Override
			public boolean isOn() {
				return PrismContainerValuePanel.this.getModelObject().isSorted();
			}
        };
        sortPropertiesButton.add(new VisibleBehaviour(() -> shouldBeButtonsShown()));
        add(sortPropertiesButton);
	}
	
	private void initAddMoreButton() {
		
		 AjaxLink<String> addChildContainerButton = new AjaxLink<String>(ID_ADD_CHILD_CONTAINER, new StringResourceModel("PrismContainerValuePanel.addMore")) {
	        	private static final long serialVersionUID = 1L;

	        	@Override
	        	public void onClick(AjaxRequestTarget target) {
	        		initMoreContainersPopup(target);
	        	}
	        };
	        
			addChildContainerButton.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(getModelObject().getChildContainers())));
			add(addChildContainerButton);

			
	}
	
	private void initMoreContainersPopup(AjaxRequestTarget target) {
		
		
		ListContainersPopup<C, CVW> listContainersPopup = new ListContainersPopup<C, CVW>(getPageBase().getMainPopupBodyId(), getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void processSelectedChildren(List<PrismContainerDefinition<?>> selected) {
				prepareNewContainers(target, selected);
			}

		};

		getPageBase().showMainPopup(listContainersPopup, target);
	}
	
	private void prepareNewContainers(AjaxRequestTarget target, List<PrismContainerDefinition<?>> containers) {
		getPageBase().hideMainPopup(target);
	}
	
	private void initRemoveButton() {
		AjaxLink<Void> removeContainerButton = new AjaxLink<Void>(ID_REMOVE_CONTAINER) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeValuePerformed(target);
			}
		};
		
		add(removeContainerButton);

	}
	
	private void removeValuePerformed(AjaxRequestTarget target) {
		CVW containerValueWrapper = getModelObject();
		containerValueWrapper.setStatus(ValueStatus.DELETED);
		refreshPanel(target);
	}
	
	private boolean shouldBeButtonsShown() {
		return getModelObject().isExpanded();
	}
	
	private void onSortClicked(AjaxRequestTarget target) {
		CVW wrapper = getModelObject();
		wrapper.setSorted(!wrapper.isSorted());
		
		//TODO let the factory do the sorting
//		wrapper.sort();
//		wrapper.computeStripes();

		refreshPanel(target);
	}
	
	private void onShowMetadataClicked(AjaxRequestTarget target) {
		CVW wrapper = getModelObject();
		wrapper.setShowMetadata(!wrapper.isShowMetadata());
		refreshPanel(target);
	}
	
class ContainersPopupDto implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		private boolean selected;
		private PrismContainerDefinition<?> def;
		
		public ContainersPopupDto(boolean selected, PrismContainerDefinition<?> def) {
			this.selected = selected;
			this.def = def;
		}
		
		public String getDisplayName() {
			if (def.getDisplayName() != null) {
				return def.getDisplayName();
			}
			
			return def.getName().getLocalPart();
		}
		
	}
	
	abstract class ListContainersPopup<A extends Containerable, CV extends PrismContainerValueWrapper<A>> extends BasePanel<CV> implements Popupable {

		
		private static final long serialVersionUID = 1L;
		
		private static final String ID_SELECTED = "selected";
		private static final String ID_DEFINITION = "definition";
		private static final String ID_SELECT = "select";
		private static final String ID_CONTAINERS = "containers";
		
		
		public ListContainersPopup(String id, IModel<CV> model) {
			super(id, model);
		}
		
		@Override
		protected void onInitialize() {
			super.onInitialize();
			initLayout();
		}
		
		private void initLayout() {
			
			IModel<List<ContainersPopupDto>> popupModel = new LoadableModel<List<ContainersPopupDto>>() {

				private static final long serialVersionUID = 1L;

				@Override
				protected List<ContainersPopupDto> load() {
					List<PrismContainerDefinition<C>> defs = PrismContainerValuePanel.this.getModelObject().getChildContainers();
					List<ContainersPopupDto> modelObject = new ArrayList<>(defs.size());

					defs.forEach(def -> modelObject.add(new ContainersPopupDto(false, def)));
					return modelObject;
				}
			};
			
			ListView<ContainersPopupDto> listView = new ListView<ContainersPopupDto>(ID_CONTAINERS, popupModel) {

				private static final long serialVersionUID = 1L;
				
				@Override
				protected void populateItem(ListItem<PrismContainerValuePanel<C, CVW>.ContainersPopupDto> item) {
					
					CheckFormGroup checkFormGroup = new CheckFormGroup(ID_SELECTED, new PropertyModel<Boolean>(item.getModel(), "selected"), 
							new StringResourceModel("ListContainersPopup.selected"), "col-md-2", "col-md-10");
					checkFormGroup.getCheck().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
					checkFormGroup.setOutputMarkupId(true);
					item.add(checkFormGroup);
					
					Label definition = new Label(ID_DEFINITION, StringResourceModel.of(item.getModelObject()::getDisplayName));
					definition.setOutputMarkupId(true);
					item.add(definition);
				}
				
				
			};
			
			add(listView);
			
			
			AjaxButton select = new AjaxButton(ID_SELECT, new StringResourceModel("ListContainerPopup.select")) {
				
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					ListView<ContainersPopupDto> listView = (ListView<PrismContainerValuePanel<C, CVW>.ContainersPopupDto>) get(ID_CONTAINERS);
					List<PrismContainerDefinition<?>> selected = new ArrayList<>();
					listView.getModelObject().forEach(child -> selected.add(child.def));
					processSelectedChildren(selected);
				}
			};
			add(select);
		}
		
		protected abstract void processSelectedChildren(List<PrismContainerDefinition<?>> selected);
		
			@Override
		public int getWidth() {
			return 400;
		}

		@Override
		public int getHeight() {
			return 600;
		}

		@Override
		public String getWidthUnit() {
			 return "%";
		}

		@Override
		public String getHeightUnit() {
			 return "%";
		}

		@Override
		public StringResourceModel getTitle() {
			return new StringResourceModel("ListContainersPopup.availableContainers");
		}

		@Override
		public Component getComponent() {
			return this;
		}
		
	}


	
}
