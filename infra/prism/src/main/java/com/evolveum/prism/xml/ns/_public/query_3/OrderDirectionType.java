
package com.evolveum.prism.xml.ns._public.query_3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OrderDirectionType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="OrderDirectionType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ascending"/>
 *     &lt;enumeration value="descending"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "OrderDirectionType")
@XmlEnum
public enum OrderDirectionType {

    @XmlEnumValue("ascending")
    ASCENDING("ascending"),
    @XmlEnumValue("descending")
    DESCENDING("descending");
    private final String value;

    OrderDirectionType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OrderDirectionType fromValue(String v) {
        for (OrderDirectionType c: OrderDirectionType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
