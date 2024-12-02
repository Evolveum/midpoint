/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author lskublik
 */
public abstract class OutboundAttributeMappingsTable<P extends Containerable> extends AttributeMappingsTable<P, ResourceAttributeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundAttributeMappingsTable.class);

    public OutboundAttributeMappingsTable(
            String id, IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_OUTBOUND_MAPPING_WIZARD;
    }

    @Override
    protected MappingDirection getMappingType() {
        return MappingDirection.OUTBOUND;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "OutboundAttributeMappingsTable.newObject";
    }

    @Override
    protected Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns() {

        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        columns.add(createSourceColumn(mappingTypeDef));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-minus text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        columns.add(new PrismPropertyWrapperColumn(
                mappingTypeDef,
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-arrow-right-long text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        ResourceAttributeDefinitionType.class));

        columns.add(createVirtualRefItemColumn(resourceAttributeDef, "col-xl-2 col-lg-2 col-md-3"));

        return columns;
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createSourceColumn(IModel<PrismContainerDefinition<MappingType>> mappingTypeDef) {
        return new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_SOURCE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {

                IModel<Collection<VariableBindingDefinitionType>> multiselectModel = createSourceMultiselectModel(rowModel);

                FocusDefinitionsMappingProvider provider;
                if (isMappingForAssociation()) {
                    provider = new FocusDefinitionsMappingProvider((IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel) {
                        @Override
                        protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                            return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
                        }
                    };
                } else {
                    provider = new FocusDefinitionsMappingProvider((IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel);
                }
                return new Select2MultiChoiceColumnPanel<>(componentId, multiselectModel, provider);
            }

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-2";
            }
        };
    }

    private boolean isMappingForAssociation() {
        var associationParent = getValueModel().getObject().getParentContainerValue(ShadowAssociationDefinitionType.class);
        return associationParent != null;
    }

    @Override
    protected ItemName getItemNameOfRefAttribute() {
        return ResourceAttributeDefinitionType.F_REF;
    }

    @Override
    protected ItemPathType getAttributeRefAttributeValue(PrismContainerValueWrapper<ResourceAttributeDefinitionType> value) {
        return value.getRealValue().getRef();
    }
}
