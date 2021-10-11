/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class DummyResourceCollection {

    private static final Trace LOGGER = TraceManager.getTrace(DummyResourceCollection.class);

    private Map<String, DummyResourceContoller> map = new HashMap<>();
    private ModelService modelService;

    public DummyResourceCollection(ModelService modelService) {
        super();
        this.modelService = modelService;
    }

    public DummyResourceContoller initDummyResource(String name, File resourceFile, String resourceOid,
            FailableProcessor<DummyResourceContoller> controllerInitLambda,
            Task task, OperationResult result) throws Exception {
        if (map.containsKey(name)) {
            throw new IllegalArgumentException("Dummy resource "+name+" already initialized");
        }
        DummyResourceContoller controller = DummyResourceContoller.create(name);
        if (controllerInitLambda != null) {
            controllerInitLambda.process(controller);
        } else {
            controller.populateWithDefaultSchema();
        }
        if (resourceFile != null) {
            LOGGER.info("Importing {}", resourceFile);
            modelService.importObjectsFromFile(resourceFile, null, task, result);
            OperationResult importResult = result.getLastSubresult();
            if (importResult.isError()) {
                throw new RuntimeException("Error importing "+resourceFile+": "+importResult.getMessage());
            }
            LOGGER.debug("File {} imported: {}", resourceFile, importResult);
        }
        if (resourceOid != null) {
            PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            controller.setResource(resource);
        }
        map.put(name, controller);
        return controller;
    }

    public void initDummyResource(String name, DummyResourceContoller controller) {
        if (map.containsKey(name)) {
            throw new IllegalArgumentException("Dummy resource "+name+" already initialized");
        }
        map.put(name, controller);
    }

    public DummyResourceContoller get(String name) {
        DummyResourceContoller contoller = map.get(name);
        if (contoller == null) {
            throw new IllegalArgumentException("No dummy resource with name "+name);
        }
        return contoller;
    }

    public PrismObject<ResourceType> getResourceObject(String name) {
        return get(name).getResource();
    }

    public ResourceType getResourceType(String name) {
        return get(name).getResourceType();
    }

    public DummyResource getDummyResource(String name) {
        return get(name).getDummyResource();
    }

    public void forEachResourceCtl(Consumer<DummyResourceContoller> lambda) {
        map.forEach((k,v) -> lambda.accept(v));
    }

    /**
     * Resets the blocking state, error simulation, etc.
     */
    public void resetResources() {
        forEachResourceCtl(c -> c.reset());
    }
}
