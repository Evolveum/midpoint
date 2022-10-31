/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Prepares text output for notification purposes.
 *
 * This is only a facade for value and delta formatters.
 * It is probably used in various notification-related expressions so we'll keep it for some time.
 */
@Component
public class TextFormatter {

    public static final List<ItemPath> SYNCHRONIZATION_PATHS = List.of(
            ShadowType.F_SYNCHRONIZATION_SITUATION,
            ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION,
            ShadowType.F_SYNCHRONIZATION_TIMESTAMP,
            ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP);

    public static final List<ItemPath> AUXILIARY_PATHS = List.of(
            ShadowType.F_METADATA,
            ShadowType.F_ACTIVATION.append(ActivationType.F_VALIDITY_STATUS), // works for user activation as well
            ShadowType.F_ACTIVATION.append(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_EFFECTIVE_STATUS),
            ShadowType.F_ACTIVATION.append(ActivationType.F_DISABLE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_ARCHIVE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_ENABLE_TIMESTAMP),
            ShadowType.F_ITERATION,
            ShadowType.F_ITERATION_TOKEN,
            FocusType.F_LINK_REF,
            ShadowType.F_TRIGGER);

    @Autowired ValueFormatter valueFormatter;
    @Autowired DeltaFormatter deltaFormatter;

    static boolean isAmongHiddenPaths(ItemPath path, Collection<ItemPath> hiddenPaths) {
        if (hiddenPaths == null) {
            return false;
        }
        for (ItemPath hiddenPath : hiddenPaths) {
            if (hiddenPath.isSubPathOrEquivalent(path)) {
                return true;
            }
        }
        return false;
    }

    public String formatShadowAttributes(ShadowType shadowType, boolean showSynchronizationItems, boolean showAuxiliaryItems) {
        Collection<ItemPath> hiddenAttributes = getHiddenPaths(showSynchronizationItems, showAuxiliaryItems);
        return valueFormatter.formatAccountAttributes(shadowType, hiddenAttributes, showAuxiliaryItems);
    }

    private Collection<ItemPath> getHiddenPaths(boolean showSynchronizationItems, boolean showAuxiliaryAttributes) {
        List<ItemPath> hiddenPaths = new ArrayList<>();
        if (!showSynchronizationItems) {
            hiddenPaths.addAll(TextFormatter.SYNCHRONIZATION_PATHS);
        }
        if (!showAuxiliaryAttributes) {
            hiddenPaths.addAll(TextFormatter.AUXILIARY_PATHS);
        }
        return hiddenPaths;
    }

    public String formatObject(PrismObject<?> object, boolean showSynchronizationAttributes, boolean showAuxiliaryAttributes) {
        Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes, showAuxiliaryAttributes);
        return formatObject(object, hiddenPaths, showAuxiliaryAttributes);
    }

    public String formatObject(PrismObject<?> object, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        return valueFormatter.formatObject(object, hiddenPaths, showOperationalAttributes);
    }

    @SuppressWarnings("unused")
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes) {
        return deltaFormatter.formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, null, null);
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, boolean showSynchronizationAttributes,
            boolean showAuxiliaryAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes, showAuxiliaryAttributes);
        return formatObjectModificationDelta(objectDelta, hiddenPaths, showAuxiliaryAttributes, objectOld, objectNew);
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        return deltaFormatter.formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, objectOld, objectNew);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean containsVisibleModifiedItems(Collection<? extends ItemDelta<?, ?>> modifications,
            boolean showSynchronizationAttributes, boolean showAuxiliaryAttributes) {
        Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes, showAuxiliaryAttributes);
        return deltaFormatter.containsVisibleModifiedItems(modifications, hiddenPaths, showAuxiliaryAttributes);
    }
}
