package com.evolveum.midpoint.ninja.action.worker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
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
        prismObject.removeItem(ObjectType.F_METADATA, PrismContainer.class);
        prismObject.removeItem(ObjectType.F_OPERATION_EXECUTION, PrismContainer.class);

        if (prismObject.isOfType(AssignmentHolderType.class)) {
            prismObject.removeItem(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, PrismContainer.class);
        }

        if (prismObject.isOfType(ResourceType.class)) {
            prismObject.removeItem(ResourceType.F_CONNECTOR_CONFIGURATION, PrismContainer.class);
        }

        keepOnly(prismObject,
                SystemConfigurationType.class,
                SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                SystemConfigurationType.F_MODEL_HOOKS,
                SystemConfigurationType.F_CORRELATION);

        keepOnly(prismObject,
                ConnectorType.class,
                ConnectorType.F_NAME,
                ConnectorType.F_AVAILABLE);

        // Remove items specified by the user.
        final Collection<ItemPath> explicitlyExcludedItems = options.getExcludeItems();
        explicitlyExcludedItems.forEach(path -> prismObject.removeItem(path, PrismProperty.class));

        // Remove items that contains ProtectedDataType values.
        final Collection<ItemPath> sensitiveItems = new ArrayList<>();
        prismObject.accept(sensitiveDataCollector(sensitiveItems::add));
        sensitiveItems.forEach(path -> prismObject.removeItem(path, PrismProperty.class));

        // Mask other ProtectedDataType values (it is quite hard to remove them, because they are not "normal"
        // properties)
        prismObject.accept(new SensitiveDataRemovingVisitor(context.getLog()));
    }

    private static void keepOnly(
            PrismObject<? extends ObjectType> prismObject, Class<? extends ObjectType> type, ItemName... itemNames) {
        if (prismObject.isOfType(type)) {
            final List<ItemName> itemNamesList = List.of(itemNames);
            prismObject.getValue().getItems().forEach(item -> item.removeIf(
                    val -> !ItemPathCollectionsUtil.containsSubpathOrEquivalent(itemNamesList, val.getPath())));
        }
    }

        // Based on object type we can remove additional items, which are not interesting to us

    private static <T extends Visitable<T>> Visitor<T> sensitiveDataCollector(Consumer<ItemPath> pathConsumer) {
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
