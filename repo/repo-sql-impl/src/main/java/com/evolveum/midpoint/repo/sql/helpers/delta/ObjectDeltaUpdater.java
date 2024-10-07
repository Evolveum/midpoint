/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.evolveum.midpoint.schema.util.cid.ContainerValueIdGenerator;

import jakarta.persistence.metamodel.ManagedType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.ObjectUpdater;
import com.evolveum.midpoint.repo.sql.helpers.modify.EntityRegistry;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityMapper;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Carries out object update (both in repository tables, except for RObject.fullObject) and in memory.
 *
 * @author Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired PrismContext prismContext;
    @Autowired RepositoryService repositoryService;
    @Autowired EntityRegistry entityRegistry;
    @Autowired RelationRegistry relationRegistry;
    @Autowired PrismEntityMapper prismEntityMapper;
    @Autowired ExtItemDictionary extItemDictionary;
    @Autowired SqlRepositoryConfiguration repositoryConfiguration;

    /**
     * modify
     */
    public <T extends ObjectType> RObject modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications,
            PrismObject<T> prismObject, RepoModifyOptions modifyOptions, EntityManager em,
            ObjectUpdater.AttemptContext attemptContext) throws SchemaException {

        LOGGER.trace("Starting to build entity changes for {}, {}, \n{}", type, oid, DebugUtil.debugDumpLazily(modifications));

        // normalize reference.relation QNames like it's done here ObjectTypeUtil.normalizeAllRelations(prismObject);

        // how to generate identifiers correctly now? to repo entities and to full xml, ids in full XML are generated
        // on different place than we later create new containers...how to match them

        // set proper owner/ownerOid/ownerType for containers/references/result and others

        // todo implement transformation from prism to entity (PrismEntityMapper), probably ROperationResult missing

        // validate lookup tables and certification campaigns

        // mark newly added containers/references as transient

        // validate metadata/*, assignment/metadata/*, assignment/construction/resourceRef changes

        ContainerValueIdGenerator idGenerator = new ContainerValueIdGenerator(prismObject);
        idGenerator.generateForNewObject();



        UpdateContext ctx = new UpdateContext(this, modifyOptions, idGenerator, em, attemptContext);

        // Preprocess modifications: We want to process only real modifications.
        //
        // What is "real"? There are three types of changes:
        // 1) changes that need to be reflected both in XML and in tables
        // 2) changes that need to be reflected in XML but not in tables (e.g. adding values with different metadata or operational data)
        // 3) changes that need to be reflected in tables but not in XML (index-only items, photo, task result, and so on)
        //
        // Category-2 changes are to be treated very carefully: we should avoid phantom add+delete in tables.
        // Category-3 changes are (hopefully) not narrowed out. [See assumeMissingItems / MID-5280.]

        // Modifications were narrowed in caller in order to propagate value to result.
        Collection<? extends ItemDelta<?, ?>> narrowedModifications = modifications;

        // Here we can still have some ADD or REPLACE operations that are significant from the point of full object application
        // but irrelevant as far as "index" tables are concerned. I.e. category-2 changes. For example, ADD values with
        // different operational data or value metadata.

        Class<? extends RObject> objectClass = RObjectType.getByJaxbType(type).getClazz();
        RObject object = em.getReference(objectClass, oid);

        // Is this correct? should we get type from rObject?
        //ManagedType<T> mainEntityType = entityRegistry.getJaxbMapping(type);

        ManagedType<T> mainEntityType = (ManagedType) entityRegistry.getMapping(object.getClass());

        for (ItemDelta<?, ?> delta : narrowedModifications) {
            UpdateDispatcher.dispatchModification(prismObject, ctx, object, mainEntityType, delta);
        }

        // the following will apply deltas to prismObject
        handleObjectCommonAttributes(type, narrowedModifications, prismObject, object);

        if (ctx.shadowPendingOperationModified) {
            ((RShadow) object).setPendingOperationCount(((ShadowType) prismObject.asObjectable()).getPendingOperation().size());
        }

        LOGGER.trace("Entity changes applied");

        return object;
    }

    private <T extends ObjectType> void handleObjectCommonAttributes(Class<T> type, Collection<? extends ItemDelta<?, ?>> modifications,
            PrismObject<T> prismObject, RObject object) {

        // update version
        String strVersion = prismObject.getVersion();
        int version;
        if (StringUtils.isNotBlank(strVersion)) {
            try {
                version = Integer.parseInt(strVersion) + 1;
            } catch (NumberFormatException e) {
                version = 1;
            }
        } else {
            version = 1;
        }
        object.setVersion(version);

        // item deltas were already applied to prism object

        handleObjectTextInfoChanges(type, modifications, prismObject, object);

        // normalize all relations
        ObjectTypeUtil.normalizeAllRelations(prismObject, relationRegistry);

        // full object column will be updated later
    }

    private void handleObjectTextInfoChanges(Class<? extends ObjectType> type, Collection<? extends ItemDelta<?, ?>> modifications,
            PrismObject<?> prismObject, RObject object) {
        FullTextSearchConfigurationType config = repositoryService.getFullTextSearchConfiguration();
        if (!FullTextSearchUtil.isObjectTextInfoRecomputationNeeded(config, type, modifications)) {
            return;
        }

        Set<RObjectTextInfo> newInfos = RObjectTextInfo.createItemsSet((ObjectType) prismObject.asObjectable(), object,
                new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary, repositoryConfiguration));

        if (newInfos == null || newInfos.isEmpty()) {
            object.getTextInfoItems().clear();
        } else {
            Set<String> existingTexts = object.getTextInfoItems().stream().map(info -> info.getText()).collect(Collectors.toSet());
            Set<String> newTexts = newInfos.stream().map(info -> info.getText()).collect(Collectors.toSet());

            object.getTextInfoItems().removeIf(existingInfo -> !newTexts.contains(existingInfo.getText()));
            for (RObjectTextInfo newInfo : newInfos) {
                if (!existingTexts.contains(newInfo.getText())) {
                    object.getTextInfoItems().add(newInfo);
                }
            }
        }
    }

    RepositoryContext createRepositoryContext() {
        return new RepositoryContext(repositoryService, prismContext,
                relationRegistry, extItemDictionary, repositoryConfiguration);
    }

}
