/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Definition of a high-level association type.
 *
 * The association type consists of:
 *
 * . a subject,
 * . zero or more objects,
 * . optionally an "associated object"; TODO we need a better term here.
 *
 * This definition is derived from {@link ShadowAssociationTypeDefinitionType}. However, there's not much here!
 * Almost everything from that bean goes to the {@link ShadowReferenceAttributeDefinition} attached to the subject or an object.
 * We only need to keep additional {@link ShadowRelationParticipantType}s here - eventually. Currently we do not support
 * that.
 *
 * So, currently this class is empty. But it is here for (near) future extensions.
 */
public class ShadowAssociationTypeDefinition
        implements DebugDumpable, Serializable {

    private ShadowAssociationTypeDefinition() {
    }

    public static ShadowAssociationTypeDefinition create() {
        return new ShadowAssociationTypeDefinition();
    }

    public static ShadowAssociationTypeDefinition empty() {
        return new ShadowAssociationTypeDefinition();
    }

    @Override
    public String debugDump(int indent) {
        return "TODO";
    }
}
