/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ObjectTypesSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final QName objectClassName;
    @Nullable private final String statisticsObjectOid;

    ObjectTypesSuggestionWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (ObjectTypesSuggestionWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(Referencable.getOid(typedDefinition.getResourceRef()), "No resource OID specified");
        objectClassName = configNonNull(typedDefinition.getObjectclass(), "No object class name specified");
        statisticsObjectOid = Referencable.getOid(typedDefinition.getStatisticsRef());
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public QName getObjectClassName() {
        return objectClassName;
    }

    public @Nullable String getStatisticsObjectOid() {
        return statisticsObjectOid;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {
        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .objectclass(objectClassName));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassName", objectClassName, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "statisticsObjectOid", statisticsObjectOid, indent+1);
    }
}
