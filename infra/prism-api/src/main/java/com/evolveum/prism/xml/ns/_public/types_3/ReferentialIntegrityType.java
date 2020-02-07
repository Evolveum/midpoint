/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.prism.xml.ns._public.types_3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ReferentialIntegrityType")
@XmlEnum
public enum ReferentialIntegrityType {

    @XmlEnumValue("strict")
    STRICT("strict"),

    @XmlEnumValue("relaxed")
    RELAXED("relaxed"),

    @XmlEnumValue("lax")
    LAX("lax"),

    @XmlEnumValue("default")
    DEFAULT("default");

    private final String value;

    ReferentialIntegrityType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ReferentialIntegrityType fromValue(String v) {
        for (ReferentialIntegrityType c: ReferentialIntegrityType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
