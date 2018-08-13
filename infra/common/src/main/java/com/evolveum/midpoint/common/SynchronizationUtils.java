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

package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_MaybeDelete;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

public class SynchronizationUtils {
	
	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationUtils.class);
	
	private static PropertyDelta<SynchronizationSituationType> createSynchronizationSituationDelta(
			PrismObject<ShadowType> shadow, SynchronizationSituationType situation) {
		
		if (situation == null) {
			SynchronizationSituationType oldValue = shadow.asObjectable().getSynchronizationSituation();
			return PropertyDelta.createModificationDeleteProperty(new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION), shadow.getDefinition(), oldValue);
		}

		return PropertyDelta.createModificationReplaceProperty(ShadowType.F_SYNCHRONIZATION_SITUATION, shadow.getDefinition(), situation);
	}

	private static PropertyDelta<XMLGregorianCalendar> createSynchronizationTimestampDelta(PrismObject<ShadowType> object,
			QName propName, XMLGregorianCalendar timestamp) {
		PropertyDelta<XMLGregorianCalendar> syncSituationDelta = PropertyDelta
				.createReplaceDelta(object.getDefinition(), propName, timestamp);
		return syncSituationDelta;
	}

	public static List<PropertyDelta<?>> createSynchronizationSituationAndDescriptionDelta(PrismObject<ShadowType> shadow,
			SynchronizationSituationType situation, String sourceChannel, boolean full) throws SchemaException {
		
		XMLGregorianCalendar timestamp = XmlTypeConverter
				.createXMLGregorianCalendar(System.currentTimeMillis());
		
		List<PropertyDelta<?>> propertyDeltas = new ArrayList<>();
		
		PropertyDelta<SynchronizationSituationDescriptionType> syncDescriptionDelta = createSynchronizationSituationDescriptionDelta(shadow, situation,
				timestamp, sourceChannel, full);
		propertyDeltas.add(syncDescriptionDelta);
		
		
		propertyDeltas.addAll(createSynchronizationTimestampsDelta(shadow, timestamp, full));

		PropertyDelta<SynchronizationSituationType> syncSituationDelta = createSynchronizationSituationDelta(shadow, situation);
		propertyDeltas.add(syncSituationDelta);
		
		return propertyDeltas;
	}
	
	private static PropertyDelta<SynchronizationSituationDescriptionType> createSynchronizationSituationDescriptionDelta(PrismObject<ShadowType> shadow,
			SynchronizationSituationType situation, XMLGregorianCalendar timestamp, String sourceChannel,
			boolean full) throws SchemaException {
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(sourceChannel);
		syncSituationDescription.setTimestamp(timestamp);
		syncSituationDescription.setFull(full);

		S_MaybeDelete builder = DeltaBuilder.deltaFor(ShadowType.class, shadow.getPrismContext())
			.item(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION).add(syncSituationDescription);
		
		
		List<SynchronizationSituationDescriptionType> oldSituationDescriptions = getSituationFromSameChannel(
				shadow, sourceChannel);
		if (CollectionUtils.isNotEmpty(oldSituationDescriptions)) {
			builder.deleteRealValues(oldSituationDescriptions);
		}

		return (PropertyDelta<SynchronizationSituationDescriptionType>) builder.asItemDelta();
	}
	
		public static List<PropertyDelta<?>> createSynchronizationTimestampsDelta(
			PrismObject<ShadowType> shadow) {
			XMLGregorianCalendar timestamp = XmlTypeConverter
					.createXMLGregorianCalendar(System.currentTimeMillis());
		return createSynchronizationTimestampsDelta(shadow, timestamp, true);
	}
	
	private static List<PropertyDelta<?>> createSynchronizationTimestampsDelta(PrismObject<ShadowType> shadow, XMLGregorianCalendar timestamp, boolean full) {
		
		List<PropertyDelta<?>> deltas = new ArrayList<>();
		PropertyDelta<XMLGregorianCalendar> timestampDelta = createSynchronizationTimestampDelta(shadow,
				ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp);
		deltas.add(timestampDelta);

		if (full) {
			timestampDelta = createSynchronizationTimestampDelta(shadow,
					ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, timestamp);
			deltas.add(timestampDelta);
		}
		return deltas;
	}

	

	private static List<SynchronizationSituationDescriptionType> getSituationFromSameChannel(
			PrismObject<ShadowType> shadow, String channel) {
		
		List<SynchronizationSituationDescriptionType> syncSituationDescriptions = shadow.asObjectable().getSynchronizationSituationDescription();
		List<SynchronizationSituationDescriptionType> valuesToDelete = new ArrayList<>();
		if (CollectionUtils.isEmpty(syncSituationDescriptions)) {
			return null;
		}
		for (SynchronizationSituationDescriptionType syncSituationDescription : syncSituationDescriptions) {
			if (StringUtils.isEmpty(syncSituationDescription.getChannel()) && StringUtils.isEmpty(channel)) {
				valuesToDelete.add(syncSituationDescription);
				continue;
			}
			if ((StringUtils.isEmpty(syncSituationDescription.getChannel()) && channel != null)
					|| (StringUtils.isEmpty(channel) && syncSituationDescription.getChannel() != null)) {
				continue;
			}
			if (syncSituationDescription.getChannel().equals(channel)) {
				valuesToDelete.add(syncSituationDescription);
				continue;
			}
		}
		return valuesToDelete;
	}
	
public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent, ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource) throws SchemaException{
		
		List<QName> policyObjectClasses = synchronizationPolicy.getObjectClass();

		if (policyObjectClasses == null || policyObjectClasses.isEmpty()) {

			String policyIntent = synchronizationPolicy.getIntent();
			ShadowKindType policyKind = synchronizationPolicy.getKind();
			ObjectClassComplexTypeDefinition policyObjectClass = null;
			RefinedResourceSchema schema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
			if (schema == null) {
				throw new SchemaException("No schema defined in resource. Possible configuration problem?");
			}
			if (policyKind == null && policyIntent == null) {
				policyObjectClass = schema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
			}

			if (policyKind != null) {
				if (StringUtils.isEmpty(policyIntent)) {
					policyObjectClass = schema.findDefaultObjectClassDefinition(policyKind);
				} else {
					policyObjectClass = schema.findObjectClassDefinition(policyKind, policyIntent);
				}

			}
			if (policyObjectClass != null && !policyObjectClass.getTypeName().equals(objectClass)) {
				return false;
			}
		}
		if (policyObjectClasses != null && !policyObjectClasses.isEmpty()) {
			if (objectClass != null && !QNameUtil.contains(policyObjectClasses, objectClass)) {
				return false;
			}
		}

		// kind
		ShadowKindType policyKind = synchronizationPolicy.getKind();
		LOGGER.trace("Comparing kinds, policy kind: {}, current kind: {}", policyKind, kind);
		if (policyKind != null && kind != null && !policyKind.equals(kind)) {
			LOGGER.trace("Kinds don't match, skipping policy {}", synchronizationPolicy);
			return false;
		}

		// intent
		// TODO is the intent always present in shadow at this time? [med]
		String policyIntent = synchronizationPolicy.getIntent();
		LOGGER.trace("Comparing intents, policy intent: {}, current intent: {}", policyIntent, intent);
		if (policyIntent != null && intent != null
				&& !MiscSchemaUtil.equalsIntent(intent, policyIntent)) {
			LOGGER.trace("Intents don't match, skipping policy {}", synchronizationPolicy);
			return false;
		}
		
		return true;
	}
}
