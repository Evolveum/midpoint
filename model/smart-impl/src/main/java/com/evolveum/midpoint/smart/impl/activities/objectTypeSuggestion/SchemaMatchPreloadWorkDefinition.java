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

class SchemaMatchPreloadWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final QName objectClassName;
    private final List<DataAccessPermissionType> permissions;
    @Nullable private final String sourceTaskOid;

    SchemaMatchPreloadWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (SchemaMatchPreloadWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(Referencable.getOid(typedDefinition.getResourceRef()), "No resource OID specified");
        objectClassName = configNonNull(typedDefinition.getObjectclass(), "No object class name specified");
        permissions = typedDefinition.getPermissions();
        sourceTaskOid = Referencable.getOid(typedDefinition.getSourceTaskRef());
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public QName getObjectClassName() {
        return objectClassName;
    }

    public List<DataAccessPermissionType> getPermissions() {
        return permissions;
    }

    public @Nullable String getSourceTaskOid() {
        return sourceTaskOid;
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
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassName", objectClassName, indent + 1);
    }
}
