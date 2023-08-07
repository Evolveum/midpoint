/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathPanel extends BasePanel<ItemPathDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM_PATH = "itemPath";
    private static final String ID_NAMESPACE = "namespace";
    private static final String ID_DEFINITION = "definition";
    private static final String ID_SWITCH_TO_TEXT_FIELD_BUTTON = "switchToTextFieldButton";
    private static final String ID_ITEM_PATH_TEXT_FIELD = "itemPathTextField";
    private static final String ID_NAMESPACE_MODE_CONTAINER = "namespaceModeConteiner";

    private static final String ID_ITEM_PATH_CONTAINER = "itemPathContainer";
    private static final String ID_ITEM_PATH_LABEL = "itemPathLabel";
    private static final String ID_CHANGE = "change";

    private static final String ID_PLUS = "plus";
    private static final String ID_MINUS = "minus";

    private boolean switchToTextFieldEnabled = false;

    public enum ItemPathPanelMode{
        NAMESPACE_MODE,
        TEXT_MODE;
    }
    private ItemPathPanelMode panelMode = ItemPathPanelMode.NAMESPACE_MODE;

    public ItemPathPanel(String id, IModel<ItemPathDto> model) {
        this(id, model, false, ItemPathPanelMode.NAMESPACE_MODE);
    }

    public ItemPathPanel(String id, IModel<ItemPathDto> model, boolean switchToTextFieldEnabled, ItemPathPanelMode defaultMode) {
        super(id, model);
        this.switchToTextFieldEnabled = switchToTextFieldEnabled;
        this.panelMode = defaultMode;
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

    @Override
    protected void onConfigure(){
        super.onConfigure();
        if (getModelObject() == null || getModelObject().getItemDef() == null){
            ItemPathSegmentPanel itemPathSegmentPanel = getItemPathSegmentPanel();
            if (itemPathSegmentPanel != null){
                itemPathSegmentPanel.getBaseFormComponent().getDefaultModel().setObject(null);
            }
        }
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

        WebMarkupContainer namespaceModeContainer = new WebMarkupContainer(ID_NAMESPACE_MODE_CONTAINER);
        namespaceModeContainer.setOutputMarkupId(true);
        namespaceModeContainer.add(new VisibleBehaviour(() -> ItemPathPanelMode.NAMESPACE_MODE.equals(panelMode)));
        itemPathPanel.add(namespaceModeContainer);

        ItemPathSegmentPanel itemDefPanel = new ItemPathSegmentPanel(ID_DEFINITION, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Map<QName, Collection<ItemDefinition<?>>> getSchemaDefinitionMap() {
                return initNamspaceDefinitionMap();
            }

            @Override
            protected void onUpdateAutoCompletePanel(AjaxRequestTarget target) {
                ItemPathPanel.this.onUpdate(ItemPathPanel.this.getModelObject());
            }
        };
        itemDefPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        itemDefPanel.setOutputMarkupId(true);
        namespaceModeContainer.add(itemDefPanel);

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
        namespaceModeContainer.add(plusButton);

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
        namespaceModeContainer.add(minusButton);

        DropDownChoicePanel<QName> namespacePanel = new DropDownChoicePanel<>(ID_NAMESPACE,
            new PropertyModel<>(getModel(), "objectType"),
            new ListModel<>(ObjectTypeListUtil.createObjectTypeList()), new QNameObjectTypeChoiceRenderer());
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
        namespaceModeContainer.add(namespacePanel);

        TextPanel<String> itemPathTextField = new TextPanel<String>(ID_ITEM_PATH_TEXT_FIELD, new PropertyModel<>(getModel(), "pathStringValue"));
        itemPathTextField.add(new VisibleBehaviour(() -> isTextFieldVisible()));
        itemPathTextField.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        itemPathPanel.add(itemPathTextField);

        AjaxButton switchButton = new AjaxButton(ID_SWITCH_TO_TEXT_FIELD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                switchButtonClickPerformed(target);
            }

        };
        switchButton.setOutputMarkupId(true);
        switchButton.add(AttributeAppender.append("title", new LoadableModel<String>() {
            @Override
            protected String load() {
                return ItemPathPanelMode.NAMESPACE_MODE.equals(panelMode) ?
                        getPageBase().createStringResource("ItemPathPanel.switchToTextModeTitle").getString()
                        : getPageBase().createStringResource("ItemPathPanel.switchToNamespaceModeTitle").getString();
            }
        }));
        switchButton.add(new VisibleBehaviour(() -> switchToTextFieldEnabled));
        itemPathPanel.add(switchButton);

    }

    protected boolean isTextFieldVisible() {
        return switchToTextFieldEnabled && ItemPathPanelMode.TEXT_MODE.equals(panelMode);
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
        ItemPathSegmentPanel pathSegmentPanel = getItemPathSegmentPanel();
        if (isAdd && !pathSegmentPanel.validate()) {
            return;
        }

        if (!isAdd) {
            ItemPathDto newItem = itemPathDto;
            ItemPathDto currentItem = itemPathDto.getParentPath();
            ItemPathDto parentPath = currentItem.getParentPath();
            ItemPathDto resultingItem;
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

        this.getModel().setObject(itemPathDto);

        target.add(this);
        onUpdate(itemPathDto);

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

    protected ItemPathSegmentPanel getItemPathSegmentPanel(){
        return (ItemPathSegmentPanel) get(ID_ITEM_PATH).get(ID_NAMESPACE_MODE_CONTAINER).get(ID_DEFINITION);
    }

    protected void switchButtonClickPerformed(AjaxRequestTarget target){
        if (ItemPathPanelMode.TEXT_MODE.equals(panelMode)){
            panelMode = ItemPathPanelMode.NAMESPACE_MODE;
        } else {
            panelMode = ItemPathPanelMode.TEXT_MODE;
        }
        target.add(ItemPathPanel.this);
    }

    public boolean isTextMode(){
        return ItemPathPanelMode.TEXT_MODE.equals(panelMode);
    }

    protected ItemPathPanelMode getPanelMode(){
        return panelMode;
    }
}
