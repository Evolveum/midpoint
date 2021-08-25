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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class SynchronizationUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationUtils.class);

    private static PropertyDelta<SynchronizationSituationType> createSynchronizationSituationDelta(
            PrismObject<ShadowType> shadow, SynchronizationSituationType situation) {

        if (situation == null) {
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

    public static List<PropertyDelta<?>> createSynchronizationSituationAndDescriptionDelta(PrismObject<ShadowType> shadow,
            SynchronizationSituationType situation, String sourceChannel, boolean full, XMLGregorianCalendar timestamp)
            throws SchemaException {

        List<PropertyDelta<?>> propertyDeltas = new ArrayList<>();

        propertyDeltas.add(
                createSynchronizationSituationDescriptionDelta(shadow, situation, timestamp, sourceChannel, full));
        propertyDeltas.addAll(createSynchronizationTimestampsDeltas(shadow, timestamp, full));
        propertyDeltas.add(createSynchronizationSituationDelta(shadow, situation));

        return propertyDeltas;
    }

    private static PropertyDelta<SynchronizationSituationDescriptionType> createSynchronizationSituationDescriptionDelta(
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

    public static List<PropertyDelta<?>> createSynchronizationTimestampsDeltas(PrismObject<ShadowType> shadow) {
        return createSynchronizationTimestampsDeltas(
                shadow,
                XmlTypeConverter.createXMLGregorianCalendar(),
                true);
    }

    private static List<PropertyDelta<?>> createSynchronizationTimestampsDeltas(PrismObject<ShadowType> shadow,
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

    public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent,
            ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource, boolean strictIntent)
            throws SchemaException {

        List<QName> policyObjectClasses = synchronizationPolicy.getObjectClass();
        //check objectClass if match
        if (CollectionUtils.isNotEmpty(policyObjectClasses) && objectClass != null) {
            if (!QNameUtil.matchAny(objectClass, policyObjectClasses)) {
                return false;
            }
        }

        RefinedResourceSchema schema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        if (schema == null) {
            throw new SchemaException("No schema defined in resource. Possible configuration problem?");
        }

        ShadowKindType policyKind = synchronizationPolicy.getKind();
        if (policyKind == null) {
            policyKind = ShadowKindType.ACCOUNT;
        }

        String policyIntent = synchronizationPolicy.getIntent();

        ObjectClassComplexTypeDefinition policyObjectClass;
        if (StringUtils.isEmpty(policyIntent)) {
            policyObjectClass = schema.findDefaultObjectClassDefinition(policyKind);
            if (policyObjectClass != null) {
                policyIntent = policyObjectClass.getIntent();
            }
        } else {
            policyObjectClass = schema.findObjectClassDefinition(policyKind, policyIntent);
        }

        if (policyObjectClass == null) {
            return false;
        }

        // re-check objctClass if wasn't defined
        if (objectClass != null && !QNameUtil.match(objectClass, policyObjectClass.getTypeName())) {
            return false;
        }

        // kind
        LOGGER.trace("Comparing kinds, policy kind: {}, current kind: {}", policyKind, kind);
        if (kind != null && kind != ShadowKindType.UNKNOWN && !policyKind.equals(kind)) {
            LOGGER.trace("Kinds don't match, skipping policy {}", synchronizationPolicy);
            return false;
        }

        // intent
        // TODO is the intent always present in shadow at this time? [med]
        LOGGER.trace("Comparing intents, policy intent: {}, current intent: {}", policyIntent, intent);
        if (!strictIntent) {
            if (intent != null && !SchemaConstants.INTENT_UNKNOWN.equals(intent) && !MiscSchemaUtil.equalsIntent(intent, policyIntent)) {
                LOGGER.trace("Intents don't match, skipping policy {}", synchronizationPolicy);
                return false;
            }
        } else {
            if (!MiscSchemaUtil.equalsIntent(intent, policyIntent)) {
                LOGGER.trace("Intents don't match, skipping policy {}", synchronizationPolicy);
                return false;
            }
        }

        return true;
    }

    public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent,
            ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource) throws SchemaException {
        return isPolicyApplicable(objectClass, kind, intent, synchronizationPolicy, resource, false);
    }
}
