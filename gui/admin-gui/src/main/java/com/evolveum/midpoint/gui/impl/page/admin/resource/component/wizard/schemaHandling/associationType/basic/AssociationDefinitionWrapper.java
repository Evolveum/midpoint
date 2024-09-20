/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class AssociationDefinitionWrapper implements Serializable {

    private final QName associationAttribute;

    private final List<ParticipantWrapper> subjects = new ArrayList<>();

    private final List<ParticipantWrapper> objects = new ArrayList<>();

    private ParticipantWrapper associationData;

    private PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> sourceValue;

    public AssociationDefinitionWrapper(
            ResourceObjectDefinition subject,
            ShadowReferenceAttributeDefinition refAttrDef,
            ResourceSchema resourceSchema) {
        this.associationAttribute = refAttrDef.getItemName();

        @NotNull Collection<ShadowRelationParticipantType> participants = refAttrDef.getTargetParticipantTypes();
        if (participants.size() == 1) {
            @NotNull ResourceObjectDefinition participantDef = participants.iterator().next().getObjectDefinition();
            if (participantDef.getObjectClassDefinition().isEmbedded()) {
                this.associationData = new ParticipantWrapper(participantDef.getObjectClassName());
            }
        }

        if (subject instanceof ResourceObjectTypeDefinition subjectObjectTypeDef) {
            this.subjects.add(new ParticipantWrapper(subjectObjectTypeDef.getKind(), subjectObjectTypeDef.getIntent(), subject.getObjectClassName()));
        } else {
            this.subjects.add(new ParticipantWrapper(subject.getObjectClassName()));
        }

        List<ShadowRelationParticipantType> objectsDefs = ProvisioningObjectsUtil.getObjectsOfSubject(refAttrDef);
        objectsDefs.forEach(associationObjectParticipantDef -> createObjectItem(
                associationObjectParticipantDef.getTypeIdentification(),
                associationObjectParticipantDef.getObjectDefinition(),
                subject.getObjectClassName()));

        if (objects.size() == 1) {
            List<ResourceObjectTypeDefinition> foundObjectTypes = resourceSchema.getObjectTypeDefinitions().stream()
                    .filter(objectType -> QNameUtil.match(objectType.getObjectClassName(), objects.get(0).getObjectClass()))
                    .toList();
            if (foundObjectTypes.size() == 1) {
                ResourceObjectTypeDefinition foundObjectType = foundObjectTypes.get(0);
                objects.clear();
                objects.add(new ParticipantWrapper(foundObjectType.getKind(), foundObjectType.getIntent(), foundObjectType.getObjectClassName()));
            }
        }
    }

    public AssociationDefinitionWrapper(PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value, ResourceSchema resourceSchema) {
        this.sourceValue = value;
        ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(resourceSchema, value);
        this.associationAttribute = assocDef.getItemName();
        if (assocDef.isComplex()) {
            ResourceObjectDefinition associationDataDef = assocDef.getAssociationDataObjectDefinition();
            this.associationData = new ParticipantWrapper(associationDataDef.getObjectClassName());
        }

        List<ResourceObjectTypeIdentificationType> subjects = AssociationChildWrapperUtil.getObjectTypesOfSubject(value);
        subjects.forEach(subject -> {
            @Nullable ResourceObjectDefinition objectTypeDef = resourceSchema.findObjectDefinition(ResourceObjectTypeIdentification.of(subject));
            if (objectTypeDef != null) {
                if (objectTypeDef instanceof ResourceObjectTypeDefinition objectObjectTypeDef) {
                    this.subjects.add(new ParticipantWrapper(
                            objectObjectTypeDef.getKind(),
                            objectObjectTypeDef.getIntent(),
                            objectTypeDef.getObjectClassName()));
                } else {
                    this.subjects.add(new ParticipantWrapper(objectTypeDef.getObjectClassName()));
                }
            }
        });

        List<ResourceObjectTypeIdentificationType> objects = AssociationChildWrapperUtil.getObjectTypesOfObject(value);
        objects.forEach(object -> {
            @Nullable ResourceObjectDefinition objectTypeDef = resourceSchema.findObjectDefinition(ResourceObjectTypeIdentification.of(object));
            if (objectTypeDef != null) {
                if (objectTypeDef instanceof ResourceObjectTypeDefinition objectObjectTypeDef) {
                    this.objects.add(new ParticipantWrapper(
                            objectObjectTypeDef.getKind(),
                            objectObjectTypeDef.getIntent(),
                            objectTypeDef.getObjectClassName()));
                } else {
                    this.objects.add(new ParticipantWrapper(objectTypeDef.getObjectClassName()));
                }
            }
        });
    }

    private void createObjectItem(
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectDefinition objectDef,
            QName subjectObjectClassName) {
        if (typeIdentification != null) {
            this.objects.add(new ParticipantWrapper(
                    typeIdentification.getKind(),
                    typeIdentification.getIntent(), subjectObjectClassName));
        } else {
            this.objects.add(new ParticipantWrapper(objectDef.getObjectClassName()));
        }
    }

    public QName getAssociationAttribute() {
        return associationAttribute;
    }

    public List<ParticipantWrapper> getSubjects() {
        return subjects;
    }

    public ParticipantWrapper getSubject() {
        return subjects.iterator().next();
    }

    public List<ParticipantWrapper> getObjects() {
        return objects;
    }

    @Nullable
    public PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> getSourceValue() {
        return sourceValue;
    }

    public ParticipantWrapper getAssociationData() {
        return associationData;
    }

    public void changeSubjectToObjectClassSelect() {
        subjects.iterator().next().kind = null;
        subjects.iterator().next().intent = null;
    }

    public boolean equalsSubject(AssociationDefinitionWrapper wrapper) {
        if (!QNameUtil.match(this.associationAttribute, wrapper.getAssociationAttribute())) {
            return false;
        }

        if (!QNameUtil.match(getSubject().objectClass, wrapper.getSubject().objectClass)) {
            return false;
        }

        if (getSubject().kind != wrapper.getSubject().kind) {
            return false;
        }

        return true;
    }

    public class ParticipantWrapper implements Serializable {
        private ShadowKindType kind;
        private String intent;
        private final QName objectClass;

        private ParticipantWrapper(@NotNull ShadowKindType kind, String intent, @NotNull QName objectClass) {
            this.kind = kind;
            this.intent = intent;
            this.objectClass = objectClass;
        }

        private ParticipantWrapper(@NotNull QName objectClass) {
            this.kind = null;
            this.intent = null;
            this.objectClass = objectClass;
        }

        public ShadowKindType getKind() {
            return kind;
        }

        public String getIntent() {
            return intent;
        }

        public QName getObjectClass() {
            return objectClass;
        }
    }
}
