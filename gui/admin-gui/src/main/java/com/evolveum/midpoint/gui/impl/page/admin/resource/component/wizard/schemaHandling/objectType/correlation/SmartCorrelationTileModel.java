/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedCorrelationItemWrapper;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.extractCorrelationItemListWrapper;

public class SmartCorrelationTileModel<T extends PrismContainerValueWrapper<ItemsSubCorrelatorType>> extends TemplateTile<T> {

    String icon;
    String name;
    String description;
    Double weight;
    Integer tier;
    Double efficiency;
    Boolean enabled;

    String resourceOid;
    String statusInfoToken;

    List<StateRecord> statesRecordList = new ArrayList<>();

    public record StateRecord(
            @Nullable String value, @Nullable String label) implements Serializable {

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public SmartCorrelationTileModel(T valueWrapper, String resourceOid, String statusInfoToken) {
        super(valueWrapper);
        setValue(valueWrapper);

        ItemsSubCorrelatorType realValue = valueWrapper.getRealValue();
        this.name = realValue.getName() != null ? realValue.getName() : "-";
        this.description = realValue.getDescription() != null ? realValue.getDescription() : "-";
        this.enabled = realValue.getEnabled() != null ? realValue.getEnabled() : Boolean.FALSE;

        CorrelatorCompositionDefinitionType composition = realValue.getComposition();
        this.weight = composition.getWeight();
        this.tier = composition.getTier();

        this.resourceOid = resourceOid;
        this.statusInfoToken = statusInfoToken;

        this.efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(valueWrapper);

        buildStateRecordList();
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

    private void buildStateRecordList() {
        String weightLabel = (weight != null) ? weight.toString() : "-";
        String tierLabel = (tier != null) ? tier.toString() : "-";
        statesRecordList.add(new StateRecord(weightLabel, "Weight"));
        statesRecordList.add(new StateRecord(tierLabel, "Tier"));

        if (this.efficiency != null) {
            statesRecordList.add(new StateRecord(String.format("%.2f%%", efficiency), "Efficiency"));
        }
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(Double efficiency) {
        this.efficiency = efficiency;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public List<StateRecord> getStatesRecordList() {
        return statesRecordList;
    }

    public List<PrismContainerValueWrapper<CorrelationItemType>> getCorrelationItems() {
        T realValue = getValue();
        return extractCorrelationItemListWrapper(realValue);
    }
}
