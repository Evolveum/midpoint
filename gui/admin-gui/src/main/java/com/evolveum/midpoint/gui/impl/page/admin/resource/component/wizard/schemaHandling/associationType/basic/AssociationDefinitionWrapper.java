/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class AssociationDefinitionWrapper implements Serializable {

    private final QName associationAttribute;

    private final ParticipantWrapper subject;

    private final List<ParticipantWrapper> objects = new ArrayList<>();

    public AssociationDefinitionWrapper(
            ResourceObjectDefinition subject,
            ShadowReferenceAttributeDefinition refAttrDef,
            CompleteResourceSchema resourceSchema) {
        this.associationAttribute = refAttrDef.getItemName();
        if (subject instanceof ResourceObjectTypeDefinition subjectObjectTypeDef) {
            this.subject = new ParticipantWrapper(subjectObjectTypeDef.getKind(), subjectObjectTypeDef.getIntent(), subject.getObjectClassName());
        } else {
            this.subject = new ParticipantWrapper(subject.getObjectClassName());
        }

        refAttrDef.getTargetParticipantTypes().forEach(objectParticipantDef -> {
            @NotNull ResourceObjectDefinition objectDef = objectParticipantDef.getObjectDefinition();
            if (objectDef.getObjectClassDefinition().isAssociationObject()) {
                objectDef.getReferenceAttributeDefinitions();
                return;
            }

            @Nullable ResourceObjectTypeIdentification typeIdentification = objectParticipantDef.getTypeIdentification();
            if (typeIdentification != null) {
                this.objects.add(new ParticipantWrapper(
                        typeIdentification.getKind(),
                        typeIdentification.getIntent(), subject.getObjectClassName()));
            } else {
                this.objects.add(new ParticipantWrapper(objectDef.getObjectClassName()));
            }
        });
    }

    public QName getAssociationAttribute() {
        return associationAttribute;
    }

    public ParticipantWrapper getSubject() {
        return subject;
    }

    public List<ParticipantWrapper> getObjects() {
        return objects;
    }

    public void changeSubjectToObjectClassSelect() {
        subject.kind = null;
        subject.intent = null;
    }

    public boolean equalsSubject(AssociationDefinitionWrapper wrapper) {
        if (!QNameUtil.match(this.associationAttribute, wrapper.getAssociationAttribute())) {
            return false;
        }

        if (!QNameUtil.match(subject.objectClass, wrapper.getSubject().objectClass)) {
            return false;
        }

        if (subject.kind != wrapper.getSubject().kind) {
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
