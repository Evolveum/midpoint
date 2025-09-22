/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.mapping;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedMappingItemWrapper;

public class SmartMappingTileModel<T extends PrismContainerValueWrapper<MappingType>> extends TemplateTile<T> {

    String icon;
    @Nullable Float efficiency;

    String resourceOid;
    String statusInfoToken;

    public SmartMappingTileModel(T valueWrapper, String resourceOid, String statusInfoToken) {
        super(valueWrapper);
        setValue(valueWrapper);

        this.resourceOid = resourceOid;
        this.statusInfoToken = statusInfoToken;


        this.efficiency = extractEfficiencyFromSuggestedMappingItemWrapper(valueWrapper);

    }

    protected StatusInfo<CorrelationSuggestionsType> getStatusInfo(@NotNull PageBase pageBase, Task task, OperationResult result) {
        SmartIntegrationService smartService = pageBase.getSmartIntegrationService();
        if (statusInfoToken != null) {
            try {
                return smartService.getSuggestCorrelationOperationStatus(statusInfoToken, task, result);
            } catch (Throwable e) {
                pageBase.error("Couldn't get correlation suggestion statusInfo: " + e.getMessage());
            }
        }
        return null;
    }


    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public @Nullable Float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(@Nullable Float efficiency) {
        this.efficiency = efficiency;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

}
