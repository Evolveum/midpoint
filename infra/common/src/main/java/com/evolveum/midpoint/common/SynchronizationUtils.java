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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

public class SynchronizationUtils {

    /**
     * Creates situation, description, and timestamp deltas.
     *
     * @param full if true, we consider this synchronization to be "full", and set the appropriate flag
     * in `synchronizationSituationDescription` as well as update `fullSynchronizationTimestamp`.
     */
    public static List<ItemDelta<?, ?>> createSynchronizationSituationAndDescriptionDelta(
            ShadowType shadow, SynchronizationSituationType situation, String sourceChannel,
            boolean full, XMLGregorianCalendar timestamp)
            throws SchemaException {

        List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();

        itemDeltas.add(
                createSynchronizationSituationDescriptionDelta(shadow, situation, timestamp, sourceChannel, full));
        itemDeltas.add(createSynchronizationTimestampDelta(timestamp));
        if (full) {
            itemDeltas.add(createFullSynchronizationTimestampDelta(timestamp));
        }
        itemDeltas.add(createSynchronizationSituationDelta(situation));

        return itemDeltas;
    }

    public static ItemDelta<?, ?> createSynchronizationSituationDelta(SynchronizationSituationType situation)
            throws SchemaException {
        return PrismContext.get().deltaFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_SITUATION)
                .replace(situation)
                .asItemDelta();
    }

    public static ItemDelta<?, ?> createSynchronizationTimestampDelta(XMLGregorianCalendar timestamp) throws SchemaException {
        return createSynchronizationTimestampDelta(ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp);
    }

    public static ItemDelta<?, ?> createFullSynchronizationTimestampDelta(XMLGregorianCalendar timestamp) throws SchemaException {
        return createSynchronizationTimestampDelta(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, timestamp);
    }

    private static ItemDelta<?, ?> createSynchronizationTimestampDelta(
            QName propName, XMLGregorianCalendar timestamp) throws SchemaException {
        return PrismContext.get().deltaFor(ShadowType.class)
                .item(propName)
                .replace(timestamp)
                .asItemDelta();
    }

    /** Creates delta for `synchronizationSituationDescription` (adding new, removing obsolete if present). */
    public static ItemDelta<?, ?> createSynchronizationSituationDescriptionDelta(
            ShadowType shadow, SynchronizationSituationType situation, XMLGregorianCalendar timestamp,
            String sourceChannel, boolean full) throws SchemaException {

        SynchronizationSituationDescriptionType descriptionToAdd = new SynchronizationSituationDescriptionType();
        descriptionToAdd.setSituation(situation);
        descriptionToAdd.setChannel(sourceChannel);
        descriptionToAdd.setTimestamp(timestamp);
        descriptionToAdd.setFull(full);

        List<SynchronizationSituationDescriptionType> descriptionsToDelete =
                getDescriptionsFromSameChannel(shadow, sourceChannel);

        return PrismContext.get().deltaFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION)
                .deleteRealValues(descriptionsToDelete)
                .add(descriptionToAdd)
                .asItemDelta();
    }

    private static @NotNull List<SynchronizationSituationDescriptionType> getDescriptionsFromSameChannel(
            ShadowType shadow, String channel) {
        return shadow.getSynchronizationSituationDescription().stream()
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
