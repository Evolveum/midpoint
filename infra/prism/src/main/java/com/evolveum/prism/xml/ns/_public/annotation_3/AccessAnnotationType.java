
package com.evolveum.prism.xml.ns._public.annotation_3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AccessAnnotationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AccessAnnotationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="read"/>
 *     &lt;enumeration value="update"/>
 *     &lt;enumeration value="create"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "AccessAnnotationType", namespace = "http://prism.evolveum.com/xml/ns/public/annotation-2")
@XmlEnum
public enum AccessAnnotationType {

    @XmlEnumValue("read")
    READ("read"),
    @XmlEnumValue("update")
    UPDATE("update"),
    @XmlEnumValue("create")
    CREATE("create");
    private final String value;

    AccessAnnotationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AccessAnnotationType fromValue(String v) {
        for (AccessAnnotationType c: AccessAnnotationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
