/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.prism.PrismConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author lukas
 */
public class AvailableRelationDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<QName> availableRelationList = new ArrayList<QName>();
    private QName defaultRelation = PrismConstants.Q_ANY;

    public AvailableRelationDto() {
    }

    public AvailableRelationDto(List<QName> availableRelationList) {
        this.availableRelationList = availableRelationList;
    }

    public AvailableRelationDto(List<QName> availableRelationList, QName defaultRelation) {
        this.availableRelationList = availableRelationList;
        this.defaultRelation = defaultRelation;
    }

    public void setAvailableRelationList(List<QName> availableRelationList) {
        this.availableRelationList = availableRelationList;
    }

    public List<QName> getAvailableRelationList() {
        return availableRelationList;
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }

    public QName getDefaultRelation() {
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        if (defaultRelation == null) {
            return null;
        }
        if (availableRelationList != null && availableRelationList.contains(defaultRelation)) {
            return defaultRelation;
        }
        return null;
    }
}
