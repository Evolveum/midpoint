/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationPairType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

/** A directional subject-to-object pair used to exclude configured associations from suggestions. */
public record AssociationPair(ResourceObjectTypeIdentification subject,
                              ResourceObjectTypeIdentification object) implements Serializable {

    public AssociationPair {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(object);
    }

    public AssociationPairType asBean() {
        return new AssociationPairType().subject(subject.asBean()).object(object.asBean());
    }

    public static AssociationPair fromBean(AssociationPairType bean) {
        return new AssociationPair(
                ResourceObjectTypeIdentification.of(bean.getSubject()),
                ResourceObjectTypeIdentification.of(bean.getObject()));
    }

    /** Includes every participant combination; incomplete or unknown identifications are ignored. */
    public static Set<AssociationPair> fromDefinition(ShadowAssociationTypeDefinitionType association) {
        Set<AssociationPair> pairs = new HashSet<>();
        if (association.getSubject() == null) {
            return pairs;
        }
        for (var subjectType : association.getSubject().getObjectType()) {
            var subject = ResourceObjectTypeIdentification.createIfKnown(subjectType.getKind(), subjectType.getIntent());
            if (subject == null) {
                continue;
            }
            for (var participant : association.getObject()) {
                for (var objectType : participant.getObjectType()) {
                    var object = ResourceObjectTypeIdentification.createIfKnown(objectType.getKind(), objectType.getIntent());
                    if (object != null) {
                        pairs.add(new AssociationPair(subject, object));
                    }
                }
            }
        }
        return pairs;
    }
}
