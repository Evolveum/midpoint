package com.evolveum.midpoint.gui.api.component.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class ItemPathPanel extends BasePanel<ItemPathDto> {

	private static final long serialVersionUID = 1L;

	private static final String ID_NAMESPACE = "namespace";
	private static final String ID_DEFINITION = "definition";

	private static final String ID_PLUS = "plus";
	private static final String ID_MINUS = "minus";

	public ItemPathPanel(String id, IModel<ItemPathDto> model, PageBase parent) {
		super(id, model);

		setParent(parent);


		initLayout();

	}

	public ItemPathPanel(String id, ItemPathDto model, PageBase parent) {
		this(id, Model.of(model), parent);

	}

	private void initLayout() {
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
		add(itemDefPanel);

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
		add(plusButton);

		AjaxButton minusButton = new AjaxButton(ID_MINUS) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				ItemPathDto path = ItemPathPanel.this.getModelObject();
//				ItemPathDto parent = null;
//				if (path.getItemDef() == null){
//					parent = path.getParentPath();
//				} else {
//					parent = path;
//				}
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
		add(minusButton);

		DropDownChoicePanel<QName> namespacePanel = new DropDownChoicePanel<QName>(ID_NAMESPACE,
				new PropertyModel<QName>(getModel(), "objectType"),
				new ListModel<QName>(WebComponentUtil.createObjectTypeList()), new QNameChoiceRenderer());
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
		add(namespacePanel);
	}

	private void refreshItemPathPanel(ItemPathDto itemPathDto, boolean isAdd, AjaxRequestTarget target) {
		ItemPathSegmentPanel pathSegmentPanel = (ItemPathSegmentPanel) get(ID_DEFINITION);
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
		// target.add(pathSegmentPanel);

	}

	private void refreshItemPath(ItemPathDto itemPathDto, AjaxRequestTarget target) {

		this.getModel().setObject(itemPathDto);
		target.add(this);
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

}
