/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.ValidatorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lskublik
 */
public abstract class SynchronizationReactionTable extends MultivalueContainerListPanel<SynchronizationReactionType> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationReactionTable.class);

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public SynchronizationReactionTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, SynchronizationReactionType.class);
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
        createNewReaction(target);
        refreshTable(target);
    }

    protected PrismContainerValueWrapper createNewReaction(AjaxRequestTarget target) {
        PrismContainerWrapper<SynchronizationReactionType> container = getContainerModel().getObject();
        PrismContainerValue<SynchronizationReactionType> newReaction = container.getItem().createNewValue();
        PrismContainerValueWrapper<SynchronizationReactionType> newReactionWrapper =
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

        item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
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
        items.add(item);
        return items;
    }

    @Override
    protected IModel<PrismContainerWrapper<SynchronizationReactionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                valueModel,
                ItemPath.create(ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION, SynchronizationReactionsType.F_REACTION));
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<SynchronizationReactionType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<SynchronizationReactionType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<SynchronizationReactionType>> reactionDef = getSynchReactionDefinition();
        columns.add(new PrismPropertyWrapperColumn<SynchronizationReactionType, String>(
                reactionDef,
                SynchronizationReactionType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<SynchronizationReactionType, String>(
                reactionDef,
                SynchronizationReactionType.F_SITUATION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismContainerWrapperColumn<>(
                reactionDef,
                SynchronizationReactionType.F_ACTIONS,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(
                    String componentId, IModel<IW> rowModel) {
                ContainersDropDownPanel<SynchronizationActionsType> panel = new ContainersDropDownPanel(
                        componentId,
                        rowModel) {
                    @Override
                    protected boolean validateChildContainer(ItemDefinition definition) {
                        return AbstractSynchronizationActionType.class.isAssignableFrom(definition.getTypeClass());
                    }
                };
                panel.setOutputMarkupId(true);
                return panel;
            }
        });

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<SynchronizationReactionType>> getSynchReactionDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<SynchronizationReactionType> load() {
                return valueModel.getObject().getDefinition().findContainerDefinition(ItemPath.create(ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION, SynchronizationReactionsType.F_REACTION));
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_SYNCHRONIZATION_REACTION_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "SynchronizationReactionTable.newObject";
    }

    public boolean isValidFormComponents(AjaxRequestTarget target) {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);
        getTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (row, object) -> {
            ((SelectableDataTable.SelectableRowItem) row).visitChildren(FormComponent.class, (baseFormComponent, object2) -> {
                baseFormComponent.getBehaviors().stream()
                        .filter(behaviour -> behaviour instanceof ValidatorAdapter
                                && ((ValidatorAdapter)behaviour).getValidator() instanceof NotNullValidator)
                        .map(adapter -> ((ValidatorAdapter)adapter).getValidator())
                        .forEach(validator -> ((NotNullValidator)validator).setUseModel(true));
                ((FormComponent)baseFormComponent).validate();
                if (baseFormComponent.hasErrorMessage()) {
                    valid.set(false);
                    if (target != null) {
                        target.add(baseFormComponent);
                        InputPanel inputParent = baseFormComponent.findParent(InputPanel.class);
                        if (inputParent != null && inputParent.getParent() != null) {
                            target.addChildren(inputParent.getParent(), FeedbackLabels.class);
                        }
                    }
                }
            });
        });
        return valid.get();
    }
}
