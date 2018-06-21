/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionObjectDeltaType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SceneUtil {

	public static Scene visualizeObjectTreeDeltas(ObjectTreeDeltasType deltas, String displayNameKey,
			PrismContext prismContext, ModelInteractionService modelInteractionService,
			Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		List<Scene> scenes = new ArrayList<>();
		if (deltas != null) {
			if (deltas.getFocusPrimaryDelta() != null) {
				ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(deltas.getFocusPrimaryDelta(), prismContext);
				scenes.add(modelInteractionService.visualizeDelta(delta, task, result));
			}
			for (ProjectionObjectDeltaType projectionObjectDelta : deltas.getProjectionPrimaryDelta()) {
				ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(projectionObjectDelta.getPrimaryDelta(), prismContext);
				scenes.add(modelInteractionService.visualizeDelta(delta, task, result));
			}
		}
		return new WrapperScene(scenes, displayNameKey);
	}
}
