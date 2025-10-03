package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ExportConfigurationWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.FreezableList;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ExportResourceSchemaAction extends AbstractRepositorySearchAction<ExportOptions, Void>{

    private static final Set<ItemName> ITEMS_TO_KEEP = Set.of(
            ResourceType.F_CONNECTOR_REF,
            ResourceType.F_NAME,
            ResourceType.F_SCHEMA
    );

    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            new ExportConsumerWorker(context, options, queue, operation) {

                @Override
                protected void editObject(PrismObject<? extends ObjectType> prismObject) {
                    // Lets remove everything except schema

                    // We copy items in order to remove it
                    var items = new ArrayList<>(prismObject.getValue().getItems());

                    for (var item : items) {
                        if (!ITEMS_TO_KEEP.contains(item.getElementName())) {
                            prismObject.remove(item);
                        }
                    }
                }
            }.run();
            return null;
        };
    }

    @Override
    public String getOperationName() {
        return "export-resource-schema-action";
    }

    @Override
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        return List.of(ObjectTypes.RESOURCE);
    }
}
