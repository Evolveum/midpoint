package com.evolveum.midpoint.ninja.action.worker;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;

public class ExportConfigurationWorker extends ExportConsumerWorker {

    public ExportConfigurationWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected boolean shouldSkipObject(PrismObject<? extends ObjectType> prismObject) {
        return false;

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

        final List<ItemName> include = List.of(
                SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION, SystemConfigurationType.F_MODEL_HOOKS,
                SystemConfigurationType.F_CORRELATION);

        if (prismObject.isOfType(SystemConfigurationType.class)) {
            prismObject.getValue().getItems().forEach(item -> item.removeIf(
                    val -> !ItemPathCollectionsUtil.containsSubpathOrEquivalent(include, val.getPath())));
        }

        final Collection<ItemPath> excludeItems = options.getExcludeItems();
        prismObject.accept(sensitiveDataCollector(excludeItems::add));
        excludeItems.forEach(path -> prismObject.removeItem(path, PrismProperty.class));


        // Based on object type we can remove additional items, which are not interesting to us
    }

    private <T extends Visitable<T>> Visitor<T> sensitiveDataCollector(Consumer<ItemPath> pathConsumer) {
        return item -> {
            if (item instanceof PrismPropertyValue<?> property
                    && property.getTypeName().equals(ProtectedDataType.COMPLEX_TYPE)) {
                pathConsumer.accept(property.getPath());
            }
        };
    }

}
