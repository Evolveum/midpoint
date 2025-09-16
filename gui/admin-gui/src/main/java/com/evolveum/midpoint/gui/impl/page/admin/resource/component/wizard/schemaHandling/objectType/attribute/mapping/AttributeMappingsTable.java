/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createNewVirtualMappingValue;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createVirtualMappingContainerModel;

/**
 * @author lskublik
 */
public abstract class AttributeMappingsTable<P extends Containerable, AP extends Containerable> extends AbstractMappingsTable<P> {

    public AttributeMappingsTable(
            String id,
            IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    @Override
    protected final PrismContainerValueWrapper<MappingType> createNewValue(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        return createNewVirtualMappingValue(
                value,
                getValueModel(),
                getMappingType(),
                getItemNameOfContainerWithMappings(),
                getItemNameOfRefAttribute(),
                getPageBase(),
                target);
    }


    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<MappingType>> createProvider() {
        return super.createProvider();
    }

    protected abstract MappingDirection getMappingType();

    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        toDelete.forEach(value -> {
            PrismContainerValueWrapper parentValue = value.getParent().getParent();
            if (parentValue.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper wrapper = (PrismContainerWrapper) parentValue.getParent();
                if (wrapper != null) {
                    wrapper.getValues().remove(parentValue);
                }
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
            value.setSelected(false);
        });
        refreshTable(target);
    }

    @Override
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return createVirtualMappingContainerModel(
                getPageBase(),
                getValueModel(),
                getItemNameOfContainerWithMappings(),
                getItemNameOfRefAttribute(),
                getMappingType());
    }

    protected abstract ItemName getItemNameOfRefAttribute();

    protected abstract ItemName getItemNameOfContainerWithMappings();


    protected abstract ItemPathType getAttributeRefAttributeValue(PrismContainerValueWrapper<AP> value);

    protected IColumn<PrismContainerValueWrapper<MappingType>, String> createVirtualRefItemColumn(
            IModel<? extends PrismContainerDefinition> resourceAttributeDef, String cssClasses) {
        return new PrismPropertyWrapperColumn(
                resourceAttributeDef,
                getItemNameOfRefAttribute(),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            protected Component createHeader(String componentId, IModel mainModel) {
                return new PrismPropertyHeaderPanel<ItemPathType>(
                        componentId,
                        new PrismPropertyWrapperHeaderModel(mainModel, itemName, getPageBase())) {

                    @Override
                    protected boolean isAddButtonVisible() {
                        return false;
                    }

                    @Override
                    protected boolean isButtonEnabled() {
                        return false;
                    }

                    @Override
                    protected Component createTitle(IModel<String> label) {
                        return super.createTitle(getPageBase().createStringResource(
                                getRefColumnPrefix() + getMappingType().name() + "." + getItemNameOfRefAttribute()));
                    }
                };
            }

            @Override
            public String getCssClass() {
                return cssClasses;
            }
        };
    }

    protected String getRefColumnPrefix() {
        return "";
    }
}
