/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractSpecificMappingTileTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class CredentialsMappingTileTable extends AbstractSpecificMappingTileTable<ResourceCredentialsDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsMappingTileTable.class);

    private final IModel<PrismContainerValueWrapper<ResourcePasswordDefinitionType>> parentValue;

    public CredentialsMappingTileTable(
            String id,
            IModel<PrismContainerWrapper<ResourceCredentialsDefinitionType>> containerModel,
            @NotNull MappingDirection mappingDirection,
            ResourceDetailsModel detailsModel) {
        super(id, containerModel, mappingDirection, detailsModel);
        this.parentValue = new ContainerValueWrapperFromObjectWrapperModel<>(getContainerModel(), ResourceCredentialsDefinitionType.F_PASSWORD);
    }

    @Override
    protected void onClickCreateMapping(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ResourcePasswordDefinitionType> parent = parentValue.getObject();
        try {
            PrismContainerWrapper<Containerable> childContainer = parent.findContainer(getMappingDirection().getContainerName());
            PrismContainerValue<Containerable> newValue = childContainer.getItem().createNewValue();

            MappingType mapping = (MappingType) newValue.asContainerable();
            mapping.beginExpression();
            ExpressionUtil.addAsIsExpressionValue(mapping.getExpression());

            PrismContainerValueWrapper<Containerable> valueWrapper = WebPrismUtil.createNewValueWrapper(
                    childContainer, newValue, getPageBase(), getDetailsModel().createWrapperContext());
            childContainer.getValues().add(valueWrapper);
            refresh(target);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't find child container by path " + getMappingDirection().getContainerName() + " in parent " + parent);
        }
    }
}
