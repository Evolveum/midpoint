/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

public class SynchronizationSituationUtil {

	public static boolean contains(ObjectType target, String sourceChannel, SynchronizationSituationType situation){
		if (target instanceof ShadowType){
			List<SynchronizationSituationDescriptionType> syncSituationDescriptions = ((ShadowType) target).getSynchronizationSituationDescription();
			if (syncSituationDescriptions == null || syncSituationDescriptions.isEmpty()){
				return false;
			}
			for (SynchronizationSituationDescriptionType syncSituationDescription : syncSituationDescriptions){
				if (sourceChannel == null && syncSituationDescription.getChannel() != null){
					return false;
				}
				if (sourceChannel != null && syncSituationDescription.getChannel() == null){
					return false;
				}
				if (situation == null && syncSituationDescription.getSituation() != null){
					return false;
				}
				if (situation != null && syncSituationDescription.getSituation() == null){
					return false;
				}
				if (((syncSituationDescription.getChannel() == null && sourceChannel == null) || (syncSituationDescription.getChannel().equals(sourceChannel)))
						&& ((syncSituationDescription.getSituation() == null && situation == null) || (syncSituationDescription.getSituation() == situation))) {
					return true;
				}
			}
		}
		return true;
	}
	
	public static PropertyDelta<SynchronizationSituationType> createSynchronizationSituationDelta(PrismObject object, SynchronizationSituationType situation){
		
		SynchronizationSituationType oldValue = ((ShadowType) object.asObjectable())
				.getSynchronizationSituation();
		if (situation == null) {
			if (oldValue != null) {
				ItemPath syncSituationPath = new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION);
				return PropertyDelta.createModificationDeleteProperty(syncSituationPath,
						object.findProperty(syncSituationPath).getDefinition(), oldValue);
			}

		} else {
			if (oldValue != situation) {
				return PropertyDelta.createReplaceDelta(object.getDefinition(),
						ShadowType.F_SYNCHRONIZATION_SITUATION, situation);
			}
		}
		return null;
	}
	
	public static PropertyDelta<XMLGregorianCalendar> createSynchronizationTimestampDelta(PrismObject object, XMLGregorianCalendar timestamp){
//		XMLGregorianCalendar gcal = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		PropertyDelta<XMLGregorianCalendar> syncSituationDelta = PropertyDelta.createReplaceDelta(object.getDefinition(),
				ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp);
		return syncSituationDelta;
	}
	
//	public static List<PropertyDelta<?>> createSynchronizationSituationAndDescriptionDelta(PrismObject object, SynchronizationSituationType situation, String sourceChannel){
//		XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
//		
//		List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.createSynchronizationSituationDescriptionDelta(object, situation, timestamp, sourceChannel);
//		PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(object, situation);
//		if (syncSituationDelta != null){
//		syncSituationDeltas.add(syncSituationDelta);
//		}
		
//		List<PropertyDelta<?>> delta = createSynchronizationSituationDescriptionDelta(object, situation, timestamp, sourceChannel);
//		
//		if (!syncSituationDeltas.isEmpty()) {
//			PropertyDelta timestampDelta = createSynchronizationTimestampDelta(object, timestamp);
//			syncSituationDeltas.add(timestampDelta);
//		}
//		
//		PropertyDelta syncSituationDelta = createSynchronizationSituationDelta(object, situation);
//		
//		if (syncSituationDelta != null) {
//			delta.add(syncSituationDelta);
//		}
//		
//		return syncSituationDeltas;
//	}
	
	public static List<PropertyDelta<?>> createSynchronizationSituationDescriptionDelta(PrismObject object, SynchronizationSituationType situation, XMLGregorianCalendar timestamp, String sourceChannel){
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(sourceChannel);
		syncSituationDescription.setTimestamp(timestamp);
		
		List<PropertyDelta<?>> deltas = new ArrayList<PropertyDelta<?>>();

		PropertyDelta syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
				ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
		syncSituationDelta.addValueToAdd(new PrismPropertyValue(syncSituationDescription));
		deltas.add(syncSituationDelta);
		
		SynchronizationSituationDescriptionType oldSituationDescription = getSituationFromSameChannel(object, sourceChannel);
		if (oldSituationDescription != null) {
			syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
					ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
			syncSituationDelta.addValueToDelete(new PrismPropertyValue(oldSituationDescription));
			deltas.add(syncSituationDelta);
		}
		
		
		return deltas;

	}
	
	private static SynchronizationSituationDescriptionType getSituationFromSameChannel(PrismObject prismObject, String channel){
		ShadowType target = (ShadowType) prismObject.asObjectable();
		List<SynchronizationSituationDescriptionType> syncSituationDescriptions = ((ShadowType) target).getSynchronizationSituationDescription();
		if (syncSituationDescriptions == null || syncSituationDescriptions.isEmpty()){
			return null;
		}
		for (SynchronizationSituationDescriptionType syncSituationDescription : syncSituationDescriptions){
			if (StringUtils.isEmpty(syncSituationDescription.getChannel()) && StringUtils.isEmpty(channel)){
				return syncSituationDescription;
			}
			if ((StringUtils.isEmpty(syncSituationDescription.getChannel()) && channel != null) || (StringUtils.isEmpty(channel) && syncSituationDescription.getChannel() != null)){
				return null;
			}
			if (syncSituationDescription.getChannel().equals(channel)){
				return syncSituationDescription;
			}
		}
		return null;
	}
}
