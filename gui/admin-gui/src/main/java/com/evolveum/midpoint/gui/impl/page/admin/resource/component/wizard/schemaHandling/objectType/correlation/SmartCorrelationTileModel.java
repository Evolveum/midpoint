/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelatorCompositionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SmartCorrelationTileModel<T extends PrismContainerValueWrapper<ItemsSubCorrelatorType>> extends TemplateTile<T> {

    String icon;
    String name;
    String description;
    Double weight;
    Integer tier;
    String efficiency;
    Boolean enabled;
    String resourceOid;

    List<StateRecord> statesRecordList = new ArrayList<>();

    public record StateRecord(
            @Nullable Double value, @Nullable String label) implements Serializable {

        public Double getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public SmartCorrelationTileModel(T valueWrapper, String resourceOid, String efficiency) {
        super(valueWrapper);
        setValue(valueWrapper);

        ItemsSubCorrelatorType realValue = valueWrapper.getRealValue();
        this.name = realValue.getName();
        this.description = realValue.getDescription();
        this.enabled = realValue.getEnabled();

        CorrelatorCompositionDefinitionType composition = realValue.getComposition();
        this.weight = composition.getWeight();
        this.tier = composition.getTier();
        this.efficiency = efficiency;

        this.resourceOid = resourceOid;

        buildStateRecordList(efficiency);
    }

    private void buildStateRecordList(String efficiency) {
        if (weight != null) {
            statesRecordList.add(new StateRecord(weight, "Weight"));
        }
        if (tier != null) {
            statesRecordList.add(new StateRecord((double) tier, "Tier"));
        }
        if (efficiency != null) {
            statesRecordList.add(new StateRecord(null, "Efficiency: " + efficiency));
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

    public String getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(String efficiency) {
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

    public List<CorrelationItemType> getCorrelationItems() {
        return getValue().getRealValue().getItem();
    }
}
