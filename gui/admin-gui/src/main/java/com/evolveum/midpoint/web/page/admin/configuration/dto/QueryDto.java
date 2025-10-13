/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class QueryDto implements Serializable {

    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_MIDPOINT_QUERY = "midPointQuery";
    public static final String F_XML_QUERY = "xmlQuery";


    private QName objectType = ObjectTypes.OBJECT.getTypeQName();
    private String midPointQuery = "";
    private String xmlQuery = "";

    public QueryDto() {
    }


    public QName getObjectType() {
        return objectType;
    }

    public void setObjectType(QName objectType) {
        this.objectType = objectType;
    }

    public String getMidPointQuery() {
        return midPointQuery;
    }

    public void setMidPointQuery(String midPointQuery) {
        this.midPointQuery = midPointQuery;
    }

    public void setXmlQuery(String xmlQuery) {
        this.xmlQuery = xmlQuery;
    }

    public String getXmlQuery() {
        return xmlQuery;
    }
}
