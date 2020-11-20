/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.S_MaybeDelete;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SynchronizationUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationUtils.class);

    private static PropertyDelta<SynchronizationSituationType> createSynchronizationSituationDelta(
            PrismObject<ShadowType> shadow, SynchronizationSituationType situation,
            PrismContext prismContext) {

        if (situation == null) {
            SynchronizationSituationType oldValue = shadow.asObjectable().getSynchronizationSituation();
            return prismContext.deltaFactory().property().createModificationDeleteProperty(ShadowType.F_SYNCHRONIZATION_SITUATION, shadow.getDefinition(), oldValue);
        }

        return prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_SYNCHRONIZATION_SITUATION, shadow.getDefinition(), situation);
    }

    private static PropertyDelta<XMLGregorianCalendar> createSynchronizationTimestampDelta(PrismObject<ShadowType> object,
            QName propName, XMLGregorianCalendar timestamp, PrismContext prismContext) {
        PropertyDelta<XMLGregorianCalendar> syncSituationDelta = prismContext.deltaFactory().property()
                .createReplaceDelta(object.getDefinition(), propName, timestamp);
        return syncSituationDelta;
    }

    public static List<PropertyDelta<?>> createSynchronizationSituationAndDescriptionDelta(PrismObject<ShadowType> shadow,
            SynchronizationSituationType situation, String sourceChannel, boolean full, XMLGregorianCalendar timestamp,
            PrismContext prismContext) throws SchemaException {

        List<PropertyDelta<?>> propertyDeltas = new ArrayList<>();

        PropertyDelta<SynchronizationSituationDescriptionType> syncDescriptionDelta = createSynchronizationSituationDescriptionDelta(shadow, situation,
                timestamp, sourceChannel, full, prismContext);
        propertyDeltas.add(syncDescriptionDelta);

        propertyDeltas.addAll(createSynchronizationTimestampsDelta(shadow, timestamp, full, prismContext));

        PropertyDelta<SynchronizationSituationType> syncSituationDelta = createSynchronizationSituationDelta(shadow, situation, prismContext);
        propertyDeltas.add(syncSituationDelta);

        return propertyDeltas;
    }

    private static PropertyDelta<SynchronizationSituationDescriptionType> createSynchronizationSituationDescriptionDelta(
            PrismObject<ShadowType> shadow,
            SynchronizationSituationType situation, XMLGregorianCalendar timestamp, String sourceChannel,
            boolean full, PrismContext prismContext) throws SchemaException {
        SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
        syncSituationDescription.setSituation(situation);
        syncSituationDescription.setChannel(sourceChannel);
        syncSituationDescription.setTimestamp(timestamp);
        syncSituationDescription.setFull(full);

        S_MaybeDelete builder = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION).add(syncSituationDescription);

        List<SynchronizationSituationDescriptionType> oldSituationDescriptions = getSituationFromSameChannel(
                shadow, sourceChannel);
        if (CollectionUtils.isNotEmpty(oldSituationDescriptions)) {
            builder.deleteRealValues(oldSituationDescriptions);
        }

        return (PropertyDelta<SynchronizationSituationDescriptionType>) builder.asItemDelta();
    }

    public static List<PropertyDelta<?>> createSynchronizationTimestampsDelta(
            PrismObject<ShadowType> shadow, PrismContext prismContext) {
        XMLGregorianCalendar timestamp = XmlTypeConverter
                .createXMLGregorianCalendar(System.currentTimeMillis());
        return createSynchronizationTimestampsDelta(shadow, timestamp, true, prismContext);
    }

    private static List<PropertyDelta<?>> createSynchronizationTimestampsDelta(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar timestamp, boolean full, PrismContext prismContext) {

        List<PropertyDelta<?>> deltas = new ArrayList<>();
        PropertyDelta<XMLGregorianCalendar> timestampDelta = createSynchronizationTimestampDelta(shadow,
                ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestamp, prismContext);
        deltas.add(timestampDelta);

        if (full) {
            timestampDelta = createSynchronizationTimestampDelta(shadow,
                    ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, timestamp, prismContext);
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

    public static boolean isPolicyApplicable(QName objectClass, ShadowKindType kind, String intent, ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource, boolean strictIntent) throws SchemaException {

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

        ObjectClassComplexTypeDefinition policyObjectClass = null;
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
