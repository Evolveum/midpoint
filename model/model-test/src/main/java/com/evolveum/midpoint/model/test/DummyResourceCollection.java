/**
 * Copyright (c) 2016-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
