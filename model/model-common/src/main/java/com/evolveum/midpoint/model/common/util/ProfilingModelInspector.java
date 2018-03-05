/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.model.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.common.util.ProfilingModelInspector.PartRuntime;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryPerformanceMonitor;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang3.ObjectUtils;

public class ProfilingModelInspector implements DiagnosticContext, ClockworkInspector, RepositoryPerformanceMonitor, DebugDumpable {

	protected static final Trace LOGGER = TraceManager.getTrace(ProfilingModelInspector.class);

	private Runtimes totalOperationTimes = new Runtimes();
	private Runtimes totalClockworkTimes = new Runtimes();
	private Map<ModelState, Runtimes> clockworkStateTimes = new HashMap<>();
	private Map<ModelState, Runtimes> projectorTimes = new HashMap<>();
	private Map<ModelState, List<PartRuntime>> projectorPartMap = new HashMap<>();
	private long totalMappingTimeMillis = 0;
	private long projectorMappingTotalMillis = 0;
	private long projectorMappingTotalCount = 0;
	private ModelContext lastLensContext;
	private ModelState currentState = null;
	private long totalRepoTime = 0;
	
	class Runtimes {
		long startTime = 0;
		long finishTime = 0;
		
		long etime() {
			return finishTime - startTime;
		}
		
		String etimeStr() {
			return etime() + " ms";
		}
	}
	
	class PartRuntime {
		
		PartRuntime(String part) {
			super();
			this.part = part;
		}
		
		String part;
		Runtimes runtimes = new Runtimes();
	}
	
	public void recordStart() {
		totalOperationTimes.startTime = System.currentTimeMillis();
	}
	
	public void recordFinish() {
		totalOperationTimes.finishTime = System.currentTimeMillis();
	}
	
	private void recordStateTime(Map<ModelState, Runtimes> map, ModelState state, Long start, Long finish) {
		Runtimes runtimes = map.get(state);
		if (runtimes == null) {
			runtimes = new Runtimes();
			map.put(state, runtimes);
		}
		if (start != null) {
			runtimes.startTime = start;
		}
		if (finish != null) {
			runtimes.finishTime = finish;
		}
	}
	
	public void reset() {
		totalClockworkTimes = new Runtimes();
		clockworkStateTimes = new HashMap<>();
		projectorTimes = new HashMap<>();
		projectorPartMap = new HashMap<>();
		totalMappingTimeMillis = 0;
		projectorMappingTotalMillis = 0;
		projectorMappingTotalCount = 0;
		lastLensContext = null;
		currentState = null;
	}

	@Override
	public <F extends ObjectType> void clockworkStart(ModelContext<F> context) {
		long now = System.currentTimeMillis();
		totalClockworkTimes.startTime = now; 
		recordStateTime(clockworkStateTimes, ModelState.INITIAL, now, null);
		currentState = ModelState.INITIAL;
	}

	@Override
	public <F extends ObjectType> void clockworkFinish(ModelContext<F> context) {
		long now = System.currentTimeMillis();
		totalClockworkTimes.finishTime = now;
		recordStateTime(clockworkStateTimes, ModelState.FINAL, null, now);
	}
	
	@Override
	public <F extends ObjectType> void clockworkStateSwitch(ModelContext<F> contextBefore, ModelState newState) {
		long now = System.currentTimeMillis();
		recordStateTime(clockworkStateTimes, contextBefore.getState(), null, now);
		recordStateTime(clockworkStateTimes, newState, now, null);
		currentState = newState;
	}
	
	@Override
	public <F extends ObjectType> void projectorStart(ModelContext<F> context) {
		recordStateTime(projectorTimes, context.getState(), System.currentTimeMillis(), null);
	}

	@Override
	public <F extends ObjectType> void projectorFinish(ModelContext<F> context) {
		recordStateTime(projectorTimes, context.getState(), null, System.currentTimeMillis());
		String desc = null;
		if (context.getFocusContext() != null) {
			PrismObject<F> focusObject = context.getFocusContext().getObjectNew();
			if (focusObject == null) {
				context.getFocusContext().getObjectOld();
			}
			if (focusObject != null) {
				desc = focusObject.toString();
			}
		} else {
			for (ModelProjectionContext projectionContext: context.getProjectionContexts()) {
				PrismObject<ShadowType> projObj = projectionContext.getObjectNew();
				if (projObj == null) {
					projObj = projectionContext.getObjectOld();
				}
				if (projObj != null) {
					desc = projObj.toString();
					break;
				}
			}
		}
		int changes = 0;
		Collection<ObjectDelta<? extends ObjectType>> allDeltas = null;
		try {
			allDeltas = context.getAllChanges();
		} catch (SchemaException e) {
			changes = -1;
		}
		if (allDeltas != null) {
			changes = allDeltas.size();
		}
		long projectorEtime = projectorTimes.get(context.getState()).etime();
		LOGGER.trace("Projector {} finished ({}), {} changes, etime: {} ms ({} mapping evaluated, {} ms total)",
				context.getState(), desc, changes, projectorEtime, projectorMappingTotalCount, projectorMappingTotalMillis);

		lastLensContext = context;
	}

	public <F extends ObjectType> ModelContext<F> getLastLensContext() {
		return lastLensContext;
	}

	@Override
	public <F extends ObjectType> void afterMappingEvaluation(ModelContext<F> context, Mapping<?,?> evaluatedMapping) {
		totalMappingTimeMillis += ObjectUtils.defaultIfNull(evaluatedMapping.getEtime(), 0L);
		projectorMappingTotalMillis += ObjectUtils.defaultIfNull(evaluatedMapping.getEtime(), 0L);
		projectorMappingTotalCount++;
	}

	private void recordProjectorPartTime(ModelState state, String componenetName, Long start, Long finish) {
		List<PartRuntime> partList = projectorPartMap.get(state);
		if (partList == null) {
			partList = new ArrayList<>();
			projectorPartMap.put(state, partList);
		}
		PartRuntime partRuntime = findPartRuntime(partList, componenetName);
		if (partRuntime == null) {
			partRuntime = new PartRuntime(componenetName);
			partList.add(partRuntime);
		}
		if (start != null) {
			partRuntime.runtimes.startTime = start;
		}
		if (finish != null) {
			partRuntime.runtimes.finishTime = finish;
		}
	}
	
	private PartRuntime findPartRuntime(List<PartRuntime> partList, String componenetName) {
		for (int i = partList.size() - 1; i >= 0; i--) {
			PartRuntime partRuntime = partList.get(i);
			if (partRuntime.part.equals(componenetName)) {
				return partRuntime;
			}
		}
		return null;
	}

	@Override
	public void projectorComponentSkip(String componentName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void projectorComponentStart(String componentName) {
		recordProjectorPartTime(currentState, componentName, System.currentTimeMillis(), null);
	}

	@Override
	public void projectorComponentFinish(String componentName) {
		recordProjectorPartTime(currentState, componentName, null, System.currentTimeMillis());
	}
	
	@Override
	public void recordRepoOperation(long durationMillis) {
		totalRepoTime = totalRepoTime + durationMillis;
	}


	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ProfilingModelInspector.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Clockwork", totalClockworkTimes==null?null:totalClockworkTimes.etimeStr(), indent + 1);
		dumpState(sb, ModelState.INITIAL, indent);
		dumpState(sb, ModelState.PRIMARY, indent);
		dumpState(sb, ModelState.SECONDARY, indent);
		dumpState(sb, ModelState.EXECUTION, indent);
		dumpState(sb, ModelState.FINAL, indent);
		dumpState(sb, ModelState.POSTEXECUTION, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "totalOperationTime", totalOperationTimes.etimeStr(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "totalMappingTime", totalMappingTimeMillis + " ms", indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "totalRepoTime", totalRepoTime + " ms", indent + 1);
		return sb.toString();
	}

	private void dumpState(StringBuilder sb, ModelState state, int indent) {
		Runtimes runtimes = clockworkStateTimes.get(state);
		if (runtimes == null) {
			return;
		}
		DebugUtil.debugDumpWithLabelLn(sb, state.toString(), runtimes==null?null:runtimes.etimeStr(), indent + 2);
		Runtimes projectorRuntimes = projectorTimes.get(state);
		if (projectorRuntimes != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "projector", projectorRuntimes.etimeStr(), indent + 3);
			List<PartRuntime> partList = projectorPartMap.get(state);
			if (partList != null) {
				for (PartRuntime partRuntime : partList) {
					DebugUtil.debugDumpWithLabelLn(sb, partRuntime.part, partRuntime.runtimes.etimeStr(), indent + 4);
				}
			}
		}
	}

}
