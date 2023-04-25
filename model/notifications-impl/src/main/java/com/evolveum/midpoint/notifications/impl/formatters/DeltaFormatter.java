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
import java.util.Locale;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.notifications.impl.NotificationFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Formats object deltas and item deltas for notification purposes.
 */
@Component
public class DeltaFormatter {

    @Autowired private ValueFormatter valueFormatter;
    @Autowired protected NotificationFunctions functions;
    @Autowired private LocalizationService localizationService;

    private static final Trace LOGGER = TraceManager.getTrace(DeltaFormatter.class);

    @SuppressWarnings("unused")
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes) {
        return formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, null, null);
    }

    // objectOld and objectNew are used for explaining changed container values, e.g. assignment[1]/tenantRef (see MID-2047)
    // if null, they are ignored
    String formatObjectModificationDelta(@NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        Validate.isTrue(objectDelta.isModify(), "objectDelta is not a modification delta");

        PrismObjectDefinition<?> objectDefinition;
        if (objectNew != null && objectNew.getDefinition() != null) {
            objectDefinition = objectNew.getDefinition();
        } else if (objectOld != null && objectOld.getDefinition() != null) {
            objectDefinition = objectOld.getDefinition();
        } else {
            objectDefinition = null;
        }

        LOGGER.trace("formatObjectModificationDelta: objectDelta:\n{}\n\nhiddenPaths:\n{}",
                objectDelta.debugDumpLazily(), PrettyPrinter.prettyPrintLazily(hiddenPaths));

        StringBuilder sb = new StringBuilder();

        List<ItemDelta<?, ?>> visibleDeltas = filterAndOrderModifications(objectDelta, hiddenPaths, showOperationalAttributes);
        for (ItemDelta<?, ?> itemDelta : visibleDeltas) {
            sb.append(" - ");
            sb.append(getItemDeltaLabel(itemDelta, objectDefinition));
            sb.append(":\n");
            formatItemDeltaContent(sb, itemDelta, objectOld, hiddenPaths, showOperationalAttributes);
        }

        explainPaths(sb, visibleDeltas, objectDefinition, objectOld, objectNew, hiddenPaths, showOperationalAttributes);

        return sb.toString();
    }

    private void explainPaths(StringBuilder sb, List<ItemDelta<?, ?>> deltas, PrismObjectDefinition<?> objectDefinition,
            PrismObject<?> objectOld, PrismObject<?> objectNew, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (objectOld == null && objectNew == null) {
            return; // no data - no point in trying
        }
        boolean first = true;
        List<ItemPath> alreadyExplained = new ArrayList<>();
        for (ItemDelta<?, ?> itemDelta : deltas) {
            ItemPath pathToExplain = getPathToExplain(itemDelta);
            if (pathToExplain == null || ItemPathCollectionsUtil.containsSubpathOrEquivalent(alreadyExplained, pathToExplain)) {
                continue; // null or already processed
            }
            PrismObject<?> source = null;
            Object item = null;
            if (objectNew != null) {
                item = objectNew.find(pathToExplain);
                source = objectNew;
            }
            if (item == null && objectOld != null) {
                item = objectOld.find(pathToExplain);
                source = objectOld;
            }
            if (item == null) {
                LOGGER.warn("Couldn't find {} in {} nor {}, no explanation could be created.", pathToExplain, objectNew, objectOld);
                continue;
            }
            if (first) {
                sb.append("\nNotes:\n");
                first = false;
            }
            String label = getItemPathLabel(pathToExplain, itemDelta.getDefinition(), objectDefinition);
            // the item should be a PrismContainerValue
            if (item instanceof PrismContainerValue) {
                sb.append(" - ").append(label).append(":\n");
                valueFormatter.formatContainerValue(sb, "   ", (PrismContainerValue<?>) item, false, hiddenPaths, showOperationalAttributes);
            } else {
                LOGGER.warn("{} in {} was expected to be a PrismContainerValue; it is {} instead", pathToExplain, source, item.getClass());
                if (item instanceof PrismContainer) {
                    valueFormatter.formatPrismContainer(sb, "   ", (PrismContainer<?>) item, false, hiddenPaths, showOperationalAttributes);
                } else if (item instanceof PrismReference) {
                    valueFormatter.formatPrismReference(sb, "   ", (PrismReference) item, false);
                } else if (item instanceof PrismProperty) {
                    valueFormatter.formatPrismProperty(sb, "   ", (PrismProperty<?>) item);
                } else {
                    sb.append("Unexpected item: ").append(item).append("\n");
                }
            }
            alreadyExplained.add(pathToExplain);
        }
    }

    private void formatItemDeltaContent(StringBuilder sb, ItemDelta<?, ?> itemDelta, PrismObject<?> objectOld,
            Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        formatItemDeltaValues(sb, "ADD", itemDelta.getValuesToAdd(), false, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
        formatItemDeltaValues(sb, "DELETE", itemDelta.getValuesToDelete(), true, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
        formatItemDeltaValues(sb, "REPLACE", itemDelta.getValuesToReplace(), false, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
    }

    private void formatItemDeltaValues(StringBuilder sb, String type, Collection<? extends PrismValue> values,
            boolean isDelete, ItemPath path, PrismObject<?> objectOld,
            Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (values != null) {
            for (PrismValue prismValue : values) {
                sb.append("   - ").append(type).append(": ");
                String prefix = "     ";
                if (isDelete && prismValue instanceof PrismContainerValue) {
                    prismValue = fixEmptyContainerValue((PrismContainerValue<?>) prismValue, path, objectOld);
                }
                valueFormatter.formatPrismValue(sb, prefix, prismValue, isDelete, hiddenPaths, showOperationalAttributes);
                if (!(prismValue instanceof PrismContainerValue)) {         // container values already end with newline
                    sb.append("\n");
                }
            }
        }
    }

    private PrismValue fixEmptyContainerValue(PrismContainerValue<?> pcv, ItemPath path, PrismObject<?> objectOld) {
        if (pcv.getId() == null || CollectionUtils.isNotEmpty(pcv.getItems())) {
            return pcv;
        }
        PrismContainer<?> oldContainer = objectOld.findContainer(path);
        if (oldContainer == null) {
            return pcv;
        }
        PrismContainerValue<?> oldValue = oldContainer.getValue(pcv.getId());
        return oldValue != null ? oldValue : pcv;
    }

    // we call this on filtered list of item deltas - all of they have definition set
    private String getItemDeltaLabel(ItemDelta<?, ?> itemDelta, PrismObjectDefinition<?> objectDefinition) {
        return getItemPathLabel(itemDelta.getPath(), itemDelta.getDefinition(), objectDefinition);
    }

    private String getItemPathLabel(ItemPath path, Definition deltaDefinition, PrismObjectDefinition<?> objectDefinition) {

        int lastNameIndex = path.lastNameIndex();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Object segment = path.getSegment(i);
            if (ItemPath.isName(segment)) {
                if (sb.length() > 0) {
                    sb.append("/");
                }
                Definition itemDefinition;
                if (objectDefinition == null) {
                    if (i == lastNameIndex) {  // definition for last segment is the definition taken from delta
                        itemDefinition = deltaDefinition;    // this may be null but we don't care
                    } else {
                        itemDefinition = null;          // definitions for previous segments are unknown
                    }
                } else {
                    // todo we could make this iterative (resolving definitions while walking down the path); but this is definitely simpler to implement and debug :)
                    itemDefinition = objectDefinition.findItemDefinition(path.allUpToIncluding(i));
                }
                if (itemDefinition != null && itemDefinition.getDisplayName() != null) {
                    sb.append(resolve(itemDefinition.getDisplayName()));
                } else {
                    sb.append(ItemPath.toName(segment).getLocalPart());
                }
            } else if (ItemPath.isId(segment)) {
                sb.append("[").append(ItemPath.toId(segment)).append("]");
            }
        }
        return sb.toString();
    }

    private String resolve(String key) {
        if (key != null) {
            return localizationService.translate(key, null, Locale.getDefault(), key);
        } else {
            return null;
        }
    }

    // we call this on filtered list of item deltas - all of they have definition set
    private ItemPath getPathToExplain(ItemDelta<?, ?> itemDelta) {
        ItemPath path = itemDelta.getPath();

        for (int i = 0; i < path.size(); i++) {
            Object segment = path.getSegment(i);
            if (ItemPath.isId(segment)) {
                if (i < path.size() - 1 || itemDelta.isDelete()) {
                    return path.allUpToIncluding(i);
                } else {
                    // this means that the path ends with [id] segment *and* the value(s) are
                    // only added and deleted, i.e. they are shown in the delta anyway
                    // (actually it is questionable whether path in delta can end with [id] segment,
                    // but we test for this case just to be sure)
                    return null;
                }
            }
        }
        return null;
    }

    private List<ItemDelta<?, ?>> filterAndOrderModifications(ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        List<ItemDelta<?, ?>> visibleModifications = getVisibleModifications(objectDelta.getModifications(), hiddenPaths,
                showOperationalAttributes, objectDelta.debugDumpLazily());
        visibleModifications.sort((delta1, delta2) -> compareDisplayOrders(delta1.getDefinition(), delta2.getDefinition()));
        return visibleModifications;
    }

    static int compareDisplayOrders(ItemDefinition<?> definition1, ItemDefinition<?> definition2) {
        Integer order1 = definition1.getDisplayOrder();
        Integer order2 = definition2.getDisplayOrder();
        if (order1 != null && order2 != null) {
            return order1 - order2;
        } else if (order1 == null && order2 == null) {
            return 0;
        } else if (order1 == null) {
            return 1;
        } else {
            return -1;
        }
    }

    @NotNull
    private List<ItemDelta<?, ?>> getVisibleModifications(Collection<? extends ItemDelta<?, ?>> modifications,
            Collection<ItemPath> hiddenPaths, boolean showOperational, Object context) {
        List<ItemDelta<?, ?>> toBeDisplayed = new ArrayList<>(modifications.size());
        List<QName> noDefinition = new ArrayList<>();
        for (ItemDelta<?, ?> itemDelta : modifications) {
            if (itemDelta.getDefinition() != null) {
                if ((showOperational || !itemDelta.getDefinition().isOperational())
                        && !TextFormatter.isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
                    toBeDisplayed.add(itemDelta);
                }
            } else {
                noDefinition.add(itemDelta.getElementName());
            }
        }
        if (!noDefinition.isEmpty()) {
            LOGGER.error("Item deltas for {} without definition - WILL NOT BE INCLUDED IN NOTIFICATION. Context:\n{}",
                    noDefinition, context);
        }
        return toBeDisplayed;
    }

    boolean containsVisibleModifiedItems(Collection<? extends ItemDelta<?, ?>> modifications,
            Collection<ItemPath> hiddenPaths, boolean showOperational) {
        List<ItemDelta<?, ?>> visibleModifications = getVisibleModifications(modifications, hiddenPaths, showOperational,
                DebugUtil.debugDumpLazily(modifications));
        return !visibleModifications.isEmpty();
    }
}
