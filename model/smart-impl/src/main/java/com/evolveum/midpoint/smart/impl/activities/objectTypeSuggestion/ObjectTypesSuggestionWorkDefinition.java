/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeSuggestion;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ObjectTypesSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final QName objectClassName;
    @Nullable private final String statisticsObjectOid;
    private final List<DataAccessPermissionType> permissions;

    ObjectTypesSuggestionWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (ObjectTypesSuggestionWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(Referencable.getOid(typedDefinition.getResourceRef()), "No resource OID specified");
        objectClassName = configNonNull(typedDefinition.getObjectclass(), "No object class name specified");
        statisticsObjectOid = Referencable.getOid(typedDefinition.getStatisticsRef());
        this.permissions = typedDefinition.getPermissions();
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

    public List<DataAccessPermissionType> getPermissions() {
        return this.permissions;
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
