/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.AbstractSpecificMappingTileTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class ActivationMappingTileTable extends AbstractSpecificMappingTileTable<ResourceActivationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationMappingTileTable.class);

    public ActivationMappingTileTable(
            String id,
            IModel<PrismContainerWrapper<ResourceActivationDefinitionType>> containerModel,
            @NotNull MappingDirection mappingDirection,
            ResourceDetailsModel detailsModel) {
        super(id, containerModel, mappingDirection, detailsModel);
    }

    @Override
    protected void onClickCreateMapping(AjaxRequestTarget target) {
        getPageBase().showMainPopup(
                createActivationPopup(),
                target);
    }

    private CreateActivationMappingPopup createActivationPopup() {
        return new CreateActivationMappingPopup(
                getPageBase().getMainPopupBodyId(),
                getMappingDirection(),
                PrismContainerValueWrapperModel.fromContainerWrapper(getContainerModel(), ItemPath.EMPTY_PATH),
                getDetailsModel()) {

            @Override
            protected <T extends PrismContainerValueWrapper<? extends Containerable>> void selectMapping(
                    IModel<T> valueModel,
                    MappingTile.MappingDefinitionType mappingDefinitionType,
                    AjaxRequestTarget target) {

                if (valueModel.getObject().getItems().size() == 1) {
                    @NotNull ItemName itemName = valueModel.getObject().getItems().iterator().next().getItemName();
                    if (itemName.equivalent(MappingType.F_LIFECYCLE_STATE)) {
                        try {
                            valueModel.getObject().findProperty(itemName).getValue().setRealValue(SchemaConstants.LIFECYCLE_ACTIVE);
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't find property for " + itemName);
                        }
                        refresh(target);
                        return;
                    }
                }

                boolean dontOpenConfiguration = true;
                for (ItemWrapper<?, ?> item : valueModel.getObject().getItems()) {
                    if (item.isMandatory()) {
                        dontOpenConfiguration = false;
                    }
                }

                if (dontOpenConfiguration) {
                    refresh(target);
                    return;
                }

                if (MappingTile.MappingDefinitionType.PREDEFINED.equals(mappingDefinitionType)) {
                    editPredefinedMapping(
                            (IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>>) valueModel,
                            target);
                } else {
                    editConfiguredMapping(
                            (IModel<PrismContainerValueWrapper<MappingType>>) valueModel,
                            target);
                }
            }
        };
    }

    @Override
    protected String getNoRuleMessageKey() {
        return "AbstractSpecificMappingTileTable.noActivationRules";
    }
}
