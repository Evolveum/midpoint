/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowBehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.BehaviorCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LastLoginTimestampCapabilityType;

public class BehaviorConverter {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationConverter.class);

    /**
     * This map is just to cache instances of {@link SimpleDateFormat} since they are expensive to create.
     */
    private static final Map<String, DateFormat> LAST_LOGIN_TIMESTAMP_DATE_FORMAT = new HashMap<>();

    @NotNull private final ProvisioningContext ctx;

    BehaviorConverter(@NotNull ProvisioningContext ctx) {
        this.ctx = ctx;
    }

    private static synchronized DateFormat getLastLoginTimestampDateFormat(String format) {
        return LAST_LOGIN_TIMESTAMP_DATE_FORMAT.computeIfAbsent(format, f -> new SimpleDateFormat(f));
    }

    //region Resource object -> midPoint (simulating/native -> behavior)

    /**
     * Completes behavior for fetched object by determining simulated values if necessary.
     */
    void completeBehavior(ResourceObjectShadow resourceObject, OperationResult result) {
        ShadowType resourceObjectBean = resourceObject.getBean();

        BehaviorCapabilityType behaviorCapability = ctx.getCapability(BehaviorCapabilityType.class);

        if (!CapabilityUtil.isCapabilityEnabled(behaviorCapability) && resourceObjectBean.getBehavior() == null) {
            LOGGER.trace("No behavior capability and also no behavior information in the resource object.");
            return;
        }

        XMLGregorianCalendar lastLoginTimestamp = determineLastLoginTimestamp(resourceObject, behaviorCapability, result);
        if (lastLoginTimestamp != null) {
            if (resourceObjectBean.getBehavior() == null) {
                resourceObjectBean.setBehavior(new ShadowBehaviorType());
            }
            resourceObjectBean.getBehavior().setLastLoginTimestamp(lastLoginTimestamp);
        } else {
            if (resourceObjectBean.getBehavior() != null) {
                resourceObjectBean.getBehavior().setLastLoginTimestamp(null);
            }
        }
    }

    private XMLGregorianCalendar determineLastLoginTimestamp(
            ResourceObjectShadow resourceObject, BehaviorCapabilityType behaviorCapability, OperationResult result) {

        ShadowType shadow = resourceObject.getBean();

        XMLGregorianCalendar nativeValue = shadow.getBehavior() != null ? shadow.getBehavior().getLastLoginTimestamp() : null;

        LastLoginTimestampCapabilityType lastLoginTimestampCapability = CapabilityUtil.getEnabledLastLoginCapabilityStrict(behaviorCapability);
        if (lastLoginTimestampCapability == null) {
            if (nativeValue != null) {
                LOGGER.trace("The lastLoginTimestamp capability is disabled. Ignoring native value: {}", nativeValue);
            }
            return null;
        }

        if (lastLoginTimestampCapability.getAttribute() == null) {
            LOGGER.trace("Simulated lastLoginTimestamp is not configured. Using native value: {}", nativeValue);
            return nativeValue;
        }

        Collection<Object> values = getSimulatingAttributeValues(resourceObject, lastLoginTimestampCapability.getAttribute());
        if (values == null) {
            return null;
        }

        List<Object> filteredValues = values.stream().filter(Objects::nonNull).toList();
        if (filteredValues.isEmpty()) {
            return null;
        }

        if (filteredValues.size() > 1) {
            LOGGER.warn("An object on {} has last login timestamp values for simulated {} attribute, expecting just one value",
                    ctx.getResource(), filteredValues.size());
            result.setPartialError(
                    "An object on " + ctx.getResource() + " has last login timestamp values for simulated "
                            + filteredValues.size() + " attribute, expecting just one value");
        }

        Object value = filteredValues.get(0);
        String format = lastLoginTimestampCapability.getFormat();
        if (format != null) {
            DateFormat df = getLastLoginTimestampDateFormat(format);
            try {
                value = df.parse(value.toString()).getTime();
            } catch (ParseException ex) {
                LOGGER.warn("An object on {} has last login timestamp values for simulated attribute, wrong format: {}",
                        ctx.getResource(), ex.getMessage());
                result.setPartialError(
                        "An object on " + ctx.getResource()
                                + " has last login timestamp values for simulated attribute, wrong format: " + ex.getMessage());
            }
        }

        if (!Boolean.FALSE.equals(lastLoginTimestampCapability.isIgnoreAttribute())) {
            removeSimulatingAttribute(resourceObject, lastLoginTimestampCapability.getAttribute());
        }

        if (value instanceof Long l) {
            return XmlTypeConverter.createXMLGregorianCalendar(l);
        }
        if (value instanceof XMLGregorianCalendar cal) {
            return cal;
        }

        LOGGER.warn(
                "An object on {} has value for simulated last login timestamp attribute of wrong type {} expecting "
                        + "Long/XMLGregorianCalendar. Probably missing format in the configured capability.",
                ctx.getResource(), value.getClass().getName());
        result.setPartialError("An object on " + ctx.getResource()
                + " has value for simulated last login timestamp attribute of wrong type " + value.getClass().getName()
                + " expecting Long/XMLGregorianCalendar. Probably missing format in the configured capability.");
        return null;
    }

    private void removeSimulatingAttribute(ResourceObjectShadow resourceObject, QName attributeName) {
        ShadowAttributesContainer attributesContainer = resourceObject.getAttributesContainer();
        attributesContainer.removeProperty(ItemPath.create(attributeName));
    }

    @Nullable
    private Collection<Object> getSimulatingAttributeValues(ResourceObjectShadow resourceObject, QName attributeName) {
        ShadowAttributesContainer attributesContainer = resourceObject.getAttributesContainer();
        ShadowSimpleAttribute<?> simulatingAttribute = attributesContainer.findSimpleAttribute(attributeName);
        return simulatingAttribute != null ? simulatingAttribute.getRealValues(Object.class) : null;
    }
}
