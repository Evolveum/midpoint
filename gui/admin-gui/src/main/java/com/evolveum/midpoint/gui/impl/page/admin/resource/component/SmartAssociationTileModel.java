/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.findObjectTypeDefinition;

public class SmartAssociationTileModel extends TemplateTile<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> {

    String icon;
    String name;
    String description;
    String subjectName;
    String objectName;
    QName subjectType;
    QName objectType;

    String resourceOid;
    String statusInfoToken;

    String cssTag;
    String cssIconTag;
    String textTag;

    boolean isSuggestion;

    public SmartAssociationTileModel(
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueWrapper,
            @NotNull ResourceType resource,
            String statusInfoToken) {
        super(valueWrapper);
        setValue(valueWrapper);
        ShadowAssociationTypeDefinitionType realValue = valueWrapper.getRealValue();

        this.name = realValue.getDisplayName() != null ? realValue.getDisplayName() : "-";
        this.description = realValue.getDescription() != null ? realValue.getDescription() : "-";

        this.cssTag = "system-badge";
        this.cssIconTag = "fa fa-gear mr-1";
        this.textTag = "System suggestion";

        List<ResourceObjectTypeIdentificationType> subjectList =
                realValue.getSubject() != null ? realValue.getSubject().getObjectType() : null;

        List<ShadowAssociationTypeObjectDefinitionType> objectList = realValue.getObject();

        ResourceObjectTypeIdentificationType subjectId =
                subjectList != null && !subjectList.isEmpty() ? subjectList.get(0) : null;

        ResourceObjectTypeIdentificationType objectId =
                (objectList != null && !objectList.isEmpty()
                        && objectList.get(0).getObjectType() != null
                        && !objectList.get(0).getObjectType().isEmpty())
                        ? objectList.get(0).getObjectType().get(0)
                        : null;

        ResourceObjectTypeDefinitionType subjectTypeDefinition =
                subjectId != null
                        ? findObjectTypeDefinition(resource.asPrismObject(), subjectId.getKind(), subjectId.getIntent())
                        : null;

        ResourceObjectTypeDefinitionType objectTypeDefinition =
                objectId != null
                        ? findObjectTypeDefinition(resource.asPrismObject(), objectId.getKind(), objectId.getIntent())
                        : null;

        if (objectTypeDefinition != null) {
            this.objectName = objectTypeDefinition.getDisplayName();
            this.objectType = objectTypeDefinition.getDelineation().getObjectClass();
        }

        if (subjectTypeDefinition != null) {
            this.subjectName = subjectTypeDefinition.getDisplayName();
            this.subjectType = subjectTypeDefinition.getDelineation().getObjectClass();
        }

        this.resourceOid = resource.getOid();
        this.statusInfoToken = statusInfoToken;
        this.isSuggestion = statusInfoToken != null && !statusInfoToken.isEmpty();
    }
    protected StatusInfo<CorrelationSuggestionsType> getStatusInfo(@NotNull PageBase pageBase, Task task, OperationResult result) {
        SmartIntegrationService smartService = pageBase.getSmartIntegrationService();
        if (statusInfoToken != null) {
            try {
                return smartService.getSuggestCorrelationOperationStatus(statusInfoToken, task, result);
            } catch (Throwable e) {
                pageBase.error("Couldn't get correlation suggestion statusInfo: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getObjectName() {
        return objectName;
    }

    public QName getSubjectType() {
        return subjectType;
    }

    public QName getObjectType() {
        return objectType;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public String getCssTag() {
        return cssTag;
    }

    public String getCssIconTag() {
        return cssIconTag;
    }

    public String getTextTag() {
        return textTag;
    }

    public boolean isSuggestion() {
        return isSuggestion;
    }

    public @Nullable String getAssociationObjectObjectClass() {
        ShadowAssociationTypeDefinitionType realValue = getValue() != null
                ? getValue().getRealValue()
                : null;
        if (realValue == null) {
            return null;
        }

        ResourceObjectTypeDefinitionType associationObject = realValue.getAssociationObject();
        if (associationObject == null
                || associationObject.getDelineation() == null
                || associationObject.getDelineation().getObjectClass() == null) {
            return null;
        }
        QName objectClass = associationObject.getDelineation().getObjectClass();
        return objectClass.getLocalPart() != null ? objectClass.getLocalPart() : "";
    }

}
