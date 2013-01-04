package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

public class SynchronizationSituationUtil {

	public static boolean contains(ObjectType target, String sourceChannel, SynchronizationSituationType situation){
		if (target instanceof ResourceObjectShadowType){
			List<SynchronizationSituationDescriptionType> syncSituationDescriptions = ((ResourceObjectShadowType) target).getSynchronizationSituationDescription();
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
	
	public static PropertyDelta createSynchronizationSituationDelta(PrismObject object, SynchronizationSituationType situation){
		List<PropertyDelta> syncSituationDeltas = new ArrayList<PropertyDelta>();
		PropertyDelta syncSituationDelta = PropertyDelta.createReplaceDelta(object.getDefinition(),
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION, situation);
		syncSituationDeltas.add(syncSituationDelta);
		
		return syncSituationDelta;
	}
	
	public static List<PropertyDelta> createSynchronizationSituationDescriptionDelta(PrismObject object, SynchronizationSituationType situation, String sourceChannel){
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(sourceChannel);
		syncSituationDescription.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		
		List<PropertyDelta> deltas = new ArrayList<PropertyDelta>();

		PropertyDelta syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
		syncSituationDelta.addValueToAdd(new PrismPropertyValue(syncSituationDescription));
		deltas.add(syncSituationDelta);
		
		SynchronizationSituationDescriptionType oldSituationDescription = getSituationFromSameChannel(object, sourceChannel);
		if (oldSituationDescription != null) {
			syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
					ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
			syncSituationDelta.addValueToDelete(new PrismPropertyValue(oldSituationDescription));
			deltas.add(syncSituationDelta);
		}
		
		
		return deltas;

	}
	
	private static SynchronizationSituationDescriptionType getSituationFromSameChannel(PrismObject prismObject, String channel){
		ResourceObjectShadowType target = (ResourceObjectShadowType) prismObject.asObjectable();
		List<SynchronizationSituationDescriptionType> syncSituationDescriptions = ((ResourceObjectShadowType) target).getSynchronizationSituationDescription();
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
