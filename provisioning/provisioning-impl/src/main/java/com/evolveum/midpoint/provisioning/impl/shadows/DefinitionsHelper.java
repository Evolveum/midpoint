/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.processor.ShadowCoordinatesQualifiedObjectDelta;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implements various methods of `applyDefinition` kind.
 */
@Experimental
@Component
class DefinitionsHelper {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;

    public void applyDefinition(ObjectDelta<ShadowType> delta, @Nullable ShadowType repoShadow,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> shadow = null;
        ResourceShadowCoordinates coordinates = null;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd();
        } else if (delta.isModify()) {
            if (delta instanceof ShadowCoordinatesQualifiedObjectDelta) {
                // This one does not have OID, it has to be specially processed
                coordinates = ((ShadowCoordinatesQualifiedObjectDelta<?>) delta).getCoordinates();
            } else {
                String shadowOid = delta.getOid();
                if (shadowOid == null) {
                    if (repoShadow == null) {
                        throw new IllegalArgumentException("No OID in object delta " + delta
                                + " and no externally-supplied shadow is present as well.");
                    }
                    shadow = repoShadow.asPrismObject();
                } else {
                    // TODO consider fetching only when really necessary
                    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null, result);
                }
            }
        } else {
            // Delete delta, nothing to do at all
            return;
        }
        ProvisioningContext ctx;
        if (shadow == null) {
            stateCheck(coordinates != null, "No shadow nor coordinates");
            ctx = ctxFactory.createForCoordinates(coordinates, task, result);
        } else {
            ctx = ctxFactory.createForShadow(shadow, task, result);
        }
        shadowCaretaker.applyAttributesDefinition(ctx, delta);
    }

    public void applyDefinition(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, parentResult);
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
    }

    public void applyDefinition(ObjectQuery query, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.createForCoordinates(
                ObjectQueryUtil.getCoordinates(query.getFilter()),
                task, result);
        applyDefinition(ctx, query);
    }

    void applyDefinition(ProvisioningContext ctx, ObjectQuery query)
            throws SchemaException {
        if (query == null) {
            return;
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            return;
        }
        com.evolveum.midpoint.prism.query.Visitor visitor = subFilter -> {
            if (subFilter instanceof PropertyValueFilter) {
                PropertyValueFilter<?> valueFilter = (PropertyValueFilter<?>) subFilter;
                ItemDefinition<?> definition = valueFilter.getDefinition();
                if (definition instanceof ResourceAttributeDefinition) {
                    return; // already has a resource-related definition
                }
                if (!ShadowType.F_ATTRIBUTES.equivalent(valueFilter.getParentPath())) {
                    return;
                }
                QName attributeName = valueFilter.getElementName();
                ResourceAttributeDefinition<?> attributeDefinition =
                        ctx.getObjectDefinitionRequired().findAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new TunnelException(
                            new SchemaException("No definition for attribute " + attributeName + " in query " + query));
                }
                //noinspection unchecked,rawtypes
                valueFilter.setDefinition((ResourceAttributeDefinition) attributeDefinition);
            }
        };
        try {
            filter.accept(visitor);
        } catch (TunnelException te) {
            throw (SchemaException) te.getCause();
        }
    }
}
