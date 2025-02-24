/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.lang.module.ModuleDescriptor;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@SuppressWarnings("unused")
public class DefaultCleanupListener implements CleanerListener {

    private static final Trace TRACE = TraceManager.getTrace(DefaultCleanupListener.class);

    private static final ModuleDescriptor.Version CONNECTOR_AVAILABLE_SUPPORT_VERSION =
            ModuleDescriptor.Version.parse("4.6");

    private static final ModuleDescriptor.Version REFERENCE_FILTER_SUPPORT_VERSION =
            ModuleDescriptor.Version.parse("4.4");

    private final PrismContext prismContext;

    private boolean warnAboutMissingReferences;

    public DefaultCleanupListener(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public boolean isWarnAboutMissingReferences() {
        return warnAboutMissingReferences;
    }

    public void setWarnAboutMissingReferences(boolean warnAboutMissingReferences) {
        this.warnAboutMissingReferences = warnAboutMissingReferences;
    }

    @Override
    public boolean onConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        event.result().getMessages().add(
                new CleanupItem<>(
                        CleanupItemType.OPTIONAL_CLEANUP,
                        new SingleLocalizableMessage(
                                "Optional item '" + event.path() + "' not cleaned up."),
                        event.path()));

        return false;
    }

    @Override
    public void onReferenceCleanup(CleanupEvent<PrismReference> event) {
        PrismReference ref = event.item();

        Class<?> type = null;
        if (event.source() instanceof PrismContainerValue val) {
            type = val.getCompileTimeClass();
        } else if (event.source() instanceof PrismContainer container) {
            type = container.getCompileTimeClass();
        }

        if (ResourceType.class.equals(type)
                && ResourceType.F_CONNECTOR_REF.equivalent(event.path())) {

            processConnectorRef(event);
            return;
        }

        if (ref.isEmpty()) {
            return;
        }

        ref.getValues().forEach(refValue -> processOtherRef(event, refValue));
    }

    private void processOtherRef(CleanupEvent<PrismReference> event, PrismReferenceValue refValue) {
        if (!isWarnAboutMissingReferences()) {
            return;
        }

        String oid = refValue.getOid();
        if (oid == null) {
            return;
        }

        QName typeName = refValue.getTargetType();
        if (typeName == null) {
            typeName = ObjectType.COMPLEX_TYPE;
        }

        ObjectTypes type = ObjectTypes.getObjectTypeFromTypeQName(typeName);
        boolean canResolve = canResolveLocalObject(type.getClassDefinition(), oid);
        if (canResolve) {
            return;
        }

        event.result().getMessages().add(
                new CleanupItem<>(
                        CleanupItemType.MISSING_REFERENCE,
                        new SingleLocalizableMessage(
                                "Unresolved reference (locally): " + refValue.getOid() + "(" + typeName.getLocalPart() + ")."),
                        new ObjectReferenceType()
                                .oid(oid)
                                .type(typeName)));
    }

    private void cleanupConnectorRef(PrismReferenceValue value) {
        value.setOid(null);
        value.setRelation(null);
        value.setTargetType(ConnectorType.COMPLEX_TYPE);
    }

    private void processConnectorRef(CleanupEvent<PrismReference> event) {
        PrismReference ref = event.item();
        if (ref.isEmpty()) {
            return;
        }

        PrismReferenceValue val = ref.getValue();
        if (val.isEmpty()) {
            return;
        }

        String oid = val.getOid();
        if (StringUtils.isEmpty(oid) || val.getFilter() != null) {
            cleanupConnectorRef(val);
            return;
        }

        ObjectReferenceType missingRef = new ObjectReferenceType()
                .oid(oid)
                .type(ConnectorType.COMPLEX_TYPE);

        try {
            PrismObject<ConnectorType> connector = resolveConnector(oid);
            if (connector == null) {
                event.result().getMessages().add(
                        new CleanupItem<>(
                                CleanupItemType.MISSING_REFERENCE,
                                new SingleLocalizableMessage(
                                        "Unresolved connector reference: Couldn't find connector with oid " + oid + "."),
                                missingRef));
                return;
            }

            ConnectorType connectorType = connector.asObjectable();

            SearchFilterType searchFilter = createSearchFilterType(ref, connectorType);
            if (searchFilter != null) {
                val.setFilter(searchFilter);
                cleanupConnectorRef(val);
            }
        } catch (Exception ex) {
            TRACE.debug("Couldn't resolve connector reference", ex);

            event.result().getMessages().add(
                    new CleanupItem<>(
                            CleanupItemType.MISSING_REFERENCE,
                            new SingleLocalizableMessage(
                                    "Unresolved connector reference: " + ex.getMessage()),
                            missingRef));
        }
    }

    private SearchFilterType createSearchFilterType(PrismReference ref, ConnectorType connectorType)
            throws PrismQuerySerialization.NotSupportedException, SchemaException {

        S_MatchingRuleEntry filterBuilder = prismContext.queryFor(ConnectorType.class)
                .item(ConnectorType.F_CONNECTOR_TYPE).eq(connectorType.getConnectorType())
                .and()
                .item(ConnectorType.F_CONNECTOR_VERSION).eq(connectorType.getConnectorVersion());

        if (shouldAddConnectorAvailable()) {
            filterBuilder = filterBuilder
                    .and()
                    .item(ConnectorType.F_AVAILABLE).eq(true);
        }

        ObjectFilter filter = filterBuilder.buildFilter();
        if (!shouldUseFilterText()) {
            return prismContext.getQueryConverter().createSearchFilterType(filter);
        }

        PrismNamespaceContext nsCtx = ref.getNamespaceContext();
        if (nsCtx == null) {
            nsCtx = PrismContext.get().getSchemaRegistry().staticNamespaceContext();
        }
        return prismContext.querySerializer()
                .serialize(filter, nsCtx, true)
                .toSearchFilterType();
    }

    private boolean shouldAddConnectorAvailable() {
        String current = getMidpointVersion();
        return current == null ||
                CONNECTOR_AVAILABLE_SUPPORT_VERSION.compareTo(ModuleDescriptor.Version.parse(current)) <= 0;
    }

    private boolean shouldUseFilterText() {
        String current = getMidpointVersion();
        return current == null ||
                REFERENCE_FILTER_SUPPORT_VERSION.compareTo(ModuleDescriptor.Version.parse(current)) <= 0;
    }

    /**
     * @return true if the object reference can be resolved, false otherwise. E.g. file/object is available locally in project.
     */
    protected <O extends ObjectType> boolean canResolveLocalObject(Class<O> type, String oid) {
        return false;
    }

    protected PrismObject<ConnectorType> resolveConnector(String oid) {
        return null;
    }

    protected String getMidpointVersion() {
        return null;
    }
}
