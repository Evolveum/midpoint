/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.dto;

import java.io.Serializable;
import java.util.List;

public class CorrelatorConfigDto implements Serializable {

    public static final String CORRELATOR_IDENTIFIER = "correlatorIdentifier";
    public static final String CORRELATOR_INDEX = "correlatorIndex";

    private String correlatorIdentifier;
    private List<VerificationAttributeDto> attributeDtoList;
    private String archetypeOid;
    private int correlatorIndex;

    public CorrelatorConfigDto(String correlatorIdentifier, String archetypeOid, List<VerificationAttributeDto> attributeDtoList,
            int correlatorIndex) {
        this.correlatorIdentifier = correlatorIdentifier;
        this.archetypeOid = archetypeOid;
        this.attributeDtoList = attributeDtoList;
        this.correlatorIndex = correlatorIndex;
    }

    public List<VerificationAttributeDto> getAttributeDtoList() {
        return attributeDtoList;
    }
}
