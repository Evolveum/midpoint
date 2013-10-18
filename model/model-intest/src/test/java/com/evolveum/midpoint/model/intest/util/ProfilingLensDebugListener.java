package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensDebugListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

public class ProfilingLensDebugListener implements LensDebugListener {
	
	protected static final Trace LOGGER = TraceManager.getTrace(ProfilingLensDebugListener.class);

	private long projectorStartTime = 0;
	private long projectorEndTime = 0;
	private long mappingTotalMillis = 0;
	private long projectorMappingTotalMillis = 0;
	private long projectorMappingTotalCount = 0;
	
	@Override
	public <F extends ObjectType, P extends ObjectType> void beforeSync(
			LensContext<F, P> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType, P extends ObjectType> void afterSync(
			LensContext<F, P> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType, P extends ObjectType> void beforeProjection(
			LensContext<F, P> context) {
		projectorStartTime = System.currentTimeMillis();
		projectorMappingTotalMillis = 0;
		projectorMappingTotalCount = 0;
	}

	@Override
	public <F extends ObjectType, P extends ObjectType> void afterProjection(
			LensContext<F, P> context) {
		projectorEndTime = System.currentTimeMillis();
		long projectorEtime = projectorEndTime - projectorStartTime;
		LOGGER.debug("Projector finished, etime: {} ms ({} mapping evaluated, {} ms total)", 
				new Object[]{projectorEtime, projectorMappingTotalCount, projectorMappingTotalMillis});
	}

	@Override
	public <F extends ObjectType, P extends ObjectType> void afterMappingEvaluation(
			LensContext<F, P> context, Mapping<?> evaluatedMapping) {
		mappingTotalMillis += evaluatedMapping.getEtime();
		projectorMappingTotalMillis += evaluatedMapping.getEtime();
		projectorMappingTotalCount++;
	}

}
