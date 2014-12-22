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
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

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
	
	public static PropertyDelta<XMLGregorianCalendar> createSynchronizationTimestampDelta(PrismObject object, 
			QName propName, XMLGregorianCalendar timestamp){
//		XMLGregorianCalendar gcal = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		PropertyDelta<XMLGregorianCalendar> syncSituationDelta = PropertyDelta.createReplaceDelta(object.getDefinition(),
				propName, timestamp);
		return syncSituationDelta;
	}
	
	public static List<PropertyDelta<?>> createSynchronizationSituationAndDescriptionDelta(PrismObject object, SynchronizationSituationType situation, 
			String sourceChannel, boolean full){
		XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		
		List<PropertyDelta<?>> delta = createSynchronizationSituationDescriptionDelta(object, situation, timestamp, sourceChannel, full);
			
		PropertyDelta timestampDelta = createSynchronizationTimestampDelta(object, ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp);
		delta.add(timestampDelta);
		
		if (full) {
			timestampDelta = createSynchronizationTimestampDelta(object, ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, timestamp);
			delta.add(timestampDelta);
		}
		
		PropertyDelta syncSituationDelta = createSynchronizationSituationDelta(object, situation);
		
		if (syncSituationDelta != null) {
			delta.add(syncSituationDelta);
		}
		
		return delta;
	}
	
	public static List<PropertyDelta<?>> createSynchronizationSituationDescriptionDelta(PrismObject object, 
			SynchronizationSituationType situation, XMLGregorianCalendar timestamp, String sourceChannel, boolean full){
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(sourceChannel);
		syncSituationDescription.setTimestamp(timestamp);
		syncSituationDescription.setFull(full);
		
		List<PropertyDelta<?>> deltas = new ArrayList<PropertyDelta<?>>();

		PropertyDelta syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
				ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
		syncSituationDelta.addValueToAdd(new PrismPropertyValue(syncSituationDescription));
		deltas.add(syncSituationDelta);
		
		List<PrismPropertyValue<SynchronizationSituationDescriptionType>> oldSituationDescriptions = getSituationFromSameChannel(object, sourceChannel);
		if (oldSituationDescriptions != null && !oldSituationDescriptions.isEmpty()) {
			syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
					ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
			syncSituationDelta.addValuesToDelete(oldSituationDescriptions);
			deltas.add(syncSituationDelta);
		}
		
		return deltas;
	}
	
	private static List<PrismPropertyValue<SynchronizationSituationDescriptionType>> getSituationFromSameChannel(PrismObject prismObject, String channel){
		ShadowType target = (ShadowType) prismObject.asObjectable();
		List<SynchronizationSituationDescriptionType> syncSituationDescriptions = ((ShadowType) target).getSynchronizationSituationDescription();
		List<PrismPropertyValue<SynchronizationSituationDescriptionType>> valuesToDelete = new ArrayList<PrismPropertyValue<SynchronizationSituationDescriptionType>>();
		if (syncSituationDescriptions == null || syncSituationDescriptions.isEmpty()){
			return null;
		}
		for (SynchronizationSituationDescriptionType syncSituationDescription : syncSituationDescriptions){
			if (StringUtils.isEmpty(syncSituationDescription.getChannel()) && StringUtils.isEmpty(channel)){
				valuesToDelete.add(new PrismPropertyValue<SynchronizationSituationDescriptionType>(syncSituationDescription));
				continue;
//				return syncSituationDescription;
			}
			if ((StringUtils.isEmpty(syncSituationDescription.getChannel()) && channel != null) || (StringUtils.isEmpty(channel) && syncSituationDescription.getChannel() != null)){
//				return null;
				continue;
			}
			if (syncSituationDescription.getChannel().equals(channel)){
				valuesToDelete.add(new PrismPropertyValue<SynchronizationSituationDescriptionType>(syncSituationDescription));
				continue;
//				return syncSituationDescription;
			}
		}
		return valuesToDelete;
	}
}
