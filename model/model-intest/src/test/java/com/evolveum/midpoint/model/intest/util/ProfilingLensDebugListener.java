package com.evolveum.midpoint.model.intest.util;

import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensDebugListener;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

public class ProfilingLensDebugListener implements LensDebugListener {
	
	protected static final Trace LOGGER = TraceManager.getTrace(ProfilingLensDebugListener.class);

	private long projectorStartTime = 0;
	private long projectorEndTime = 0;
	private long mappingTotalMillis = 0;
	private long projectorMappingTotalMillis = 0;
	private long projectorMappingTotalCount = 0;
	private LensContext lastLensContext;
	
	@Override
	public <F extends ObjectType> void beforeSync(LensContext<F> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType> void afterSync(LensContext<F> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType> void beforeProjection(LensContext<F> context) {
		projectorStartTime = System.currentTimeMillis();
		projectorMappingTotalMillis = 0;
		projectorMappingTotalCount = 0;
	}

	@Override
	public <F extends ObjectType> void afterProjection(LensContext<F> context) {
		projectorEndTime = System.currentTimeMillis();
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
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
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
		long projectorEtime = projectorEndTime - projectorStartTime;
		LOGGER.trace("Projector finished ({}), {} changes, etime: {} ms ({} mapping evaluated, {} ms total)", 
				new Object[]{desc, changes, projectorEtime, projectorMappingTotalCount, projectorMappingTotalMillis});
		
		lastLensContext = context;
	}

	public <F extends ObjectType> LensContext<F> getLastLensContext() {
		return lastLensContext;
	}

	@Override
	public <F extends ObjectType> void afterMappingEvaluation(LensContext<F> context, Mapping<?> evaluatedMapping) {
		mappingTotalMillis += evaluatedMapping.getEtime();
		projectorMappingTotalMillis += evaluatedMapping.getEtime();
		projectorMappingTotalCount++;
	}

}
