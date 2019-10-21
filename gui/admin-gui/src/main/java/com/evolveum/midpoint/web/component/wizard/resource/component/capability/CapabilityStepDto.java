/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 */
public class CapabilityStepDto implements Serializable {

    public static final String F_CAPABILITIES = "capabilities";

    private CapabilityDto selectedDto;

    private List<CapabilityDto<CapabilityType>> capabilities = new ArrayList<>();

    public CapabilityStepDto(List<CapabilityDto<CapabilityType>> capabilities) {
        this.capabilities = capabilities;
    }

    public List<CapabilityDto<CapabilityType>> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<CapabilityDto<CapabilityType>> capabilities) {
        this.capabilities = capabilities;
    }

    public CapabilityDto getSelectedDto() {
        return selectedDto;
    }

    public void setSelected(CapabilityDto selected) {
        this.selectedDto = selected;
    }

}
