/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RCertCaseReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RCertWorkItemReference;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetContainerableResult;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Contains methods specific to handle certification cases.
 * (As these cases are stored outside main certification campaign object.)
 *
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 *
 * @author mederly
 */
@Component
public class CertificationCaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationCaseHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private GeneralHelper generalHelper;
    @Autowired private NameResolutionHelper nameResolutionHelper;
    @Autowired private ObjectRetriever objectRetriever;
    @Autowired private RepositoryService repositoryService;

    public void addCertificationCampaignCases(Session session, RObject object, boolean deleteBeforeAdd) {
        if (!(object instanceof RAccessCertificationCampaign)) {
            return;
        }
        RAccessCertificationCampaign campaign = (RAccessCertificationCampaign) object;

        if (deleteBeforeAdd) {
            LOGGER.trace("Deleting existing cases for {}", campaign.getOid());
            deleteCertificationCampaignCases(session, campaign.getOid());
        }
        if (campaign.getCase() != null) {
            for (RAccessCertificationCase aCase : campaign.getCase()) {
                session.save(aCase);
            }
        }
    }

    public void addCertificationCampaignCases(Session session, String campaignOid, Collection<PrismContainerValue> values, int currentId, List<Long> affectedIds) throws DtoTranslationException {
        for (PrismContainerValue value : values) {
            AccessCertificationCaseType caseType = new AccessCertificationCaseType();
            caseType.setupContainerValue(value);
            caseType.setCampaignRef(ObjectTypeUtil.createObjectRef(campaignOid, ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN));
            if (caseType.getId() == null) {
                caseType.setId((long) currentId);
                currentId++;
            }

            // we need to generate IDs but we (currently) do not use that for setting "isTransient" flag
            PrismIdentifierGenerator generator = new PrismIdentifierGenerator();
            generator.generate(caseType, PrismIdentifierGenerator.Operation.MODIFY);

            RAccessCertificationCase row = RAccessCertificationCase.toRepo(campaignOid, caseType, createRepositoryContext());
            row.setId(RUtil.toInteger(caseType.getId()));
            affectedIds.add(caseType.getId());
            session.save(row);
        }
    }

    @NotNull
    private RepositoryContext createRepositoryContext() {
        return new RepositoryContext(repositoryService, prismContext);
    }

    public void deleteCertificationCampaignCases(Session session, String oid) {
        // TODO couldn't this cascading be done by hibernate itself?
        Query deleteDecisions = session.getNamedQuery("delete.campaignCasesDecisions");
        deleteDecisions.setParameter("oid", oid);
        deleteDecisions.executeUpdate();

        Query deleteReferences = session.getNamedQuery("delete.campaignCasesReferences");
        deleteReferences.setParameter("oid", oid);
        deleteReferences.executeUpdate();

        Query deleteWorkItemReferences = session.getNamedQuery("delete.campaignCasesWorkItemReferences");
        deleteWorkItemReferences.setParameter("oid", oid);
        deleteWorkItemReferences.executeUpdate();

        Query deleteWorkItems = session.getNamedQuery("delete.campaignCasesWorkItems");
        deleteWorkItems.setParameter("oid", oid);
        deleteWorkItems.executeUpdate();

        Query deleteCases = session.getNamedQuery("delete.campaignCases");
        deleteCases.setParameter("oid", oid);
        deleteCases.executeUpdate();
    }

    public <T extends ObjectType> Collection<? extends ItemDelta> filterCampaignCaseModifications(Class<T> type,
                                                                                                  Collection<? extends ItemDelta> modifications) {
        Collection<ItemDelta> caseDelta = new ArrayList<>();
        if (!AccessCertificationCampaignType.class.equals(type)) {
            return caseDelta;
        }

        ItemPath casePath = new ItemPath(AccessCertificationCampaignType.F_CASE);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Certification campaign cannot be modified via empty-path modification");
            } else if (path.equivalent(casePath)) {
                caseDelta.add(delta);
            } else if (path.isSuperPath(casePath)) {        // like case[id]/xxx
                caseDelta.add(delta);
            }
        }

        modifications.removeAll(caseDelta);

        return caseDelta;
    }

    public <T extends ObjectType> void updateCampaignCases(Session session, String campaignOid,
			Collection<? extends ItemDelta> modifications, RepoModifyOptions modifyOptions) throws SchemaException, ObjectNotFoundException, DtoTranslationException {
        if (modifications.isEmpty() && !RepoModifyOptions.isExecuteIfNoChanges(modifyOptions)) {
            return;
        }

        List<Long> casesAddedOrDeleted = addOrDeleteCases(session, campaignOid, modifications);
        LOGGER.trace("Cases added/deleted (null means REPLACE operation) = {}", casesAddedOrDeleted);

        updateCasesContent(session, campaignOid, modifications, casesAddedOrDeleted, modifyOptions);
    }

    protected List<Long> addOrDeleteCases(Session session, String campaignOid, Collection<? extends ItemDelta> modifications) throws SchemaException, DtoTranslationException {
        final ItemPath casePath = new ItemPath(AccessCertificationCampaignType.F_CASE);
        boolean replacePresent = false;
        List<Long> affectedIds = new ArrayList<>();
        for (ItemDelta delta : modifications) {
            ItemPath deltaPath = delta.getPath();
            if (!casePath.isSubPathOrEquivalent(deltaPath)) {
                throw new IllegalStateException("Wrong campaign delta sneaked into updateCampaignCases: class=" + delta.getClass() + ", path=" + deltaPath);
            }

            if (deltaPath.size() == 1) {
                if (delta.getValuesToDelete() != null) {
                    // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                    for (PrismContainerValue value : (Collection<PrismContainerValue>) delta.getValuesToDelete()) {
                        Long id = value.getId();
                        if (id == null) {
                            throw new SchemaException("Couldn't delete certification case with null id");
                        }
                        affectedIds.add(id);
                        // TODO couldn't this cascading be done by hibernate itself?
                        Integer integerCaseId = RUtil.toInteger(id);
                        Query deleteCaseDecisions = session.getNamedQuery("delete.campaignCaseDecisions");
                        deleteCaseDecisions.setString("oid", campaignOid);
                        deleteCaseDecisions.setInteger("id", integerCaseId);
                        deleteCaseDecisions.executeUpdate();
                        Query deleteCaseReferences = session.createSQLQuery("delete from " + RCertCaseReference.TABLE +
                                " where owner_owner_oid=:oid and owner_id=:id");
                        deleteCaseReferences.setString("oid", campaignOid);
                        deleteCaseReferences.setInteger("id", integerCaseId);
                        deleteCaseReferences.executeUpdate();
                        Query deleteWorkItemReferences = session.createSQLQuery("delete from " + RCertWorkItemReference.TABLE +
                                " where owner_owner_owner_oid=:oid and owner_owner_id=:id");
                        deleteWorkItemReferences.setString("oid", campaignOid);
                        deleteWorkItemReferences.setInteger("id", integerCaseId);
                        deleteWorkItemReferences.executeUpdate();
                        Query deleteCaseWorkItems = session.createSQLQuery("delete from " + RAccessCertificationWorkItem.TABLE +
                                " where owner_owner_oid=:oid and owner_id=:id");
                        deleteCaseWorkItems.setString("oid", campaignOid);
                        deleteCaseWorkItems.setInteger("id", integerCaseId);
                        deleteCaseWorkItems.executeUpdate();
                        Query deleteCase = session.getNamedQuery("delete.campaignCase");
                        deleteCase.setString("oid", campaignOid);
                        deleteCase.setInteger("id", integerCaseId);
                        deleteCase.executeUpdate();
                    }
                }
                // TODO generated IDs might conflict with client-provided ones
                // also, client-provided IDs might conflict with those that are already in the database
                // So it's safest not to provide any IDs by the client
                if (delta.getValuesToAdd() != null) {
                    int currentId = generalHelper.findLastIdInRepo(session, campaignOid, "get.campaignCaseLastId") + 1;
                    addCertificationCampaignCases(session, campaignOid, delta.getValuesToAdd(), currentId, affectedIds);
                }
                if (delta.getValuesToReplace() != null) {
                    deleteCertificationCampaignCases(session, campaignOid);
                    addCertificationCampaignCases(session, campaignOid, delta.getValuesToReplace(), 1, affectedIds);
                    replacePresent = true;
                }

            }
        }
        return replacePresent ? null : affectedIds;
    }

    private void updateCasesContent(Session session, String campaignOid, Collection<? extends ItemDelta> modifications,
			List<Long> casesAddedOrDeleted, RepoModifyOptions modifyOptions) throws SchemaException, ObjectNotFoundException, DtoTranslationException {
		Set<Long> casesModified = new HashSet<>();
        for (ItemDelta delta : modifications) {
            ItemPath deltaPath = delta.getPath();
            if (deltaPath.size() > 1) {
                LOGGER.trace("Updating campaign " + campaignOid + " with delta " + delta);

                // should start with "case[id]"
                long id = checkPathSanity(deltaPath, casesAddedOrDeleted);

                Query query = session.getNamedQuery("get.campaignCase");
                query.setString("ownerOid", campaignOid);
                query.setInteger("id", (int) id);

                byte[] fullObject = (byte[]) query.uniqueResult();
                if (fullObject == null) {
                    throw new ObjectNotFoundException("Couldn't update cert campaign " + campaignOid + " + by delta with path " + deltaPath + " - specified case does not exist");
                }
                AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(fullObject, prismContext, false);

                delta = delta.clone();                                      // to avoid changing original modifications
                delta.setParentPath(delta.getParentPath().tail(2));         // remove "case[id]" from the delta path
                delta.applyTo(aCase.asPrismContainerValue());

                // we need to generate IDs but we (currently) do not use that for setting "isTransient" flag
                PrismIdentifierGenerator generator = new PrismIdentifierGenerator();
                generator.generate(aCase, PrismIdentifierGenerator.Operation.MODIFY);

                RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(campaignOid, aCase, createRepositoryContext());
                session.merge(rCase);

                LOGGER.trace("Access certification case {} merged", rCase);
				casesModified.add(aCase.getId());
            }
        }

		// refresh campaign cases, if requested
		if (RepoModifyOptions.isExecuteIfNoChanges(modifyOptions)) {
			Query query = session.getNamedQuery("get.campaignCases");
			query.setString("ownerOid", campaignOid);
			List<Object> cases = query.list();
			for (Object o : cases) {
				if (!(o instanceof byte[])) {
					throw new IllegalStateException("Certification case: expected byte[], got " + o.getClass());
				}
				byte[] fullObject = (byte[]) o;
				AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(fullObject, prismContext, false);
				Long id = aCase.getId();
				if (id != null && casesAddedOrDeleted != null && !casesAddedOrDeleted.contains(id) && !casesModified.contains(id)) {
					RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(campaignOid, aCase, createRepositoryContext());
					session.merge(rCase);
					LOGGER.trace("Access certification case {} refreshed", rCase);
				}
			}
		}
    }

    private long checkPathSanity(ItemPath deltaPath, List<Long> casesAddedOrDeleted) {
        ItemPathSegment secondSegment = deltaPath.getSegments().get(1);
        if (!(secondSegment instanceof IdItemPathSegment)) {
            throw new IllegalStateException("Couldn't update cert campaign by delta with path " + deltaPath + " - should start with case[id]");
        }
        Long id = ((IdItemPathSegment) secondSegment).getId();
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
    public AccessCertificationCaseType updateLoadedCertificationCase(GetContainerableResult result, Map<String, PrismObject> ownersMap,
			Collection<SelectorOptions<GetOperationOptions>> options,
			Session session, OperationResult operationResult) throws SchemaException {

        AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(result.getFullObject(), prismContext, false);
        nameResolutionHelper.resolveNamesIfRequested(session, aCase.asPrismContainerValue(), options);
        generalHelper.validateContainerable(aCase, AccessCertificationCaseType.class);

        String ownerOid = result.getOwnerOid();
        PrismObject<AccessCertificationCampaignType> campaign = resolveCampaign(ownerOid, ownersMap, session, operationResult);
        if (campaign != null) {
            campaign.asObjectable().getCase().add(aCase);
        }
        return aCase;
    }

    private PrismObject<AccessCertificationCampaignType> resolveCampaign(String campaignOid, Map<String, PrismObject> campaignsCache, Session session,
			OperationResult operationResult) {
        PrismObject campaign = campaignsCache.get(campaignOid);
        if (campaign != null) {
            return campaign;
        }
        try {
            campaign = objectRetriever.getObjectInternal(session, AccessCertificationCampaignType.class, campaignOid, null, false, operationResult);
        } catch (ObjectNotFoundException|SchemaException|DtoTranslationException|RuntimeException e) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get campaign with OID {}", e, campaignOid);
            return null;
        }
        campaignsCache.put(campaignOid, campaign);
        return campaign;
    }

    // adds cases to campaign if requested by options
    public <T extends ObjectType> void updateLoadedCampaign(PrismObject<T> object,
                                                            Collection<SelectorOptions<GetOperationOptions>> options,
                                                            Session session) throws SchemaException {
        if (!SelectorOptions.hasToLoadPath(AccessCertificationCampaignType.F_CASE, options)) {
            return;
        }

        LOGGER.debug("Loading certification campaign cases.");

        Criteria criteria = session.createCriteria(RAccessCertificationCase.class);
        criteria.add(Restrictions.eq("ownerOid", object.getOid()));

        // TODO fetch only XML representation
        List<RAccessCertificationCase> cases = criteria.list();
        if (cases == null || cases.isEmpty()) {
            return;
        }

        AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) object.asObjectable();
        List<AccessCertificationCaseType> jaxbCases = campaign.getCase();
        for (RAccessCertificationCase rCase : cases) {
            AccessCertificationCaseType jaxbCase = rCase.toJAXB(prismContext);
            jaxbCases.add(jaxbCase);
        }
    }

}
