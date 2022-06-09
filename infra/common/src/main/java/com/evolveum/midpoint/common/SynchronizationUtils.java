/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

public class SynchronizationUtils {

    public static PropertyDelta<SynchronizationSituationType> createSynchronizationSituationDelta(
            PrismObject<ShadowType> shadow, SynchronizationSituationType situation) {

        if (situation == null) {
            // TODO why this strange construction?
            SynchronizationSituationType oldValue = shadow.asObjectable().getSynchronizationSituation();
            return PrismContext.get().deltaFactory().property()
                    .createModificationDeleteProperty(ShadowType.F_SYNCHRONIZATION_SITUATION, shadow.getDefinition(), oldValue);
        } else {
            return PrismContext.get().deltaFactory().property()
                    .createModificationReplaceProperty(ShadowType.F_SYNCHRONIZATION_SITUATION, shadow.getDefinition(), situation);
        }
    }

    private static PropertyDelta<XMLGregorianCalendar> createSynchronizationTimestampDelta(PrismObject<ShadowType> object,
            QName propName, XMLGregorianCalendar timestamp) {
        return PrismContext.get().deltaFactory().property()
                .createReplaceDelta(object.getDefinition(), propName, timestamp);
    }

    /**
     * @param full if true, we consider this synchronization to be "full", and set the appropriate flag
     * in `synchronizationSituationDescription` as well as update `fullSynchronizationTimestamp`.
     */
    public static List<ItemDelta<?, ?>> createSynchronizationSituationAndDescriptionDelta(PrismObject<ShadowType> shadow,
            SynchronizationSituationType situation, String sourceChannel, boolean full, XMLGregorianCalendar timestamp)
            throws SchemaException {

        List<ItemDelta<?, ?>> propertyDeltas = new ArrayList<>();

        propertyDeltas.add(
                createSynchronizationSituationDescriptionDelta(shadow, situation, timestamp, sourceChannel, full));
        propertyDeltas.addAll(createSynchronizationTimestampsDeltas(shadow, timestamp, full));
        propertyDeltas.add(createSynchronizationSituationDelta(shadow, situation));

        return propertyDeltas;
    }

    public static PropertyDelta<SynchronizationSituationDescriptionType> createSynchronizationSituationDescriptionDelta(
            PrismObject<ShadowType> shadow, SynchronizationSituationType situation, XMLGregorianCalendar timestamp,
            String sourceChannel, boolean full) throws SchemaException {

        SynchronizationSituationDescriptionType descriptionToAdd = new SynchronizationSituationDescriptionType();
        descriptionToAdd.setSituation(situation);
        descriptionToAdd.setChannel(sourceChannel);
        descriptionToAdd.setTimestamp(timestamp);
        descriptionToAdd.setFull(full);

        List<SynchronizationSituationDescriptionType> descriptionsToDelete =
                getDescriptionsFromSameChannel(shadow, sourceChannel);

        //noinspection unchecked
        return (PropertyDelta<SynchronizationSituationDescriptionType>) PrismContext.get().deltaFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION)
                    .deleteRealValues(descriptionsToDelete)
                    .add(descriptionToAdd)
                .asItemDelta();
    }

    public static List<PropertyDelta<?>> createSynchronizationTimestampsDeltas(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar timestamp, boolean full) {

        List<PropertyDelta<?>> deltas = new ArrayList<>();
        deltas.add(createSynchronizationTimestampDelta(shadow, ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp));
        if (full) {
            deltas.add(createSynchronizationTimestampDelta(shadow, ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, timestamp));
        }
        return deltas;
    }

    private static @NotNull List<SynchronizationSituationDescriptionType> getDescriptionsFromSameChannel(
            PrismObject<ShadowType> shadow, String channel) {

        List<SynchronizationSituationDescriptionType> existingDescriptions =
                shadow.asObjectable().getSynchronizationSituationDescription();

        return existingDescriptions.stream()
                .filter(description -> isSameChannel(description.getChannel(), channel))
                .collect(Collectors.toList());
    }

    private static boolean isSameChannel(String ch1, String ch2) {
        if (StringUtils.isEmpty(ch1)) {
            return StringUtils.isEmpty(ch2);
        } else {
            return ch1.equals(ch2);
        }
    }
}
