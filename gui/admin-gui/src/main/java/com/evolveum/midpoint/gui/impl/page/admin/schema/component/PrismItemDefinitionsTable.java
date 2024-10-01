/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrismItemDefinitionsTable extends AbstractWizardTable<PrismItemDefinitionType, ComplexTypeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismItemDefinitionsTable.class);

    private static final String COLUMN_CSS = "mp-w-sm-2 mp-w-md-1 text-nowrap";

    private enum Type {
        PROPERTY,
        REFERENCE,
        CONTAINER
    }

    public PrismItemDefinitionsTable(
            String id, IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, PrismItemDefinitionType.class);
    }

    @Override
    protected IModel<PrismContainerWrapper<PrismItemDefinitionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
                ComplexTypeDefinitionType.F_ITEM_DEFINITIONS);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<PrismItemDefinitionType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<PrismItemDefinitionType>, String>> createDefaultColumns() {
        LoadableDetachableModel<PrismContainerDefinition<PrismItemDefinitionType>> defModel = new LoadableDetachableModel() {
            @Override
            protected PrismContainerDefinition<PrismItemDefinitionType> load() {
                PrismContainerDefinition<PrismSchemaType> schemaDef =
                        PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(PrismSchemaType.class);
                return schemaDef.findContainerDefinition(
                        ItemPath.create(PrismSchemaType.F_COMPLEX_TYPE, ComplexTypeDefinitionType.F_ITEM_DEFINITIONS));
            }
        };

        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {

                    @Override
                    public String getCssClass() {
                        return "text-nowrap";
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_TYPE,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
                    @Override
                    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                        return new PrismPropertyWrapperColumnPanel<>(componentId, (IModel<PrismPropertyWrapper<QName>>) rowModel, getColumnType()) {

                            @Override
                            protected void onBeforeRender() {
                                super.onBeforeRender();
                                visitChildren(FormComponent.class, (formComponent, object) -> {
                                    formComponent.add(new AjaxFormComponentUpdatingBehavior("change") {
                                        @Override
                                        protected void onUpdate(AjaxRequestTarget target) {
                                            PrismPropertyWrapper<QName> object = (PrismPropertyWrapper<QName>) rowModel.getObject();
                                            PrismPropertyValueWrapper<QName> objectValue = null;
                                            try {
                                                objectValue = object.getValue();
                                            } catch (SchemaException e) {
                                                LOGGER.debug("Couldn't get value for " + object);
                                            }
                                            if (objectValue == null) {
                                                return;
                                            }

                                            PrismContainerValueWrapper<PrismItemDefinitionType> containerValue = object.getParentContainerValue(PrismItemDefinitionType.class);
                                            if (containerValue == null) {
                                                LOGGER.debug("Couldn't find parent value for PrismItemDefinitionType in " + object);
                                                return;
                                            }
                                            if (ValueStatus.ADDED != containerValue.getStatus()) {
                                                return;
                                            }

                                            Type type = defineTypeOfProperty(objectValue, containerValue);

                                            PrismContainerValue<PrismItemDefinitionType> newValue = null;

                                            switch (type) {
                                                case PROPERTY ->
                                                        newValue = new PrismPropertyDefinitionType().asPrismContainerValue();
                                                case REFERENCE ->
                                                        newValue = new PrismReferenceDefinitionType().asPrismContainerValue();
                                                case CONTAINER ->
                                                        newValue = new PrismContainerDefinitionType().asPrismContainerValue();
                                            }

                                            try {
                                                containerValue.getParent().getItem().remove(containerValue.getNewValue());
                                                containerValue.getParent().getItem().add(newValue);

                                                for (Item<?, ?> item : containerValue.getNewValue().getItems()) {
                                                    ItemDefinition<?> def = newValue.getDefinition().findItemDefinition(item.getPath().namedSegmentsOnly());
                                                    if (def != null) {
                                                        newValue.add(item.clone());
                                                    }
                                                }
                                            } catch (SchemaException e) {
                                                LOGGER.debug("Couldn't add old items from containerValue value to new value");
                                            }
                                            try {
                                                PrismContainerValueWrapper newPrismValue =
                                                        WebPrismUtil.createNewValueWrapper(
                                                                containerValue.getParent(), newValue, getPageBase());
                                                containerValue.clearItems();
                                                containerValue.addItems(newPrismValue.getItems());
                                            } catch (SchemaException e) {
                                                LOGGER.debug("Couldn't create new prism value wrapper for " + newValue);
                                            }
                                            containerValue.replaceContainerItemValue(newValue);
                                            target.add(getTableComponent());
                                        }
                                    });

                                });
                            }
                        };
                    }

                    @Override
                    public String getCssClass() {
                        return "text-nowrap";
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_DISPLAY_NAME,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return "text-nowrap";
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_DISPLAY_ORDER,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
                    @Override
                    public String getCssClass() {
                        return "mp-w-sm-2 mp-w-md-1 text-nowrap";
                    }

                    @Override
                    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<PrismItemDefinitionType>> mainModel) {
                        return new PrismPropertyHeaderPanel<Integer>(
                                componentId,
                                new PrismPropertyWrapperHeaderModel(mainModel, itemName, PrismItemDefinitionsTable.this.getPageBase())) {

                            @Override
                            protected boolean isAddButtonVisible() {
                                return false;
                            }

                            @Override
                            protected boolean isButtonEnabled() {
                                return false;
                            }

                            @Override
                            protected boolean isHelpTextVisible() {
                                return true;
                            }

                            @Override
                            public IModel<String> createLabelModel() {
                                return Model.of(LocalizationUtil.translate("PrismItemDefinitionsTable.order"));
                            }
                        };
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_REQUIRED,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return COLUMN_CSS;
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_MULTIVALUE,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return COLUMN_CSS;
                    }
                },
                new PrismPropertyWrapperColumn<>(defModel, PrismItemDefinitionType.F_INDEXED,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()){
                    @Override
                    public String getCssClass() {
                        return COLUMN_CSS;
                    }
                }
        );
    }

    private Type defineTypeOfProperty(PrismPropertyValueWrapper<QName> objectValue, PrismContainerValueWrapper<PrismItemDefinitionType> containerValue) {
        Type type = Type.PROPERTY;
        Class<Object> javaClass = XsdTypeMapper.getXsdToJavaMapping(objectValue.getRealValue());
        if (javaClass != null) {
            type = Type.PROPERTY;
        } else if (QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, objectValue.getRealValue())) {
            type = Type.REFERENCE;
        } else {
            PrismSchema schema = PrismContext.get().getSchemaRegistry().getPrismSchema(SchemaConstants.NS_C);
            TypeDefinition def = schema.findTypeDefinitionByType(objectValue.getRealValue());
            if (def instanceof EnumerationTypeDefinition) {
                type = Type.PROPERTY;
            } else if (def instanceof ComplexTypeDefinition complexTypeDefinition) {
                if (complexTypeDefinition.isContainerMarker()) {
                    type = Type.CONTAINER;
                } else {
                    type = Type.PROPERTY;
                }
            }
        }

        PrismContainerValueWrapper<PrismSchemaType> schema =
                containerValue.getParentContainerValue(PrismSchemaType.class);
        if (schema != null && schema.getRealValue() != null) {
            boolean match = schema.getRealValue().getComplexType().stream()
                    .filter(complexType -> complexType.getExtension() == null)
                    .anyMatch(complexType -> QNameUtil.match(complexType.getName(), objectValue.getRealValue()));
            if (match) {
                type = Type.CONTAINER;
            } else {
                match = schema.getRealValue().getEnumerationType().stream()
                        .anyMatch(enumType -> QNameUtil.match(enumType.getName(), objectValue.getRealValue()));
                if (match) {
                    type = Type.PROPERTY;
                }
            }
        }
        return type;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createDeleteInlineMenu());
        menuItems.add(createEditInlineMenu());
        return menuItems;
    }

    @Override
    protected ButtonInlineMenuItem createDeleteInlineMenu() {
        ButtonInlineMenuItem menu = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
        menu.setVisibilityChecker(
                (InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
                    if (isHeader){
                        return false;
                    }
                    PrismContainerValueWrapper<PrismItemDefinitionType> rowObject =
                            (PrismContainerValueWrapper<PrismItemDefinitionType>) rowModel.getObject();
                    return ValueStatus.ADDED.equals(rowObject.getStatus());
                });
        return menu;
    }

    @Override
    protected boolean allowEditMultipleValuesAtOnce() {
        return false;
    }

    @Override
    public void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> rowModel,
            List<PrismContainerValueWrapper<PrismItemDefinitionType>> listItems) {

        if (isValidFormComponentsOfRow(rowModel, target)) {

            PageAssignmentHolderDetails parent = findParent(PageAssignmentHolderDetails.class);
            if (parent == null) {
                warn("Couldn't create popup for new item");
                return;
            }

            if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> valueModel;
                if (rowModel == null) {
                    valueModel = () -> listItems.iterator().next();
                } else {
                    valueModel = rowModel;
                }
                if (valueModel != null) {
                    OnePanelPopupPanel popup = new OnePanelPopupPanel(
                            getPageBase().getMainPopupBodyId(),
                            createStringResource("PrismItemDefinitionsTable.modifyProperty")) {
                        @Override
                        protected WebMarkupContainer createPanel(String id) {
                            return new BasicPrimItemDefinitionPanel(
                                    (AssignmentHolderDetailsModel<SchemaType>) parent.getObjectDetailsModels(), valueModel) {
                                @Override
                                public String getId() {
                                    return id;
                                }
                            };
                        }

                        @Override
                        protected void processHide(AjaxRequestTarget target) {
                            WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                            target.add(getTableComponent());
                            super.processHide(target);
                        }
                    };
                    popup.setOutputMarkupId(true);
                    getPageBase().showMainPopup(popup, target);
                }
            } else {
                warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                target.add(getPageBase().getFeedbackPanel());
            }
        }
    }

    protected String getKeyOfTitleForNewObjectButton() {
        return "PrismItemDefinitionsTable.newObject";
    }

    protected String getNewButtonCssClass() {
        return "btn btn-primary btn-sm";
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }

    @Override
    protected boolean isHeaderVisible() {
        return true;
    }
}
