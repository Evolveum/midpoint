/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.gui.impl.component.input.SourceMappingProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.MappingOverrideTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.model.Model;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.StringTextChoiceProvider;

/**
 * @author lskublik
 */
public abstract class CorrelationItemsTable extends MultivalueContainerListPanel<ItemsSubCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemsTable.class);

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public CorrelationItemsTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, ItemsSubCorrelatorType.class);
        this.valueModel = valueModel;
    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = super.createToolbarButtonsList(idButton);
        buttons.forEach(button -> {
            if (button instanceof AjaxIconButton) {
                ((AjaxIconButton) button).showTitleAsLabel(true);
            }
        });
        return buttons;
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        createNewItems(target);
        refreshTable(target);
    }

    private PrismContainerValueWrapper createNewItems(AjaxRequestTarget target) {
        PrismContainerWrapper<ItemsSubCorrelatorType> container = getContainerModel().getObject();
        PrismContainerValue<ItemsSubCorrelatorType> newReaction = container.getItem().createNewValue();
        PrismContainerValueWrapper<ItemsSubCorrelatorType> newReactionWrapper =
                createNewItemContainerValueWrapper(newReaction, container, target);
        return newReactionWrapper;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        };
        items.add(item);

        items.add(new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        });
        return items;
    }

    @Override
    protected IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                valueModel,
                ItemPath.create(
                        ResourceObjectTypeDefinitionType.F_CORRELATION,
                        CorrelationDefinitionType.F_CORRELATORS,
                        CompositeCorrelatorType.F_ITEMS));
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ItemsSubCorrelatorType>> reactionDef = getCorrelationItemsDefinition();
        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()){
            @Override
            public String getCssClass() {
                return "col-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_DESCRIPTION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                ItemsSubCorrelatorType realValue = (ItemsSubCorrelatorType) rowModel.getObject().getParent().getRealValue();
                StringBuilder items = new StringBuilder();
                String prefix = "";
                for (CorrelationItemType item : realValue.getItem()){
                    items.append(prefix).append(item.getRef().toString());
                    prefix = ", ";
                }

                PrismPropertyWrapperColumnPanel panel = new PrismPropertyWrapperColumnPanel<>(
                        componentId, (IModel<PrismPropertyWrapper<String>>) rowModel, getColumnType()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        visitChildren(FormComponent.class, (formComponent, object) -> {
                            formComponent.add(AttributeAppender.append(
                                    "placeholder",
                                    () -> {
                                        if (items.length() == 0) {
                                            return "";
                                        } else {
                                            return getPageBase().createStringResource(
                                                    "CorrelationItemsTable.column.description.placeholder",
                                                    items.toString()).getString();
                                        }
                                    }));
                        });
                    }

                };
                return panel;
            }


            @Override
            public String getCssClass() {
                return "col-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_WEIGHT),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_TIER),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_IGNORE_IF_MATCHED_BY),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()){
            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                IModel<Collection<String>> multiselectModel = new IModel<>() {

                    @Override
                    public Collection<String> getObject() {

                        return ((PrismPropertyWrapper<String>) rowModel.getObject())
                                .getValues().stream()
                                .filter(value -> !ValueStatus.DELETED.equals(value.getStatus()) && value.getRealValue() != null)
                                .map(value -> value.getRealValue())
                                .collect(Collectors.toList());
                    }

                    @Override
                    public void setObject(Collection<String> newValues) {

                        PrismPropertyWrapper<String> ignoreIfMatchedByItem =
                                ((PrismPropertyWrapper<String>) rowModel.getObject());
                        List<PrismPropertyValueWrapper<String>> toRemoveValues
                                = new ArrayList<>(
                                ignoreIfMatchedByItem.getValues().stream()
                                        .filter(v -> v.getRealValue() != null)
                                        .collect(Collectors.toList()));

                        newValues.forEach(newValue -> {
                            if (StringUtils.isEmpty(newValue)) {
                                return;
                            }
                            Optional<PrismPropertyValueWrapper<String>> found = ignoreIfMatchedByItem.getValues().stream()
                                    .filter(actualValue -> StringUtils.isNotEmpty(actualValue.getRealValue())
                                            && newValue.equals(actualValue.getRealValue()))
                                    .findFirst();
                            if (found.isPresent()) {
                                toRemoveValues.remove(found.get());
                                if (ValueStatus.DELETED.equals(found.get().getStatus())) {
                                    found.get().setStatus(ValueStatus.NOT_CHANGED);
                                }
                            } else {
                                try {
                                    PrismPropertyValue<String> newPrismValue
                                            = getPrismContext().itemFactory().createPropertyValue();
                                    newPrismValue.setValue(newValue);
                                    ignoreIfMatchedByItem.add(newPrismValue, getPageBase());
                                } catch (SchemaException e) {
                                    LOGGER.error("Couldn't initialize new value for Source item", e);
                                }
                            }
                        });

                        toRemoveValues.forEach(toRemoveValue -> {
                            try {
                                ignoreIfMatchedByItem.remove(toRemoveValue, getPageBase());
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't remove old value for Source item", e);
                            }
                        });

                    }
                };

                return new Select2MultiChoicePanel<>(componentId, multiselectModel, new StringTextChoiceProvider() {
                    @Override
                    public void query(String input, int i, Response<String> response) {
                        response.addAll(getContainerModel().getObject().getValues().stream()
                                .map(value -> value.getRealValue().getName())
                                .filter(name -> StringUtils.isNotEmpty(name))
                                .filter(name -> StringUtils.isEmpty(input) || name.startsWith(input))
                                .collect(Collectors.toList()));
                    }
                });
            }

            @Override
            public String getCssClass() {
                return "col-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ItemsSubCorrelatorType>> getCorrelationItemsDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ItemsSubCorrelatorType> load() {
                return valueModel.getObject().getDefinition().findContainerDefinition(
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_CORRELATION,
                                CorrelationDefinitionType.F_CORRELATORS,
                                CompositeCorrelatorType.F_ITEMS));
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "CorrelationItemsTable.newObject";
    }
}
