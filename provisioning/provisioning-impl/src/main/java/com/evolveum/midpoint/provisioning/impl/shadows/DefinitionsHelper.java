/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
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

    @Autowired private PrismContext prismContext;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;

    public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType repoShadow,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> shadow = null;
        ResourceShadowDiscriminator discriminator = null;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd();
        } else if (delta.isModify()) {
            if (delta instanceof ShadowDiscriminatorObjectDelta) {
                // This one does not have OID, it has to be specially processed
                discriminator = ((ShadowDiscriminatorObjectDelta) delta).getDiscriminator();
            } else {
                String shadowOid = delta.getOid();
                if (shadowOid == null) {
                    if (repoShadow == null) {
                        throw new IllegalArgumentException("No OID in object delta " + delta
                                + " and no externally-supplied shadow is present as well.");
                    }
                    shadow = repoShadow.asPrismObject();
                } else {
                    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null,
                            parentResult); // TODO consider fetching only when really necessary
                }
            }
        } else {
            // Delete delta, nothing to do at all
            return;
        }
        ProvisioningContext ctx;
        if (shadow == null) {
            ctx = ctxFactory.create(discriminator, null, parentResult);
            ctx.assertDefinition();
        } else {
            ctx = ctxFactory.create(shadow, null, parentResult);
            ctx.assertDefinition();
        }
        shadowCaretaker.applyAttributesDefinition(ctx, delta);
    }

    public void applyDefinition(PrismObject<ShadowType> shadow, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
        ctx.assertDefinition();
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
    }

    public void applyDefinition(final ObjectQuery query, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter(), prismContext);
        ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
        ctx.assertDefinition();
        applyDefinition(ctx, query);
    }

    void applyDefinition(final ProvisioningContext ctx, final ObjectQuery query)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (query == null) {
            return;
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            return;
        }
        final RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        com.evolveum.midpoint.prism.query.Visitor visitor = subfilter -> {
            if (subfilter instanceof PropertyValueFilter) {
                PropertyValueFilter<?> valueFilter = (PropertyValueFilter<?>) subfilter;
                ItemDefinition definition = valueFilter.getDefinition();
                if (definition instanceof ResourceAttributeDefinition) {
                    return;        // already has a resource-related definition
                }
                if (!ShadowType.F_ATTRIBUTES.equivalent(valueFilter.getParentPath())) {
                    return;
                }
                QName attributeName = valueFilter.getElementName();
                ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new TunnelException(new SchemaException("No definition for attribute "
                            + attributeName + " in query " + query));
                }
                valueFilter.setDefinition(attributeDefinition);
            }
        };
        try {
            filter.accept(visitor);
        } catch (TunnelException te) {
            throw (SchemaException) te.getCause();
        }
    }
}
