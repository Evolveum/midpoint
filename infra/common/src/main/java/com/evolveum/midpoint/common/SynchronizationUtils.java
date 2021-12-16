/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.*;

import static java.util.Objects.requireNonNullElse;

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

    /**
     * Checks if the synchronization policy matches given "parameters" (object class, kind, intent).
     */
    public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent,
            @NotNull ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource, boolean strictIntent)
            throws SchemaException {

        if (objectClassDefinedAndNotMatching(objectClass, synchronizationPolicy.getObjectClass())) {
            return false;
        }

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        Objects.requireNonNull(schema, "No schema defined in resource. Possible configuration problem?");

        ShadowKindType policyKind = requireNonNullElse(synchronizationPolicy.getKind(), ACCOUNT);

        String policyIntent = synchronizationPolicy.getIntent();

        ResourceObjectDefinition policyObjectClass;
        if (StringUtils.isEmpty(policyIntent)) {
            policyObjectClass = schema.findObjectDefinition(policyKind, null); // TODO check this
            if (policyObjectClass instanceof ResourceObjectTypeDefinition) {
                policyIntent = ((ResourceObjectTypeDefinition) policyObjectClass).getIntent();
            }
        } else {
            policyObjectClass = schema.findObjectDefinition(policyKind, policyIntent);
        }

        if (policyObjectClass == null) {
            return false;
        }

        // re-check objectClass if wasn't defined
        if (objectClassDefinedAndNotMatching(objectClass, List.of(policyObjectClass.getTypeName()))) {
            return false;
        }

        // kind
        LOGGER.trace("Comparing kinds, policy kind: {}, current kind: {}", policyKind, kind);
        if (kind != null && kind != UNKNOWN && !policyKind.equals(kind)) {
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

    private static boolean objectClassDefinedAndNotMatching(@Nullable QName objectClass, @NotNull List<QName> policyObjectClasses) {
        return objectClass != null &&
                !policyObjectClasses.isEmpty() &&
                !QNameUtil.matchAny(objectClass, policyObjectClasses);
    }

    public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent,
            ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource) throws SchemaException {
        return isPolicyApplicable(objectClass, kind, intent, synchronizationPolicy, resource, false);
    }
}
