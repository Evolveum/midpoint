package com.evolveum.midpoint.ninja.action.worker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

import org.apache.commons.lang3.ArrayUtils;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class ExportConfigurationWorker extends ExportConsumerWorker {

    public ExportConfigurationWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void editObject(PrismObject<? extends ObjectType> prismObject) {

        prismObject.getValue().removeOperationalItems();

        // removal of the connector configuration (two places)
        remove(prismObject, ResourceType.class, ResourceType.F_CONNECTOR_CONFIGURATION);
        remove(prismObject, ResourceType.class, ResourceType.F_ADDITIONAL_CONNECTOR,
                ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION);

        // removal of other items
        remove(prismObject, ResourceType.class, ResourceType.F_OPERATIONAL_STATE); // not marked operational in 4.8
        remove(prismObject, ResourceType.class, ResourceType.F_OPERATIONAL_STATE_HISTORY); // not marked operational in 4.8
        remove(prismObject, AssignmentHolderType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF);
        remove(prismObject, AbstractRoleType.class, AbstractRoleType.F_ADMIN_GUI_CONFIGURATION);
        remove(prismObject, ArchetypeType.class, ArchetypeType.F_ARCHETYPE_POLICY, ArchetypePolicyType.F_DISPLAY);
        remove(prismObject, ArchetypeType.class, ArchetypeType.F_ARCHETYPE_POLICY, ArchetypePolicyType.F_ITEM_CONSTRAINT);
        remove(prismObject, ArchetypeType.class, ArchetypeType.F_ARCHETYPE_POLICY, ArchetypePolicyType.F_ADMIN_GUI_CONFIGURATION);
        remove(prismObject, ObjectType.class, ObjectType.F_EXTENSION);
        remove(prismObject, ObjectType.class, ObjectType.F_METADATA);
        remove(prismObject, ObjectType.class, ObjectType.F_OPERATION_EXECUTION);

        removeAllExcept(prismObject,
                SystemConfigurationType.class,
                SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                SystemConfigurationType.F_MODEL_HOOKS,
                SystemConfigurationType.F_CORRELATION);

        removeAllExcept(prismObject,
                ConnectorType.class,
                ConnectorType.F_NAME,
                ConnectorType.F_AVAILABLE);

        // Remove items specified by the user.
        final Collection<ItemPath> explicitlyExcludedItems = options.getExcludeItems();
        explicitlyExcludedItems.forEach(path -> removeItem(prismObject, path));

        // Remove items that contains ProtectedDataType values.
        final Collection<ItemPath> sensitiveItems = new ArrayList<>();
        prismObject.accept(sensitivePropertiesCollector(sensitiveItems::add));
        sensitiveItems.forEach(path -> removeItem(prismObject, path));

        // Mask other ProtectedDataType values (it is quite hard to remove them, because they are not "normal"
        // properties)
        //noinspection unchecked
        prismObject.accept(new SensitiveDataRemovingVisitor<>(context.getLog()));
    }

    /** If object is of given type, we keep only the specified root-level items. */
    private static void removeAllExcept(
            PrismObject<? extends ObjectType> prismObject, Class<? extends ObjectType> type, ItemName... rootItemNames) {
        if (prismObject.isOfType(type)) {
            var rootItemNamesToKeep = List.of(rootItemNames);
            for (QName rootItemName : prismObject.getValue().getItemNames()) {
                if (!QNameUtil.matchAny(rootItemName, rootItemNamesToKeep)) {
                    removeItem(prismObject, ItemName.fromQName(rootItemName));
                }
            }
        }
    }

    /** If object is of given type, we remove item with given path (specified as path segments). */
    private static void remove(
            PrismObject<? extends ObjectType> prismObject, Class<? extends ObjectType> type, ItemName... pathSegments) {
        if (prismObject.isOfType(type)) {
            removeItem(prismObject, ItemPath.create(List.of(pathSegments)));
        }
    }

    private static void removeItem(PrismObject<?> object, ItemPath path) {
        removeItem(object.getValue(), path);
    }

    /**
     * Removes all items matching given (name-only) path.
     *
     * Similar to {@link PrismContainerValue#removeItems(List)} but deals not only with a single value, but
     * can treat multi-valued intermediate containers.
     *
     * Similar to PrismContainerValue.removePaths but that method is more complex + not available in 4.8.
     */
    private static void removeItem(PrismContainerValue<?> pcv, ItemPath path) {
        if (path.isEmpty()) {
            return; // shouldn't occur
        }
        var first = path.firstToName();
        var subItem = pcv.findItem(first);
        if (subItem == null) {
            return; // Final or intermediate item is not present; nothing to do here.
        }
        var rest = path.rest();
        if (rest.isEmpty()) {
            pcv.remove(subItem);
        } else {
            // Remove the item from all values of the subItem
            for (PrismValue value : subItem.getValues()) {
                if (value instanceof PrismContainerValue<?> subPcv) {
                    removeItem(subPcv, rest);
                }
            }
        }
    }

    private static <T extends Visitable<T>> Visitor<T> sensitivePropertiesCollector(Consumer<ItemPath> pathConsumer) {
        return item -> {
            if (item instanceof PrismPropertyValue<?> propertyValue) {
                var typeName = propertyValue.getTypeName();
                if (ProtectedDataType.COMPLEX_TYPE.equals(typeName)
                        || ProtectedStringType.COMPLEX_TYPE.equals(typeName)) {
                    // Note: ProtectedByteArrayType has the same type name as ProtectedDataType (why?)
                    // TODO we might also check using getRealValue() but that may be too fragile - what would we do if
                    //  that check failed?
                    pathConsumer.accept(propertyValue.getPath());
                }
            }
        };
    }

    private static class SensitiveDataRemovingVisitor<T extends Visitable<T>>
            implements ConfigurableVisitor<T>, JaxbVisitor {

        private final Log logger;

        private SensitiveDataRemovingVisitor(Log logger) {
            this.logger = logger;
        }

        @Override
        public void visit(JaxbVisitable visitable) {
            if (visitable instanceof ProtectedDataType<?> protectedData) {
                protectedData.setEncryptedData(null);
                protectedData.setHashedData(null);
                if (protectedData instanceof ProtectedStringType protectedString) {
                    protectedString.setClearValue("REDACTED");
                } else if (protectedData instanceof ProtectedByteArrayType protectedByteArray) {
                    protectedByteArray.setClearValue(ArrayUtils.toObject("REDACTED".getBytes(StandardCharsets.UTF_8)));
                } else {
                    // currently no other subtypes here; I'm not sure if the empty object would be OK if we get here
                }
            } else {
                // Should we parse not-yet-parsed RawType here?
                JaxbVisitable.visitPrismStructure(visitable, this);
            }
        }

        @Override
        public void visit(Visitable visitable) {
            if (visitable instanceof PrismPropertyValue<?> propertyValue) {
                try {
                    if (propertyValue.getRealValue() instanceof JaxbVisitable jaxbVisitable) {
                        jaxbVisitable.accept(this);
                    }
                } catch (Exception e) {
                    this.logger.warn("Couldn't get real value of item @{}: {}", propertyValue, e.getMessage());
                }
            }
        }

        @Override
        public boolean shouldVisitEmbeddedObjects() {
            return true;
        }
    }
}
