/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.metadata;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.impl.metadata.ValueMetadataAdapter;
import com.evolveum.midpoint.prism.metadata.ValueMetadataMockUpFactory;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * TEMPORARY. WILL BE REMOVED AFTER PROTOTYPING IS OVER.
 */
@Experimental
public class MidpointValueMetadataMockUpFactory implements ValueMetadataMockUpFactory {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointValueMetadataMockUpFactory.class);

    private static final String NS_MOCKUP = "http://midpoint.evolveum.com/xml/ns/public/common/extension-metadata-mockup-3";
    private static final ItemName ATTACHED_VALUE_METADATA_NAME = new ItemName(NS_MOCKUP, "attachedValueMetadata");

    private static final ItemName ATTACHED_PATH = new ItemName(NS_MOCKUP, "path");
    private static final ItemName ATTACHED_VALUE = new ItemName(NS_MOCKUP, "value");
    private static final ItemName ATTACHED_METADATA = new ItemName(NS_MOCKUP, "metadata");
    private static final ItemName ATTACHED_SKIP_LEGACY_METADATA = new ItemName(NS_MOCKUP, "skipLegacyMetadata");

    @NotNull private final PrismContext prismContext;

    public MidpointValueMetadataMockUpFactory(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public Optional<ValueMetadata> createValueMetadata(@NotNull PrismValue value) throws SchemaException {
        PrismObject<?> object = getOwningObject(value);
        if (object != null) {
            ValueMetadataType metadata = createValueMetadata(object, value);
            return metadata != null ?
                    Optional.of(ValueMetadataAdapter.holding(metadata.asPrismContainerValue())) : Optional.empty();
        } else {
            LOGGER.warn("No owning object for {}, no metadata can be provided", value);
            return Optional.empty();
        }
    }

    private PrismObject<?> getOwningObject(PrismValue value) {
        Itemable parent = value.getParent();
        if (parent instanceof PrismObject) {
            return ((PrismObject<?>) parent);
        } else if (parent instanceof Item) {
            return getOwningObject(((Item<?, ?>) parent));
        } else {
            return null;
        }
    }

    private PrismObject<?> getOwningObject(Item<?, ?> item) {
        PrismContainerValue<?> parent = item.getParent();
        if (parent != null) {
            return getOwningObject(parent);
        } else {
            return null;
        }
    }

    private ValueMetadataType createValueMetadata(PrismObject<?> object, PrismValue value) throws SchemaException {
        MetadataSources sources = findMetadataSources(object, value);
        if (sources.attached == null && sources.legacy == null) {
            return null;
        } else {
            ValueMetadataType aggregated;
            if (sources.attached != null) {
                aggregated = sources.attached.clone();
            } else {
                aggregated = new ValueMetadataType(prismContext);
            }

            if (sources.legacy != null) {
                implantLegacy(aggregated, sources.legacy);
            }
            return aggregated;
        }
    }

    private void implantLegacy(ValueMetadataType aggregated, MetadataType legacy) throws SchemaException {
        implant(aggregated, legacy, MetadataType.F_CREATE_TIMESTAMP, ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATE_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_CREATOR_REF, ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATOR_REF);
        implant(aggregated, legacy, MetadataType.F_CREATE_CHANNEL, ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATE_CHANNEL);
        implant(aggregated, legacy, MetadataType.F_CREATE_TASK_REF, ValueMetadataType.F_STORAGE, StorageMetadataType.F_CREATE_TASK_REF);

        implant(aggregated, legacy, MetadataType.F_MODIFY_TIMESTAMP, ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFY_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_MODIFIER_REF, ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFIER_REF);
        implant(aggregated, legacy, MetadataType.F_MODIFY_CHANNEL, ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFY_CHANNEL);
        implant(aggregated, legacy, MetadataType.F_MODIFY_TASK_REF, ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFY_TASK_REF);

        implant(aggregated, legacy, MetadataType.F_REQUEST_TIMESTAMP, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_REQUEST_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_REQUESTOR_REF, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_REQUESTOR_REF);
        implant(aggregated, legacy, MetadataType.F_REQUESTOR_COMMENT, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_REQUESTOR_COMMENT);
        implant(aggregated, legacy, MetadataType.F_CREATE_APPROVER_REF, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CREATE_APPROVER_REF);
        implant(aggregated, legacy, MetadataType.F_CREATE_APPROVAL_COMMENT, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CREATE_APPROVAL_COMMENT);
        implant(aggregated, legacy, MetadataType.F_CREATE_APPROVAL_TIMESTAMP, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CREATE_APPROVAL_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_MODIFY_APPROVER_REF, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_MODIFY_APPROVER_REF);
        implant(aggregated, legacy, MetadataType.F_MODIFY_APPROVAL_COMMENT, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_MODIFY_APPROVAL_COMMENT);
        implant(aggregated, legacy, MetadataType.F_MODIFY_APPROVAL_TIMESTAMP, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_MODIFY_APPROVAL_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_CERTIFICATION_FINISHED_TIMESTAMP, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CERTIFICATION_FINISHED_TIMESTAMP);
        implant(aggregated, legacy, MetadataType.F_CERTIFICATION_OUTCOME, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CERTIFICATION_OUTCOME);
        implant(aggregated, legacy, MetadataType.F_CERTIFIER_REF, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CERTIFIER_REF);
        implant(aggregated, legacy, MetadataType.F_CERTIFIER_COMMENT, ValueMetadataType.F_PROCESS, ProcessMetadataType.F_CERTIFIER_COMMENT);

        implant(aggregated, legacy, MetadataType.F_LAST_PROVISIONING_TIMESTAMP, ValueMetadataType.F_PROVISIONING, ProvisioningMetadataType.F_LAST_PROVISIONING_TIMESTAMP);

        // origin mapping name is ignored
    }

    private void implant(ValueMetadataType aggregated, MetadataType legacy, ItemName legacyName, ItemName... aggregatePathSegments)
            throws SchemaException {
        Item<?, ?> legacyItem = legacy.asPrismContainerValue().findItem(legacyName);
        if (legacyItem != null) {
            ItemDelta<?, ?> delta = prismContext.deltaFor(ValueMetadataType.class)
                    .item(aggregatePathSegments)
                    .replace(CloneUtil.cloneCollectionMembers(legacyItem.getValues()))
                    .asItemDelta();
            delta.applyTo(aggregated.asPrismContainerValue());
        }
    }

    private static class MetadataSources {
        private final ValueMetadataType attached;
        private final MetadataType legacy;

        private MetadataSources(ValueMetadataType attached, MetadataType legacy) {
            this.attached = attached;
            this.legacy = legacy;
        }
    }

    private MetadataSources findMetadataSources(PrismObject<?> object, PrismValue value) {
        ItemPath path = value.getPath(); // will succeed as we know we can step up to the root object
        System.out.println("Deriving value metadata for " + value + " in " + object + " (path = " + path + ")");

        ValueMetadataType attached;
        PrismContainerValue<?> attachedInfo = findAttachedMetadata(object, path, value);
        boolean skipLegacy;
        if (attachedInfo != null) {
            PrismContainer<ValueMetadataType> attachedMetadataContainer = attachedInfo.findContainer(ATTACHED_METADATA);
            attached = attachedMetadataContainer != null ? attachedMetadataContainer.getRealValue() : null;
            skipLegacy = Boolean.TRUE.equals(attachedInfo.getPropertyRealValue(ATTACHED_SKIP_LEGACY_METADATA, Boolean.class));
        } else {
            attached = null;
            skipLegacy = false;
        }

        MetadataType legacy;
        if (!skipLegacy && value instanceof PrismContainerValue) {
            PrismContainer<MetadataType> legacyMetadataContainer =
                    ((PrismContainerValue<?>) value).findContainer(ObjectType.F_METADATA);
            legacy = legacyMetadataContainer != null ? legacyMetadataContainer.getRealValue() : null;
        } else {
            legacy = null;
        }

        return new MetadataSources(attached, legacy);
    }

    // returns AttachedValueMetadataType or null
    private PrismContainerValue<?> findAttachedMetadata(PrismObject<?> object, ItemPath path, PrismValue value) {
        PrismContainer<?> allAttached = object.findExtensionItem(ATTACHED_VALUE_METADATA_NAME);
        if (allAttached != null) {
            for (PrismContainerValue<?> attached : allAttached.getValues()) {
                if (matches(attached, path, value)) {
                    return attached;
                }
            }
        }
        return null;
    }

    private boolean matches(PrismContainerValue<?> attached, ItemPath path, PrismValue value) {
        ItemPath attachedPath = getAttachedPath(attached);
        if (path.equivalent(attachedPath)) {
            PrismProperty<?> attachedValueProperty = attached.findProperty(ATTACHED_VALUE);
            return attachedValueProperty == null || attachedValueProperty.isEmpty()
                    || valueMatches(attachedValueProperty.getValue(), value);
        } else {
            return false;
        }
    }

    // Temporary implementation, expecting attached is always a String
    private boolean valueMatches(PrismPropertyValue<?> attached, PrismValue real) {
        String expected = (String) attached.getRealValue();
        return real.getRealValue() != null && real.getRealValue().toString().equals(expected);
    }

    @NotNull
    private ItemPath getAttachedPath(PrismContainerValue<?> attached) {
        PrismProperty<ItemPathType> pathProperty = attached.findProperty(ATTACHED_PATH);
        if (pathProperty != null) {
            ItemPathType attachedPathValue = pathProperty.getRealValue();
            if (attachedPathValue != null) {
                return attachedPathValue.getItemPath();
            } else {
                return ItemPath.EMPTY_PATH;
            }
        } else {
            return ItemPath.EMPTY_PATH;
        }
    }
}
