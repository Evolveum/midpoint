package com.evolveum.midpoint.gui.api.component.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteItemDefinitionPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

public class ItemPathSegmentPanel extends BasePanel<ItemPathDto> {

	private static final long serialVersionUID = 1L;

	private static final String ID_DEFINITION = "definition";
	private static final String ID_PARENT = "parentPath";

	public ItemPathSegmentPanel(String id, ItemPathDto model) {
		this(id, Model.of(model));
	}
	
	public ItemPathSegmentPanel(String id, IModel<ItemPathDto> model) {
		super(id, model);

		initLayout();
	}

	private void initLayout() {

		Label label = new Label(ID_PARENT,
				new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;
				@Override
				public String getObject() {
					
					if (getModelObject().getParentPath() == null) {
						return null;
					}
					
					if (getModelObject().getParentPath().toItemPath() == null) {
						return null;
					}
					
					return getString("ItemPathSegmentPanel.itemToSearch", getModelObject().getParentPath().toItemPath().toString());
				}
				});

		label.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModelObject().getParentPath() != null && getModelObject().getParentPath().toItemPath() != null;
			}
		});
		label.setOutputMarkupId(true);
		add(label);

		final AutoCompleteItemDefinitionPanel itemDefPanel = new AutoCompleteItemDefinitionPanel(
				ID_DEFINITION, new PropertyModel<ItemDefinition<?>>(getModel(), "itemDef")) {
			private static final long serialVersionUID = 1L;

			protected Map<String, ItemDefinition<?>> listChoices(String input) {
				return collectAvailableDefinitions(input);

			}
		};
//		itemDefPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		itemDefPanel.setOutputMarkupId(true);
		add(itemDefPanel);
	}

	private Map<String, ItemDefinition<?>> collectAvailableDefinitions(String input) {
		Map<String, ItemDefinition<?>> toSelect = new HashMap<>();
		ItemPathDto parentItem = getModelObject().getParentPath();
		if (parentItem != null) {
			if (parentItem.getItemDef() instanceof PrismContainerDefinition<?>) {
				PrismContainerDefinition<?> parentContainer = (PrismContainerDefinition<?>) parentItem.getItemDef();
				collectItems(parentContainer.getDefinitions(), input, toSelect);
			}
		} else {
			Collection<ItemDefinition<?>> definitions = getSchemaDefinitionMap().get(getModelObject().getObjectType());
			collectItems(definitions, input, toSelect);
		}
		return toSelect;
	}

	private void collectItems(Collection<? extends ItemDefinition> definitions, String input, Map<String, ItemDefinition<?>> toSelect) {
		if (definitions == null) {
			return;
		}
		for (ItemDefinition<?> def : definitions) {
			if (StringUtils.isBlank(input)) {
				toSelect.put(def.getName().getLocalPart(), def);
			} else {
				if (def.getName().getLocalPart().startsWith(input)) {
					toSelect.put(def.getName().getLocalPart(), def);
				}
			}
		}
	}

	public void refreshModel(ItemPathDto newModel) {
		getModel().setObject(newModel);
	}

	protected Map<QName, Collection<ItemDefinition<?>>> getSchemaDefinitionMap() {
		return new HashMap<>();
	}
	
	public boolean validate() {
//		AutoCompleteItemDefinitionPanel autocompletePanel = (AutoCompleteItemDefinitionPanel) get(ID_DEFINITION);
//		String current = (String) autocompletePanel.getBaseFormComponent().getModelObject();
//		if (getModelObject().getParentPath() != null && StringUtils.isNotBlank(current)) {
//			ItemDefinition<?> def = getModelObject().getParentPath().getItemDef();
//			if (def.getName().getLocalPart())
//			( !=
//		}
		return getModelObject().getItemDef() != null;
		
//		return autocompletePanel.getBaseFormComponent().getModelObject() != null;
	}

}
