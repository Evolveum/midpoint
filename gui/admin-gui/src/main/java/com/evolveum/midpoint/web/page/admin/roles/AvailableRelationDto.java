/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationSearchItemConfigurationType;

import org.apache.cxf.common.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author lukas
 */
public class AvailableRelationDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<QName> availableRelationList = new ArrayList<QName>();
    private QName defaultRelation = PrismConstants.Q_ANY;
    private final RelationSearchItemConfigurationType configuration;

    public AvailableRelationDto(List<QName> availableRelationList, RelationSearchItemConfigurationType configuration) {
        this.availableRelationList = availableRelationList;
        this.configuration = configuration;
    }

    public AvailableRelationDto(List<QName> availableRelationList, QName defaultRelation, RelationSearchItemConfigurationType configuration) {
        this.availableRelationList = availableRelationList;
        this.defaultRelation = defaultRelation;
        this.configuration = configuration;
    }

    public void setAvailableRelationList(List<QName> availableRelationList) {
        this.availableRelationList = availableRelationList;
    }

    public List<QName> getAvailableRelationList() {
        if (configuration == null || CollectionUtils.isEmpty(configuration.getSupportedRelations())) {
            return availableRelationList;
        }
        return configuration.getSupportedRelations();
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }

    public QName getDefaultRelation() {
        return getDefaultRelation(false);
    }

    private QName getDefaultRelation(boolean allowAny) {
        List<QName>  availableRelationList = getAvailableRelationList();
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        QName defaultRelation = configuration == null ? null : configuration.getDefaultValue();
        if (defaultRelation == null) {
            if (this.defaultRelation == null) {
                return null;
            }
            defaultRelation = this.defaultRelation;
        }
        if (allowAny && QNameUtil.match(defaultRelation, PrismConstants.Q_ANY)){
            return defaultRelation;
        }
        if (availableRelationList != null && availableRelationList.contains(defaultRelation)) {
            return defaultRelation;
        }
        return null;
    }

    public QName getDefaultRelationAllowAny() {
        return getDefaultRelation(true);
    }
}
