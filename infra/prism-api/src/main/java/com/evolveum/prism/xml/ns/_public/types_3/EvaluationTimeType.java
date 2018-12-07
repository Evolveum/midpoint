
package com.evolveum.prism.xml.ns._public.types_3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EvaluationTimeType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="EvaluationTimeType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="import"/&gt;
 *     &lt;enumeration value="run"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 *
 */
@XmlType(name = "EvaluationTimeType")
@XmlEnum
public enum EvaluationTimeType {


    /**
     *
     * <pre>
     * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;p xmlns:a="http://prism.evolveum.com/xml/ns/public/annotation-3" xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3" xmlns:cap="http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-3" xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3" xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3" xmlns:tns="http://midpoint.evolveum.com/xml/ns/public/common/common-3" xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;
     *                 			Import-time. Evaluation happens when the object is imported
     *                 			into midPoint repository.
     *                 		&lt;/p&gt;
     * </pre>
     *
     *
     */
    @XmlEnumValue("import")
    IMPORT("import"),

    /**
     *
     * <pre>
     * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;p xmlns:a="http://prism.evolveum.com/xml/ns/public/annotation-3" xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3" xmlns:cap="http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-3" xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3" xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3" xmlns:tns="http://midpoint.evolveum.com/xml/ns/public/common/common-3" xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc" xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;
     * 	                		Run-time. Evaluation happens every time when the object is used.
     *                 		&lt;/p&gt;
     * </pre>
     *
     *
     */
    @XmlEnumValue("run")
    RUN("run");
    private final String value;

    EvaluationTimeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EvaluationTimeType fromValue(String v) {
        for (EvaluationTimeType c: EvaluationTimeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
