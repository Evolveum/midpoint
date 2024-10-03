/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.util.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RCertWorkItemReference;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.QueryEngine;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Contains methods specific to handle certification cases.
 * (As these cases are stored outside main certification campaign object.)
 * <p>
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 */
@Component
public class CertificationCaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationCaseHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private GeneralHelper generalHelper;
    @Autowired private ObjectRetriever objectRetriever;
    @Autowired private RepositoryService repositoryService;
    @Autowired private ExtItemDictionary extItemDictionary;
    @Autowired private BaseHelper baseHelper;

    void addCertificationCampaignCases(EntityManager em, RObject object, boolean deleteBeforeAdd) {
        if (!(object instanceof RAccessCertificationCampaign)) {
            return;
        }
        RAccessCertificationCampaign campaign = (RAccessCertificationCampaign) object;

        if (deleteBeforeAdd) {
            LOGGER.trace("Deleting existing cases for {}", campaign.getOid());
            deleteCertificationCampaignCases(em, campaign.getOid());
        }
        if (campaign.getCase() != null) {
            for (RAccessCertificationCase aCase : campaign.getCase()) {
                if (deleteBeforeAdd) {
                    aCase.setTransient(true);
                }
                em.merge(aCase);
            }
        }
    }

    private void addCertificationCampaignCases(EntityManager em, String campaignOid,
            Collection<PrismContainerValue> values, int currentId, List<Long> affectedIds)
            throws DtoTranslationException {

        for (PrismContainerValue value : values) {
            AccessCertificationCaseType caseType = new AccessCertificationCaseType();
            caseType.setupContainerValue(value);
            if (caseType.getId() == null) {
                caseType.setId((long) currentId);
                currentId++;
            }

            // we need to generate IDs but we (currently) do not use that for setting "isTransient" flag
            PrismIdentifierGenerator generator = new PrismIdentifierGenerator(PrismIdentifierGenerator.Operation.MODIFY);
            generator.generate(caseType);

            RAccessCertificationCase row = RAccessCertificationCase.toRepo(campaignOid, caseType, createRepositoryContext());
            row.setId(RUtil.toInteger(caseType.getId()));
            affectedIds.add(caseType.getId());

            em.merge(row);
        }
    }

    @NotNull
    private RepositoryContext createRepositoryContext() {
        return new RepositoryContext(repositoryService, prismContext, relationRegistry, extItemDictionary, baseHelper.getConfiguration());
    }

    void deleteCertificationCampaignCases(EntityManager em, String oid) {
        // TODO couldn't this cascading be done by hibernate itself?
//        Query deleteReferences = em.getNamedQuery("delete.campaignCasesReferences");
//        deleteReferences.setParameter("oid", oid);
//        deleteReferences.executeUpdate();

        Query deleteWorkItemReferences = em.createNamedQuery("delete.campaignCasesWorkItemReferences");
        deleteWorkItemReferences.setParameter("oid", oid);
        deleteWorkItemReferences.executeUpdate();

        Query deleteWorkItems = em.createNamedQuery("delete.campaignCasesWorkItems");
        deleteWorkItems.setParameter("oid", oid);
        deleteWorkItems.executeUpdate();

        Query deleteCases = em.createNamedQuery("delete.campaignCases");
        deleteCases.setParameter("oid", oid);
        deleteCases.executeUpdate();
    }

    <T extends ObjectType> Collection<? extends ItemDelta<?, ?>> filterCampaignCaseModifications(Class<T> type,
            Collection<? extends ItemDelta<?, ?>> modifications) {
        Collection<ItemDelta<?, ?>> caseDelta = new ArrayList<>();
        if (!AccessCertificationCampaignType.class.equals(type)) {
            return caseDelta;
        }

        ItemPath casePath = AccessCertificationCampaignType.F_CASE;
        for (ItemDelta<?, ?> delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Certification campaign cannot be modified via empty-path modification");
            } else if (path.equivalent(casePath)) {
                caseDelta.add(delta);
            } else if (path.isSuperPath(casePath)) { // like case[id]/xxx
                caseDelta.add(delta);
            }
        }

        //noinspection SuspiciousMethodCalls
        modifications.removeAll(caseDelta);

        return caseDelta;
    }

    void updateCampaignCases(EntityManager em, String campaignOid,
            Collection<? extends ItemDelta<?, ?>> modifications, RepoModifyOptions modifyOptions)
            throws SchemaException, ObjectNotFoundException, DtoTranslationException {
        if (modifications.isEmpty() && !RepoModifyOptions.isForceReindex(modifyOptions)) {
            return;
        }

        List<Long> casesAddedOrDeleted = addOrDeleteCases(em, campaignOid, modifications);
        LOGGER.trace("Cases added/deleted (null means REPLACE operation) = {}", casesAddedOrDeleted);

        updateCasesContent(em, campaignOid, modifications, casesAddedOrDeleted, modifyOptions);
    }

    private List<Long> addOrDeleteCases(EntityManager em, String campaignOid, Collection<? extends ItemDelta> modifications)
            throws SchemaException, DtoTranslationException {

        boolean replacePresent = false;
        List<Long> affectedIds = new ArrayList<>();
        for (ItemDelta delta : modifications) {
            ItemPath deltaPath = delta.getPath();
            if (!AccessCertificationCampaignType.F_CASE.isSubPathOrEquivalent(deltaPath)) {
                throw new IllegalStateException("Wrong campaign delta sneaked into updateCampaignCases: class=" + delta.getClass() + ", path=" + deltaPath);
            }

            if (deltaPath.size() == 1) {
                if (delta.getValuesToDelete() != null) {
                    // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                    for (PrismContainerValue<?> value : (Collection<PrismContainerValue<?>>) delta.getValuesToDelete()) {
                        Long id = value.getId();
                        if (id == null) {
                            throw new SchemaException("Couldn't delete certification case with null id");
                        }
                        affectedIds.add(id);
                        // TODO couldn't this cascading be done by hibernate itself?
                        Integer integerCaseId = RUtil.toInteger(id);
                        Query deleteWorkItemReferences = em.createNativeQuery("delete from " + RCertWorkItemReference.TABLE +
                                " where owner_owner_owner_oid=:oid and owner_owner_id=:id");
                        deleteWorkItemReferences.setParameter("oid", campaignOid);
                        deleteWorkItemReferences.setParameter("id", integerCaseId);
                        deleteWorkItemReferences.executeUpdate();
                        Query deleteCaseWorkItems = em.createNativeQuery("delete from " + RAccessCertificationWorkItem.TABLE +
                                " where owner_owner_oid=:oid and owner_id=:id");
                        deleteCaseWorkItems.setParameter("oid", campaignOid);
                        deleteCaseWorkItems.setParameter("id", integerCaseId);
                        deleteCaseWorkItems.executeUpdate();
                        Query deleteCase = em.createNamedQuery("delete.campaignCase");
                        deleteCase.setParameter("oid", campaignOid);
                        deleteCase.setParameter("id", integerCaseId);
                        deleteCase.executeUpdate();
                    }
                }
                // TODO generated IDs might conflict with client-provided ones
                // also, client-provided IDs might conflict with those that are already in the database
                // So it's safest not to provide any IDs by the client
                if (delta.getValuesToAdd() != null) {
                    int currentId = generalHelper.findLastIdInRepo(em, campaignOid, "get.campaignCaseLastId") + 1;
                    addCertificationCampaignCases(em, campaignOid, delta.getValuesToAdd(), currentId, affectedIds);
                }
                if (delta.getValuesToReplace() != null) {
                    deleteCertificationCampaignCases(em, campaignOid);
                    addCertificationCampaignCases(em, campaignOid, delta.getValuesToReplace(), 1, affectedIds);
                    replacePresent = true;
                }

            }
        }
        return replacePresent ? null : affectedIds;
    }

    private void updateCasesContent(EntityManager em, String campaignOid, Collection<? extends ItemDelta> modifications,
            List<Long> casesAddedOrDeleted, RepoModifyOptions modifyOptions) throws SchemaException, ObjectNotFoundException, DtoTranslationException {
        Set<Long> casesModified = new HashSet<>();
        for (ItemDelta delta : modifications) {
            ItemPath deltaPath = delta.getPath();
            if (deltaPath.size() > 1) {
                LOGGER.trace("Updating campaign {} with delta {}", campaignOid, delta);

                // should start with "case[id]"
                long id = checkPathSanity(deltaPath, casesAddedOrDeleted);

                Query query = em.createNamedQuery("get.campaignCase");
                query.setParameter("ownerOid", campaignOid);
                query.setParameter("id", (int) id);

                byte[] fullObject = RUtil.getSingleResultOrNull(query);
                if (fullObject == null) {
                    throw new ObjectNotFoundException(
                            String.format(
                                    "Couldn't update cert campaign %s by delta with path '%s' - specified case does not exist",
                                    campaignOid, deltaPath),
                            AccessCertificationCaseType.class,
                            campaignOid);
                }
                AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(fullObject, prismContext);

                delta = delta.clone();                                      // to avoid changing original modifications
                delta.setParentPath(delta.getParentPath().rest(2));         // remove "case[id]" from the delta path
                delta.applyTo(aCase.asPrismContainerValue());

                // we need to generate IDs but we (currently) do not use that for setting "isTransient" flag
                PrismIdentifierGenerator generator = new PrismIdentifierGenerator(PrismIdentifierGenerator.Operation.MODIFY);
                generator.generate(aCase);

                RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(campaignOid, aCase, createRepositoryContext());

                // TODO this is wrong, but merge works afterwards (removes work items...)... remove this one, figure out how to do it properly
                em.find(RAccessCertificationCase.class, new RContainerId(rCase.getId(), rCase.getOwnerOid()));

                rCase = em.merge(rCase);

                LOGGER.trace("Access certification case {} merged", rCase);
                casesModified.add(aCase.getId());
            }
        }

        // refresh campaign cases, if requested
        if (RepoModifyOptions.isForceReindex(modifyOptions)) {
            //noinspection unchecked
            Query query = em.createNamedQuery("get.campaignCases");
            query.setParameter("ownerOid", campaignOid);
            List<Object> cases = query.getResultList();
            for (Object o : cases) {
                if (!(o instanceof byte[])) {
                    throw new IllegalStateException("Certification case: expected byte[], got " + o.getClass());
                }
                byte[] fullObject = (byte[]) o;
                AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(fullObject, prismContext);
                Long id = aCase.getId();
                if (id != null && casesAddedOrDeleted != null && !casesAddedOrDeleted.contains(id) && !casesModified.contains(id)) {
                    RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(campaignOid, aCase, createRepositoryContext());
                    rCase = em.merge(rCase);
                    LOGGER.trace("Access certification case {} refreshed", rCase);
                }
            }
        }
    }

    private long checkPathSanity(ItemPath deltaPath, List<Long> casesAddedOrDeleted) {
        Object secondSegment = deltaPath.getSegment(1);
        if (!ItemPath.isId(secondSegment)) {
            throw new IllegalStateException("Couldn't update cert campaign by delta with path " + deltaPath + " - should start with case[id]");
        }
        Long id = ItemPath.toId(secondSegment);
        if (id == null) {
            throw new IllegalStateException("Couldn't update cert campaign by delta with path " + deltaPath + " - should start with case[id]");
        }
        if (deltaPath.size() == 2) {        // not enough
            throw new IllegalStateException("Couldn't update cert campaign by delta with path " + deltaPath + " - should start with case[id] and contain additional path");
        }
        if (casesAddedOrDeleted == null || casesAddedOrDeleted.contains(id)) {
            throw new IllegalArgumentException("Couldn't update certification case that was added/deleted in this operation. Path=" + deltaPath);
        }
        return id;
    }

    // TODO find a better name
    public AccessCertificationCaseType updateLoadedCertificationCase(
            GetContainerableResult result, Map<String, PrismObject<AccessCertificationCampaignType>> ownersMap,
            Collection<SelectorOptions<GetOperationOptions>> options,
            EntityManager em, OperationResult operationResult) throws SchemaException {

        byte[] fullObject = result.getFullObject();
        AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(fullObject, prismContext);
        generalHelper.validateContainerable(aCase, AccessCertificationCaseType.class);

        String ownerOid = result.getOwnerOid();
        PrismObject<AccessCertificationCampaignType> campaign = resolveCampaign(ownerOid, ownersMap, em, operationResult);
        if (campaign != null && !campaign.asObjectable().getCase().contains(aCase)) {
            campaign.asObjectable().getCase().add(aCase);
        }
        objectRetriever.attachDiagDataIfRequested(aCase.asPrismContainerValue(), fullObject, options);
        return aCase;
    }

    public AccessCertificationWorkItemType updateLoadedCertificationWorkItem(GetCertificationWorkItemResult result,
            Map<String, PrismContainerValue<AccessCertificationCaseType>> casesCache,        // key=OID:ID
            Map<String, PrismObject<AccessCertificationCampaignType>> campaignsCache,        // key=OID
            Collection<SelectorOptions<GetOperationOptions>> options,
            QueryEngine engine, EntityManager em, OperationResult operationResult) throws SchemaException, QueryException {

        String campaignOid = result.getCampaignOid();
        Integer caseId = result.getCaseId();
        Integer workItemId = result.getId();
        String caseKey = campaignOid + ":" + caseId;
        PrismContainerValue<AccessCertificationCaseType> casePcv = casesCache.get(caseKey);
        if (casePcv == null) {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId(campaignOid)
                    .and().id(caseId)
                    .build();
            RQuery caseQuery = engine.interpret(query, AccessCertificationCaseType.class, null, false, em);
            List<GetContainerableResult> cases = caseQuery.list();
            if (cases.size() > 1) {
                throw new IllegalStateException(
                        "More than one certification case found for campaign " + campaignOid + ", ID " + caseId);
            } else if (cases.isEmpty()) {
                // we need it, because otherwise we have only identifiers for the work item, no data
                throw new IllegalStateException("No certification case found for campaign " + campaignOid + ", ID " + caseId);
            }
            // TODO really use options of 'null' ?
            AccessCertificationCaseType acase = updateLoadedCertificationCase(cases.get(0), campaignsCache, null, em, operationResult);
            casePcv = acase.asPrismContainerValue();
            casesCache.put(caseKey, casePcv);
        }
        @SuppressWarnings({ "raw", "unchecked" })
        PrismContainerValue<AccessCertificationWorkItemType> workItemPcv = (PrismContainerValue<AccessCertificationWorkItemType>)
                casePcv.find(ItemPath.create(AccessCertificationCaseType.F_WORK_ITEM, workItemId));
        if (workItemPcv == null) {
            throw new IllegalStateException("No work item " + workItemId + " in " + casePcv);
        } else {
            return workItemPcv.asContainerable();
        }
    }

    private PrismObject<AccessCertificationCampaignType> resolveCampaign(String campaignOid,
            Map<String, PrismObject<AccessCertificationCampaignType>> campaignsCache,
            EntityManager em, OperationResult operationResult) {
        PrismObject<AccessCertificationCampaignType> campaign = campaignsCache.get(campaignOid);
        if (campaign != null) {
            return campaign;
        }
        try {
            campaign = objectRetriever.getObjectInternal(em, AccessCertificationCampaignType.class, campaignOid, null, false);
        } catch (ObjectNotFoundException | SchemaException | DtoTranslationException | RuntimeException e) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get campaign with OID {}", e, campaignOid);
            return null;
        }
        campaignsCache.put(campaignOid, campaign);
        return campaign;
    }

    // adds cases to campaign if requested by options
    <T extends ObjectType> void updateLoadedCampaign(
            PrismObject<T> object, Collection<SelectorOptions<GetOperationOptions>> options, EntityManager em)
            throws SchemaException {
        if (!SelectorOptions.hasToFetchPathNotRetrievedByDefault(AccessCertificationCampaignType.F_CASE, options)) {
            return;
        }

        LOGGER.debug("Loading certification campaign cases.");

        TypedQuery<RAccessCertificationCase> query = em.createQuery(
                        "from RAccessCertificationCase c where c.ownerOid = :oid", RAccessCertificationCase.class)
                .setParameter("oid", object.getOid());

        // TODO fetch only XML representation
        List<RAccessCertificationCase> cases = query.getResultList();
        if (CollectionUtils.isNotEmpty(cases)) {
            AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) object.asObjectable();
            List<AccessCertificationCaseType> jaxbCases = campaign.getCase();
            for (RAccessCertificationCase rCase : cases) {
                AccessCertificationCaseType jaxbCase = rCase.toJAXB(prismContext);
                jaxbCases.add(jaxbCase);
            }
            PrismContainer<AccessCertificationCaseType> caseContainer = object.findContainer(AccessCertificationCampaignType.F_CASE);
            caseContainer.setIncomplete(false);
        } else {
            PrismContainer<AccessCertificationCaseType> caseContainer = object.findContainer(AccessCertificationCampaignType.F_CASE);
            if (caseContainer != null) {
                caseContainer.clear();      // just in case
                caseContainer.setIncomplete(false);
            }
        }
    }
}
