package com.evolveum.midpoint.web.component.prism;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

public class PrismContainerValueHeaderPanel<C extends Containerable> extends PrismHeaderPanel<ContainerValueWrapper<C>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_SHOW_METADATA = "showMetadata";
    private static final String ID_SHOW_EMPTY_FIELDS = "showEmptyFields";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";

	
	public PrismContainerValueHeaderPanel(String id, IModel<ContainerValueWrapper<C>> model) {
		super(id, model);
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
        		containerValueWrapper.sort(getPageBase());

                onButtonClick(target);
            }

        	@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isSorted();
			}
        };
        sortPropertiesButton.add(buttonsVisibleBehaviour);
        add(sortPropertiesButton);
		
        ToggleIconButton addChildContainerButton = new ToggleIconButton(ID_ADD_CHILD_CONTAINER,
        		GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS, GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS) {
        	private static final long serialVersionUID = 1L;

        	@Override
            public void onClick(AjaxRequestTarget target) {
            }

        	@Override
			public boolean isOn() {
				return PrismContainerValueHeaderPanel.this.getModelObject().isSorted();
			}
        };
		addChildContainerButton.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().containsMultivalueContainer();
			}
		});
        add(addChildContainerButton);

	}
	
	@Override
	protected String getLabel() {
		return getModel().getObject().getDisplayName();
	}
	
	private void onShowEmptyClick(AjaxRequestTarget target) {
		
		ContainerValueWrapper<C> wrapper = PrismContainerValueHeaderPanel.this.getModelObject();
		wrapper.setShowEmpty(!wrapper.isShowEmpty(), false);
			
		onButtonClick(target);
		
	}


}
