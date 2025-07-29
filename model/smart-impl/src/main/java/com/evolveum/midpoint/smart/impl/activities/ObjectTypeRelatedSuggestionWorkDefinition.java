/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeRelatedSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * For suggesting correlation and mappings (for given object type).
 */
public class ObjectTypeRelatedSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final ResourceObjectTypeIdentification typeIdentification;

    ObjectTypeRelatedSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (ObjectTypeRelatedSuggestionWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(Referencable.getOid(typedDefinition.getResourceRef()), "No resource OID specified");
        typeIdentification =
                ResourceObjectTypeIdentification.of(
                        configNonNull(typedDefinition.getObjectType(), "No object type specified"));
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {
        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .kind(typeIdentification.getKind())
                        .intent(typeIdentification.getIntent()));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "typeIdentification", typeIdentification.toString(), indent+1);
    }
}
