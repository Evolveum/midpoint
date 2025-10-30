package com.evolveum.midpoint.ninja.action;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import com.evolveum.midpoint.ninja.action.worker.ExportConfigurationSeparateWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportConfigurationWorker;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExportConfigurationAction extends AbstractRepositorySearchAction<ExportConfigurationOptions, Void>{

    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        if (options.isSplitFiles()) {
            return () -> {
                new ExportConfigurationSeparateWorker(context, options, queue, operation).run();
                return null;
            };
        } else {
            return () -> {
                new ExportConfigurationWorker(context, options, queue, operation).run();
                return null;
            };
        }
    }

    @Override
    public String getOperationName() {
        return "export-configuration";
    }

    @Override
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        return List.of(ObjectTypes.SYSTEM_CONFIGURATION,
            ObjectTypes.FUNCTION_LIBRARY,
            ObjectTypes.OBJECT_TEMPLATE,
            ObjectTypes.POLICY,
            ObjectTypes.ARCHETYPE,
            ObjectTypes.RESOURCE,
            ObjectTypes.SCHEMA,
            ObjectTypes.CONNECTOR
        );
    }
}
