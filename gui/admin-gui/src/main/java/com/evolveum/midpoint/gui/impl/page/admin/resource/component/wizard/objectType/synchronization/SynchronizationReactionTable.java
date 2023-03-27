/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.column.ToggleSimulationModeColumn;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardTable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public abstract class SynchronizationReactionTable extends AbstractResourceWizardTable<SynchronizationReactionType, ResourceObjectTypeDefinitionType> {

    public SynchronizationReactionTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, SynchronizationReactionType.class);
    }

    @Override
    protected IModel<PrismContainerWrapper<SynchronizationReactionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
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

        columns.add(new ToggleSimulationModeColumn<>(getContainerModel(), getPageBase()));

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<SynchronizationReactionType>> getSynchReactionDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<SynchronizationReactionType> load() {
                return getValueModel().getObject().getDefinition().findContainerDefinition(ItemPath.create(ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION, SynchronizationReactionsType.F_REACTION));
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
}
