/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionReportEngineConfigurationType;

import javax.xml.namespace.QName;

public class SearchContext {

    private ResourceObjectDefinition resourceObjectDefinition;
    private PrismContainerDefinition<? extends Containerable> definitionOverride;

    private CollectionPanelType collectionPanelType;

    private QName assignmentTargetType;
    private ObjectCollectionReportEngineConfigurationType reportCollection;

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public void setResourceObjectDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    public CollectionPanelType getPanelType() {
        return collectionPanelType;
    }

    public void setPanelType(CollectionPanelType collectionPanelType) {
        this.collectionPanelType = collectionPanelType;
    }

    public QName getAssignmentTargetType() {
        return assignmentTargetType;
    }

    public void setAssignmentTargetType(QName assignmentTargetType) {
        this.assignmentTargetType = assignmentTargetType;
    }

    public ObjectCollectionReportEngineConfigurationType getReportCollection() {
        return reportCollection;
    }

    public void setReportCollection(ObjectCollectionReportEngineConfigurationType reportCollection) {
        this.reportCollection = reportCollection;
    }

    public PrismContainerDefinition<? extends Containerable> getDefinitionOverride() {
        return definitionOverride;
    }

    public void setDefinitionOverride(PrismContainerDefinition<? extends Containerable> definitionOverride) {
        this.definitionOverride = definitionOverride;
    }
}
