/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Table of ShadowItemDependencyType for volatility of attribute. Table joins incoming and outgoing dependencies.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 *
 * @author lskublik
 */
public class AttributeVolatilityTable extends AbstractWizardTable<ShadowItemDependencyType, ShadowItemVolatilityType> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeVolatilityTable.class);


    public AttributeVolatilityTable(
            String id,
            IModel<PrismContainerValueWrapper<ShadowItemVolatilityType>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, ShadowItemDependencyType.class);
    }

    private PrismContainerValueWrapper<ShadowItemDependencyType> createNewValue(PrismContainerValue<ShadowItemDependencyType> value, ItemName path, AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ShadowItemDependencyType> dependencyContainer = getValueModel().getObject().findContainer(path);
            PrismContainerValue<ShadowItemDependencyType> dependency = value;

            if (dependency == null) {
                dependency = dependencyContainer.getItem().createNewValue();
            }

            return WebPrismUtil.createNewValueWrapper(dependencyContainer, dependency, getPageBase(), target);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new dependency for volatility");
        }
        return null;
    }

    @Override
    protected void newItemPerformed(PrismContainerValue<ShadowItemDependencyType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
        if(isDuplicate) {
            if (value != null) {
                @NotNull ItemName path = value.getDefinition().getItemName();
                createNewValue(value, path, target);
                refreshTable(target);
            }
        }

        VolatilityCreationPopup popup = new VolatilityCreationPopup(getPageBase().getMainPopupBodyId()){
            @Override
            protected void onVolatilityPerformed(ItemName path, AjaxRequestTarget target) {
                createNewValue(value, path, target);
                refreshTable(target);
            }
        };
        getPageBase().showMainPopup(popup, target);
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ShadowItemDependencyType>> createProvider() {
        return new MultivalueContainerListDataProvider<>(AttributeVolatilityTable.this, getSearchModel(), new PropertyModel<>(getContainerModel(), "values")) {

            @Override
            protected List<PrismContainerValueWrapper<ShadowItemDependencyType>> searchThroughList() {
                List<PrismContainerValueWrapper<ShadowItemDependencyType>> list = new ArrayList<>();
                try {
                    PrismContainerWrapper<ShadowItemDependencyType> incoming = getValueModel().getObject().findContainer(ShadowItemVolatilityType.F_INCOMING);
                    list.addAll(incoming.getValues());

                    PrismContainerWrapper<ShadowItemDependencyType> outgoing = getValueModel().getObject().findContainer(ShadowItemVolatilityType.F_OUTGOING);
                    list.addAll(outgoing.getValues());
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find container for ShadowItemDependencyType", e);
                }
                return list;
            }
        };
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ShadowItemDependencyType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ShadowItemDependencyType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ShadowItemDependencyType>> attributeDef = getShadowItemDependencyDefinition();

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("AttributeVolatilityTable.column.kindOfVolatility")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ShadowItemDependencyType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ShadowItemDependencyType>> iModel) {
                IModel<String> displayName = () -> iModel.getObject().getParent().getDisplayName();
                item.add(new Label(id, displayName));
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ShadowItemDependencyType, String>(
                attributeDef,
                ShadowItemDependencyType.F_OPERATION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ShadowItemDependencyType>> getShadowItemDependencyDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ShadowItemDependencyType> load() {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ShadowItemDependencyType.class);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_ATTRIBUTE_VOLATILITY_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "AttributeVolatilityTable.newObject";
    }

    @Override
    protected IModel<PrismContainerWrapper<ShadowItemDependencyType>> getContainerModel() {
        return Model.of();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createDeleteItemMenu());
        return items;
    }
}
