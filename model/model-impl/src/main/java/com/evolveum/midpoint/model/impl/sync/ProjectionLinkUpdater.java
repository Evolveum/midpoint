/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ShadowDeathEvent;
import com.evolveum.midpoint.provisioning.api.ShadowDeathListener;
import com.evolveum.midpoint.provisioning.api.ShadowLivenessState;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Updates focus -> shadow links (linkRef, to be renamed to projectionRef)
 * based on {@link ShadowDeathEvent} emitted by provisioning.
 */
@Component
public class ProjectionLinkUpdater implements ShadowDeathListener {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionLinkUpdater.class);

    private static final String OP_UPDATE_PROJECTION_LINK = ProjectionLinkUpdater.class.getName() + ".updateProjectionLink";

    @Autowired private PrismContext prismContext;
    @Autowired private EventDispatcher provisioningEventDispatcher;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @PostConstruct
    void postConstruct() {
        provisioningEventDispatcher.registerListener(this);
    }

    @PreDestroy
    void preDestroy() {
        provisioningEventDispatcher.unregisterListener(this);
    }

    @Override
    public void notify(ShadowDeathEvent event, Task task, OperationResult result) {
        updateProjectionLink(event, result);
    }

    private void updateProjectionLink(ShadowDeathEvent event, OperationResult parentResult) {
        LOGGER.trace("Updating projection link: {}", event);
        OperationResult result = parentResult.subresult(OP_UPDATE_PROJECTION_LINK)
                .addArbitraryObjectAsParam("event", event)
                .setMinor()
                .build();
        try {
            String shadowOid = event.getOid();
            ShadowLivenessState newState = event.getNewState();
            List<PrismObject<FocusType>> owners = findShadowOwners(shadowOid, result);
            LOGGER.trace("Found {} owner(s)", owners.size());
            for (PrismObject<FocusType> owner : owners) {
                FocusType ownerBean = owner.asObjectable();
                Class<? extends FocusType> ownerType = ownerBean.getClass();
                List<ObjectReferenceType> currentReferences = ownerBean.getLinkRef();

                Collection<ObjectReferenceType> referencesToAdd = new ArrayList<>();
                Collection<ObjectReferenceType> referencesToDelete = new ArrayList<>();

                if (newState == ShadowLivenessState.DELETED) {
                    currentReferences.stream()
                            .filter(ref -> shadowOid.equals(ref.getOid()))
                            .map(ObjectReferenceType::clone)
                            .forEach(referencesToDelete::add);
                } else if (newState == ShadowLivenessState.DEAD) {
                    currentReferences.stream()
                            .filter(ref -> shadowOid.equals(ref.getOid()))
                            .filter(ShadowUtil::isNotDead)
                            .forEach(ref -> {
                                referencesToDelete.add(ref.clone());
                                referencesToAdd.add(ref.clone().relation(SchemaConstants.ORG_RELATED));
                            });
                } else {
                    throw new IllegalStateException("Unexpected new shadow liveness state: " + newState);
                }

                if (!referencesToAdd.isEmpty() || !referencesToDelete.isEmpty()) {
                    Collection<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ownerType)
                            .item(FocusType.F_LINK_REF)
                            .deleteRealValues(referencesToDelete)
                            .addRealValues(referencesToAdd)
                            .asItemDeltas();
                    repositoryService.modifyObject(ownerType, owner.getOid(), modifications, result);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("LINK UPDATE: Error while updating projection links. Event: {}", event, t);
            result.recordFatalError("Error while updating projection links on shadow death " + t.getMessage(), t);
            //nothing more to do. and we don't want to throw exception to not cancel the whole execution.
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Currently should return 0 or 1 owner. But for robustness let us update really all owners.
     */
    private List<PrismObject<FocusType>> findShadowOwners(String shadowOid, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF).ref(shadowOid, null, PrismConstants.Q_ANY)
                .build();
        return repositoryService.searchObjects(FocusType.class, query, readOnly(), result);
    }

    @Override
    public String getName() {
        return "projection link updater";
    }
}
