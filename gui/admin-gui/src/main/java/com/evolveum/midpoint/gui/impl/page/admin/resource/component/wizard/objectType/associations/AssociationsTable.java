/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardTable;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@Experimental
public abstract class AssociationsTable extends AbstractResourceWizardTable<ResourceObjectAssociationType, ResourceObjectTypeDefinitionType> {

    public AssociationsTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, valueModel, ResourceObjectAssociationType.class);
    }

    @Override
    protected IModel<PrismContainerWrapper<ResourceObjectAssociationType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
                ResourceObjectTypeDefinitionType.F_ASSOCIATION);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ResourceObjectAssociationType>> associationsDef = getAssociationsDefinition();
        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_DISPLAY_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_KIND,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_INTENT,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_DIRECTION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_VALUE_ATTRIBUTE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ResourceObjectAssociationType>> getAssociationsDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceObjectAssociationType> load() {
                return getValueModel().getObject().getDefinition().findContainerDefinition(
                        ResourceObjectTypeDefinitionType.F_ASSOCIATION);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "AssociationsTable.newObject";
    }
}
