/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;

/**
 * @author skublik
 */

public class StringLimitationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer minOccurs;
    private Integer maxOccurs;
    private PolyStringType name;
    private PolyStringType help;
    private Boolean mustBeFirst;
    private Boolean isSuccess;

    public Integer getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(Integer minOccurs) {
        this.minOccurs = minOccurs;
    }

    public Integer getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(Integer maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public PolyStringType getName() {
        return name;
    }

    public void setName(PolyStringType name) {
        this.name = name;
    }

    public PolyStringType getHelp() {
        return help;
    }

    public void setHelp(PolyStringType help) {
        this.help = help;
    }

    public Boolean isMustBeFirst() {
        return mustBeFirst;
    }

    public void setMustBeFirst(Boolean mustBeFirst) {
        this.mustBeFirst = mustBeFirst;
    }

    public Boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(Boolean success) {
        isSuccess = success;
    }
}
