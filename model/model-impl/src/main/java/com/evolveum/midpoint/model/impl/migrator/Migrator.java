/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.migrator;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 *
 */
@Component
public class Migrator {

    public <I extends ObjectType, O extends ObjectType> PrismObject<O> migrate(PrismObject<I> original) {
        Class<I> origType = original.getCompileTimeClass();
        if (ObjectTemplateType.class.isAssignableFrom(origType)) {
            PrismObject<ObjectTemplateType> out = migrateObjectTemplate((PrismObject<ObjectTemplateType>) original);
            return (PrismObject<O>) out;
        }
        if (ResourceType.class.isAssignableFrom(origType)) {
            PrismObject<ResourceType> out = migrateResource((PrismObject<ResourceType>) original);
            return (PrismObject<O>) out;
        }
        if (FocusType.class.isAssignableFrom(origType)) {
            PrismObject<FocusType> out = migrateFocus((PrismObject<FocusType>) original);
            original = (PrismObject<I>) out;
        }
        if (UserType.class.isAssignableFrom(origType)) {
            PrismObject<UserType> out = migrateUser((PrismObject<UserType>) original);
            return (PrismObject<O>) out;
        }
        return (PrismObject<O>) original;
    }

    private PrismObject<ObjectTemplateType> migrateObjectTemplate(PrismObject<ObjectTemplateType> orig) {
        QName elementName = orig.getElementName();
        if (elementName.equals(SchemaConstants.C_OBJECT_TEMPLATE)) {
            return orig;
        }
        PrismObject<ObjectTemplateType> migrated = orig.clone();
        migrated.setElementName(SchemaConstants.C_OBJECT_TEMPLATE);
        return migrated;
    }

    private PrismObject<ResourceType> migrateResource(PrismObject<ResourceType> orig) {
        return orig;
    }

    private void migrateObjectSynchronization(ObjectSynchronizationType sync) {
        if (sync == null || sync.getReaction() == null){
            return;
        }

        List<SynchronizationReactionType> migratedReactions = new ArrayList<>();
        for (SynchronizationReactionType reaction : sync.getReaction()){
            if (reaction.getAction() == null){
                continue;
            }
            List<SynchronizationActionType> migratedAction = new ArrayList<>();
            for (SynchronizationActionType action : reaction.getAction()){
                migratedAction.add(migrateAction(action));
            }
            SynchronizationReactionType migratedReaction = reaction.clone();
            migratedReaction.getAction().clear();
            migratedReaction.getAction().addAll(migratedAction);
            migratedReactions.add(migratedReaction);
        }

        sync.getReaction().clear();
        sync.getReaction().addAll(migratedReactions);
    }

    private SynchronizationActionType migrateAction(SynchronizationActionType action){
        return action;
    }

    private PrismObject<FocusType> migrateFocus(PrismObject<FocusType> orig) {
        return orig;
    }

    private PrismObject<UserType> migrateUser(PrismObject<UserType> orig) {
        return orig;
    }

    public <F extends ObjectType> void executeAfterOperationMigration(LensContext<F> context, OperationResult result) {

    }

}
