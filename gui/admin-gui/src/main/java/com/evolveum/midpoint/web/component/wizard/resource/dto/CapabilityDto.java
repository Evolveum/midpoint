/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CapabilityDto<T extends CapabilityType> implements Serializable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_SELECTED = "selected";

    private String displayName;
    private String resourceKey;
    private T capability;
    private boolean selected;                                // used only when adding capabilities (multi-select dialog)
    private boolean amongNativeCapabilities;

    public CapabilityDto(T capability, boolean amongNativeCapabilities) {
        this.capability = capability;
        this.displayName = Capability.getDisplayNameForClass(capability.getClass());
        this.resourceKey = Capability.getResourceKeyForClass(capability.getClass());
        this.amongNativeCapabilities = amongNativeCapabilities;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getValue() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public T getCapability() {
        return capability;
    }

    public void setCapability(T capability) {
        this.capability = capability;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public String getTooltipKey() {
        return resourceKey != null ? "CapabilityStep.capability."+resourceKey+".tooltip" : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilityDto)) return false;

        CapabilityDto that = (CapabilityDto) o;

        if (selected != that.selected) return false;
        if (capability != null ? !capability.equals(that.capability) : that.capability != null) return false;
        if (resourceKey != null ? !resourceKey.equals(that.resourceKey) : that.resourceKey != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (selected ? 1 : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (resourceKey != null ? resourceKey.hashCode() : 0);
        result = 31 * result + (capability != null ? capability.hashCode() : 0);
        return result;
    }

    public boolean isAmongNativeCapabilities() {
        return amongNativeCapabilities;
    }

    public void setAmongNativeCapabilities(boolean amongNativeCapabilities) {
        this.amongNativeCapabilities = amongNativeCapabilities;
    }
}
