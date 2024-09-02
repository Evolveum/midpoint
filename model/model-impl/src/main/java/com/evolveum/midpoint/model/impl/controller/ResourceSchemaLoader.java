package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ResourceSchemaLoader implements Function<String, PrismObject<ResourceType>> {

    @Autowired
    private ProvisioningService provisioningService;
    @Autowired TaskManager taskManager;

    @Autowired
    private ResourceSchemaRegistry resourceSchemaRegistry;

    // FIXME: This is antipattern, but prism does not have tasks associated and parsing APIs do not have OperationResults
    private Task lookupTask;

    @PostConstruct
    public void init() {
        this.lookupTask = taskManager.createTaskInstance("system-resource-lookup-for-queries");
        resourceSchemaRegistry.registerResourceObjectLoader(this);
    }

    @Override
    public PrismObject<ResourceType> apply(String uid) {

        try {
            return provisioningService.getObject(ResourceType.class, uid, null, lookupTask, lookupTask.getResult());
        } catch (Exception e) {
            // NOOP
        }
        return null;
    }
}
