package com.evolveum.midpoint.gui.api.component.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathPanel extends BasePanel<ItemPathDto> {

	private static final long serialVersionUID = 1L;

	private static final String ID_ITEM_PATH = "itemPath";
	private static final String ID_NAMESPACE = "namespace";
	private static final String ID_DEFINITION = "definition";
	
	private static final String ID_ITEM_PATH_CONTAINER = "itemPathContainer";
	private static final String ID_ITEM_PATH_LABEL = "itemPathLabel";
	private static final String ID_CHANGE = "change";

	private static final String ID_PLUS = "plus";
	private static final String ID_MINUS = "minus";

	public ItemPathPanel(String id, IModel<ItemPathDto> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	public ItemPathPanel(String id, ItemPathDto model) {
		this(id, Model.of(model));

	}
	
	public ItemPathPanel(String id, ItemPathType itemPath) {
		this(id, Model.of(new ItemPathDto(itemPath)));

	}

	private void initLayout() {
		initItemPathPanel();
		
		initItemPathLabel();
		
		setOutputMarkupId(true);
	}
	
	private void initItemPathPanel() {
		WebMarkupContainer itemPathPanel = new WebMarkupContainer(ID_ITEM_PATH);
		itemPathPanel.setOutputMarkupId(true);
		add(itemPathPanel);
		itemPathPanel.add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !getModelObject().isPathDefined();
			}
		
		});
		
		ItemPathSegmentPanel itemDefPanel = new ItemPathSegmentPanel(ID_DEFINITION,
				new AbstractReadOnlyModel<ItemPathDto>() {

					private static final long serialVersionUID = 1L;
					public ItemPathDto getObject() {
						return ItemPathPanel.this.getModelObject();
					}
				}) {

			private static final long serialVersionUID = 1L;

			@Override
			protected Map<QName, Collection<ItemDefinition<?>>> getSchemaDefinitionMap() {
				return initNamspaceDefinitionMap();
			}
		};
		itemDefPanel.setOutputMarkupId(true);
		itemPathPanel.add(itemDefPanel);

		AjaxButton plusButton = new AjaxButton(ID_PLUS) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				refreshItemPathPanel(new ItemPathDto(ItemPathPanel.this.getModelObject()), true, target);

			}

		};
		plusButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				if (getModelObject().getParentPath() == null || getModelObject().getParentPath().toItemPath() == null) {
					return true;
				}
				return (getModelObject().getParentPath().getItemDef() instanceof PrismContainerDefinition);
			}
		});
		plusButton.setOutputMarkupId(true);
		itemPathPanel.add(plusButton);

		AjaxButton minusButton = new AjaxButton(ID_MINUS) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				ItemPathDto path = ItemPathPanel.this.getModelObject();
				refreshItemPathPanel(path, false, target);

			}
		};
		minusButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModelObject().getParentPath() != null && getModelObject().getParentPath().toItemPath() != null;
			}
		});
		minusButton.setOutputMarkupId(true);
		itemPathPanel.add(minusButton);

		DropDownChoicePanel<QName> namespacePanel = new DropDownChoicePanel<>(ID_NAMESPACE,
            new PropertyModel<>(getModel(), "objectType"),
            new ListModel<>(WebComponentUtil.createObjectTypeList()), new QNameChoiceRenderer());
		namespacePanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				refreshItemPath(ItemPathPanel.this.getModelObject(), target);

			}
		});

		namespacePanel.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModelObject().getParentPath() == null || getModelObject().getParentPath().toItemPath() == null;
			}
		});
		namespacePanel.setOutputMarkupId(true);
		itemPathPanel.add(namespacePanel);
	}
	
	private void initItemPathLabel() {
		WebMarkupContainer itemPathLabel = new WebMarkupContainer(ID_ITEM_PATH_CONTAINER);
		itemPathLabel.setOutputMarkupId(true);
		add(itemPathLabel);
		itemPathLabel.add(new VisibleEnableBehaviour() { 
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getModelObject().isPathDefined();
			}
			
		});
		
		TextPanel<ItemPath> textPanel = new TextPanel<>(ID_ITEM_PATH_LABEL, new PropertyModel<>(getModel(), "path"));
		textPanel.setEnabled(false);
		textPanel.setOutputMarkupId(true);
		itemPathLabel.add(textPanel);
		
		AjaxButton change = new AjaxButton(ID_CHANGE, createStringResource("ItemPathPanel.button.reset")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				ItemPathDto newPath = new ItemPathDto();
				ItemPathPanel.this.getModel().setObject(newPath);
				target.add(ItemPathPanel.this);
				onUpdate(newPath);
			}
		};
		
		change.setOutputMarkupId(true);
		itemPathLabel.add(change);
	}

	private void refreshItemPathPanel(ItemPathDto itemPathDto, boolean isAdd, AjaxRequestTarget target) {
		ItemPathSegmentPanel pathSegmentPanel = (ItemPathSegmentPanel) get(createComponentPath(ID_ITEM_PATH, ID_DEFINITION));
		if (isAdd && !pathSegmentPanel.validate()) {
			return;
		}

		if (!isAdd) {
			ItemPathDto newItem = itemPathDto;
			ItemPathDto currentItem = itemPathDto.getParentPath();
			ItemPathDto parentPath = currentItem.getParentPath();
			ItemPathDto resultingItem = null;
			if (parentPath == null) {
				parentPath = new ItemPathDto();
				parentPath.setObjectType(currentItem.getObjectType());
				resultingItem = parentPath;
			} else {
				resultingItem = parentPath;
			}
			newItem.setParentPath(resultingItem);
			itemPathDto = resultingItem;
		}
		// pathSegmentPanel.refreshModel(itemPathDto);
		
		this.getModel().setObject(itemPathDto);

		target.add(this);
		onUpdate(itemPathDto);
		// target.add(pathSegmentPanel);

	}

	private void refreshItemPath(ItemPathDto itemPathDto, AjaxRequestTarget target) {

		this.getModel().setObject(itemPathDto);
		target.add(this);
		
		onUpdate(itemPathDto);
	}

	private Map<QName, Collection<ItemDefinition<?>>> initNamspaceDefinitionMap() {
		Map<QName, Collection<ItemDefinition<?>>> schemaDefinitionsMap = new HashMap<>();
		if (getModelObject().getObjectType() != null) {
			Class clazz = WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
					getModelObject().getObjectType());
			if (clazz != null) {
				PrismObjectDefinition<?> objectDef = getPageBase().getPrismContext().getSchemaRegistry()
						.findObjectDefinitionByCompileTimeClass(clazz);
				Collection<? extends ItemDefinition> defs = objectDef.getDefinitions();
				Collection<ItemDefinition<?>> itemDefs = new ArrayList<>();
				for (Definition def : defs) {
					if (def instanceof ItemDefinition) {
						ItemDefinition<?> itemDef = (ItemDefinition<?>) def;
						itemDefs.add(itemDef);
					}
				}
				schemaDefinitionsMap.put(getModelObject().getObjectType(), itemDefs);
			}
		}
		return schemaDefinitionsMap;
	}
	

	protected void onUpdate(ItemPathDto itemPathDto) {
		
	}

}
