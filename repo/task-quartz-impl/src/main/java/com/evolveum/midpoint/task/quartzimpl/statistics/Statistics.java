/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.task.quartzimpl.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;

/**
 *  Code to manage operational statistics. Originally it was a part of the TaskQuartzImpl
 *  but it is cleaner to keep it separate.
 *
 *  It is used for
 *  1) running background tasks (RunningTask) - both heavyweight and lightweight
 *  2) transient tasks e.g. those invoked from GUI
 */
public class Statistics {

	private static final Trace LOGGER = TraceManager.getTrace(Statistics.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	@NotNull private final PrismContext prismContext;

	public Statistics(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	private EnvironmentalPerformanceInformation environmentalPerformanceInformation = new EnvironmentalPerformanceInformation();
	private SynchronizationInformation synchronizationInformation;                // has to be explicitly enabled
	private IterativeTaskInformation iterativeTaskInformation;                    // has to be explicitly enabled
	private ActionsExecutedInformation actionsExecutedInformation;            // has to be explicitly enabled

	private EnvironmentalPerformanceInformation getEnvironmentalPerformanceInformation() {
		return environmentalPerformanceInformation;
	}

	private SynchronizationInformation getSynchronizationInformation() {
		return synchronizationInformation;
	}

	private IterativeTaskInformation getIterativeTaskInformation() {
		return iterativeTaskInformation;
	}

	public ActionsExecutedInformation getActionsExecutedInformation() {
		return actionsExecutedInformation;
	}

	@NotNull
	public List<String> getLastFailures() {
		return iterativeTaskInformation != null ? iterativeTaskInformation.getLastFailures() : Collections.emptyList();
	}

	private EnvironmentalPerformanceInformationType getAggregateEnvironmentalPerformanceInformation(Collection<Statistics> children) {
		if (environmentalPerformanceInformation == null) {
			return null;
		}
		EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
		EnvironmentalPerformanceInformation.addTo(rv, environmentalPerformanceInformation.getAggregatedValue());
		for (Statistics child : children) {
			EnvironmentalPerformanceInformation info = child.getEnvironmentalPerformanceInformation();
			if (info != null) {
				EnvironmentalPerformanceInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private IterativeTaskInformationType getAggregateIterativeTaskInformation(Collection<Statistics> children) {
		if (iterativeTaskInformation == null) {
			return null;
		}
		IterativeTaskInformationType rv = new IterativeTaskInformationType();
		IterativeTaskInformation.addTo(rv, iterativeTaskInformation.getAggregatedValue(), false);
		for (Statistics child : children) {
			IterativeTaskInformation info = child.getIterativeTaskInformation();
			if (info != null) {
				IterativeTaskInformation.addTo(rv, info.getAggregatedValue(), false);
			}
		}
		return rv;
	}

	private SynchronizationInformationType getAggregateSynchronizationInformation(Collection<Statistics> children) {
		if (synchronizationInformation == null) {
			return null;
		}
		SynchronizationInformationType rv = new SynchronizationInformationType();
		SynchronizationInformation.addTo(rv, synchronizationInformation.getAggregatedValue());
		for (Statistics child : children) {
			SynchronizationInformation info = child.getSynchronizationInformation();
			if (info != null) {
				SynchronizationInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private ActionsExecutedInformationType getAggregateActionsExecutedInformation(Collection<Statistics> children) {
		if (actionsExecutedInformation == null) {
			return null;
		}
		ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
		ActionsExecutedInformation.addTo(rv, actionsExecutedInformation.getAggregatedValue());
		for (Statistics child : children) {
			ActionsExecutedInformation info = child.getActionsExecutedInformation();
			if (info != null) {
				ActionsExecutedInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	public OperationStatsType getAggregatedLiveOperationStats(Collection<Statistics> children) {
		EnvironmentalPerformanceInformationType env = getAggregateEnvironmentalPerformanceInformation(children);
		IterativeTaskInformationType itit = getAggregateIterativeTaskInformation(children);
		SynchronizationInformationType sit = getAggregateSynchronizationInformation(children);
		ActionsExecutedInformationType aeit = getAggregateActionsExecutedInformation(children);
		if (env == null && itit == null && sit == null && aeit == null) {
			return null;
		}
		OperationStatsType rv = new OperationStatsType();
		rv.setEnvironmentalPerformanceInformation(env);
		rv.setIterativeTaskInformation(itit);
		rv.setSynchronizationInformation(sit);
		rv.setActionsExecutedInformation(aeit);
		rv.setTimestamp(createXMLGregorianCalendar(new Date()));
		return rv;
	}

	public void recordState(String message) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{}", message);
		}
		if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
			PERFORMANCE_ADVISOR.debug("{}", message);
		}
		environmentalPerformanceInformation.recordState(message);
	}

	public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
			ProvisioningOperation operation, boolean success, int count, long duration) {
		environmentalPerformanceInformation
				.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
	}

	public void recordNotificationOperation(String transportName, boolean success, long duration) {
		environmentalPerformanceInformation.recordNotificationOperation(transportName, success, duration);
	}

	public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
			long duration) {
		environmentalPerformanceInformation.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
	}

	public synchronized void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid,
			long started, Throwable exception, SynchronizationInformation.Record originalStateIncrement,
			SynchronizationInformation.Record newStateIncrement) {
		if (synchronizationInformation != null) {
			synchronizationInformation
					.recordSynchronizationOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception,
							originalStateIncrement, newStateIncrement);
		}
	}

	public synchronized void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (synchronizationInformation != null) {
			synchronizationInformation.recordSynchronizationOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	public synchronized void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid, long started, Throwable exception) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception);
		}
	}

	public void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
		recordIterativeOperationEnd(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid(), started, exception);
	}

	public void recordIterativeOperationStart(ShadowType shadow) {
		recordIterativeOperationStart(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid());
	}

	public synchronized void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
			ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation
					.recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel,
							exception);
		}
	}

	public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, String channel, Throwable exception) {
		recordObjectActionExecuted(object, null, null, changeType, channel, exception);
	}

	public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
			String defaultOid, ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			String name, displayName, oid;
			PrismObjectDefinition definition;
			Class<T> clazz;
			if (object != null) {
				name = PolyString.getOrig(object.getName());
				displayName = StatisticsUtil.getDisplayName(object);
				definition = object.getDefinition();
				clazz = object.getCompileTimeClass();
				oid = object.getOid();
				if (oid == null) {        // in case of ADD operation
					oid = defaultOid;
				}
			} else {
				name = null;
				displayName = null;
				definition = null;
				clazz = objectTypeClass;
				oid = defaultOid;
			}
			if (definition == null && clazz != null) {
				definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
			}
			QName typeQName;
			if (definition != null) {
				typeQName = definition.getTypeName();
			} else {
				typeQName = ObjectType.COMPLEX_TYPE;
			}
			actionsExecutedInformation
					.recordObjectActionExecuted(name, displayName, typeQName, oid, changeType, channel, exception);
		}
	}

	public void markObjectActionExecutedBoundary() {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation.markObjectActionExecutedBoundary();
		}
	}

	public void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
		environmentalPerformanceInformation = new EnvironmentalPerformanceInformation(value);
	}

	public void resetSynchronizationInformation(SynchronizationInformationType value) {
		synchronizationInformation = new SynchronizationInformation(value);
	}

	public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
		iterativeTaskInformation = new IterativeTaskInformation(value);
	}

	public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
		actionsExecutedInformation = new ActionsExecutedInformation(value);
	}

	public void startCollectingOperationStatsFromZero(boolean enableIterationStatistics, boolean enableSynchronizationStatistics,
			boolean enableActionsExecutedStatistics) {
		resetEnvironmentalPerformanceInformation(null);
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(null);
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(null);
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(null);
		}
	}

	public void startCollectingOperationStatsFromStoredValues(OperationStatsType stored, boolean enableIterationStatistics,
			boolean enableSynchronizationStatistics, boolean enableActionsExecutedStatistics) {
		if (stored == null) {
			stored = new OperationStatsType();
		}
		resetEnvironmentalPerformanceInformation(stored.getEnvironmentalPerformanceInformation());
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(stored.getIterativeTaskInformation());
		} else {
			iterativeTaskInformation = null;
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(stored.getSynchronizationInformation());
		} else {
			synchronizationInformation = null;
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(stored.getActionsExecutedInformation());
		} else {
			actionsExecutedInformation = null;
		}
	}
}
