package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ResourceSchemaLoader implements Function<String, PrismObject<ResourceType>> {

    private static final Trace LOG = TraceManager.getTrace(ResourceSchemaLoader.class);

    @Autowired
    private ProvisioningService provisioningService;
    @Autowired TaskManager taskManager;

    @Autowired
    private ResourceSchemaRegistry resourceSchemaRegistry;


    @PostConstruct
    public void init() {
        resourceSchemaRegistry.registerResourceObjectLoader(this);
    }

    @Override
    public PrismObject<ResourceType> apply(String uid) {
        var lookupTask = taskManager.createTaskInstance(getClass().getName() + ".apply");
        var result = lookupTask.getResult();
        try  {
            return provisioningService.getObject(ResourceType.class, uid, null, lookupTask, result);
        } catch (Exception e) {
            // NOOP
            LoggingUtils.logException(LOG, "Can not get resource {}" , e, uid);
            result.recordException(e);
        } finally {
            result.close();
        }
        return null;
    }
}
