package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

public class FocusTypeSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final ResourceObjectTypeIdentification typeIdentification;
    private final ShadowKindType kind;
    private final String intent;

    FocusTypeSuggestionWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (FocusTypeSuggestionWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(Referencable.getOid(typedDefinition.getResourceRef()), "No resource OID specified");
        typeIdentification =
                ResourceObjectTypeIdentification.of(
                        typedDefinition.getKind(),
                        typedDefinition.getIntent());
        kind = typedDefinition.getKind();
        intent = typedDefinition.getIntent();
    }

    public String getResourceOid() { return resourceOid; }

    public ResourceObjectTypeIdentification getTypeIdentification() { return typeIdentification; }

    public ShadowKindType getKind() { return kind; }

    public String getIntent() { return intent; }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {
        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .kind(kind)
                        .intent(intent));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "kind", kind, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent+1);
    }
}
