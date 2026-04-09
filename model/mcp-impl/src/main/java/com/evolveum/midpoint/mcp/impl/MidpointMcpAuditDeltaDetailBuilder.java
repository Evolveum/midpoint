/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Builds structured attribute-level summaries from {@link ObjectDeltaType} beans stored in audit records.
 */
final class MidpointMcpAuditDeltaDetailBuilder {

    record AttributeChangesResult(List<Map<String, Object>> rows, boolean truncated) {}

    private static final Trace LOGGER = TraceManager.getTrace(MidpointMcpAuditDeltaDetailBuilder.class);

    private static final String REDACTED = "***";

    /** Max rows appended per object delta (item-level modifications + summarized add-object rows). */
    private static final int MAX_ROWS_PER_OBJECT_DELTA = 200;

    /** Max characters per old/new string (each side truncated independently). */
    private static final int MAX_VALUE_CHARS = 4000;

    private MidpointMcpAuditDeltaDetailBuilder() {}

    /**
     * @return rows with keys path, modificationType, oldValue, newValue (string or null); truncated if capped
     */
    static AttributeChangesResult buildAttributeChanges(ObjectDeltaType delta) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (delta == null) {
            return new AttributeChangesResult(rows, false);
        }
        ChangeTypeType changeType = delta.getChangeType();
        if (changeType == ChangeTypeType.ADD && delta.getObjectToAdd() != null) {
            return appendRowsForObjectToAdd(delta.getObjectToAdd(), rows);
        }
        if (changeType == ChangeTypeType.MODIFY) {
            boolean truncated = false;
            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                if (rows.size() >= MAX_ROWS_PER_OBJECT_DELTA) {
                    truncated = true;
                    break;
                }
                appendRowForItemDelta(itemDelta, rows);
            }
            return new AttributeChangesResult(rows, truncated);
        }
        return new AttributeChangesResult(rows, false);
    }

    private static AttributeChangesResult appendRowsForObjectToAdd(
            com.evolveum.prism.xml.ns._public.types_3.ObjectType objectToAdd, List<Map<String, Object>> rows) {
        boolean truncated = false;
        try {
            var prism = objectToAdd.asPrismObject();
            for (Object element : prism.getValue().getItems()) {
                if (!(element instanceof Item<?, ?> item)) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS_PER_OBJECT_DELTA) {
                    truncated = true;
                    break;
                }
                ItemPath path = ItemPath.create(item.getElementName());
                if (isOmittedDeltaDetailPath(path) || isMetadataItemPath(path)) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("path", path.toString());
                row.put("modificationType", "ADD");
                row.put("oldValue", null);
                if (isSensitiveItemPath(path)) {
                    row.put("newValue", REDACTED);
                } else if (item instanceof PrismProperty<?> prop) {
                    row.put("newValue", joinFormatted(prop.getRealValues(), path));
                } else if (item instanceof PrismReference ref) {
                    row.put("newValue", joinFormatted(ref.getValues(), path));
                } else if (item instanceof PrismContainer<?> cont) {
                    row.put("newValue", summarizeContainer(cont));
                } else {
                    row.put("newValue", truncate(PrettyPrinter.prettyPrint(item)));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not summarize objectToAdd: {}", e.getMessage());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", "(object)");
            row.put("modificationType", "ADD");
            row.put("oldValue", null);
            row.put("newValue", "[unavailable: " + e.getClass().getSimpleName() + "]");
            rows.add(row);
        }
        return new AttributeChangesResult(rows, truncated);
    }

    private static String summarizeContainer(PrismContainer<?> cont) {
        int n = cont.size();
        return n + " container value(s)";
    }

    private static void appendRowForItemDelta(ItemDeltaType itemDelta, List<Map<String, Object>> rows) {
        if (itemDelta.getPath() == null) {
            return;
        }
        ItemPath path = itemDelta.getPath().getItemPath();
        if (path == null || isOmittedDeltaDetailPath(path) || isMetadataItemPath(path)) {
            return;
        }
        ModificationTypeType modType = itemDelta.getModificationType();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("path", path.toString());
        row.put("modificationType", modType != null ? modType.name() : null);

        boolean sensitive = isSensitiveItemPath(path);
        if (sensitive) {
            row.put("oldValue", REDACTED);
            row.put("newValue", REDACTED);
            rows.add(row);
            return;
        }

        String oldJoined = joinRawValues(itemDelta.getEstimatedOldValue(), path);
        if (modType == ModificationTypeType.REPLACE) {
            row.put("oldValue", StringUtils.isNotBlank(oldJoined) ? oldJoined : null);
            row.put("newValue", joinRawValues(itemDelta.getValue(), path));
        } else if (modType == ModificationTypeType.ADD) {
            row.put("oldValue", StringUtils.isNotBlank(oldJoined) ? oldJoined : null);
            row.put("newValue", joinRawValues(itemDelta.getValue(), path));
        } else if (modType == ModificationTypeType.DELETE) {
            row.put("oldValue", joinRawValues(itemDelta.getValue(), path));
            row.put("newValue", null);
        } else {
            row.put("oldValue", StringUtils.isNotBlank(oldJoined) ? oldJoined : null);
            row.put("newValue", joinRawValues(itemDelta.getValue(), path));
        }
        rows.add(row);
    }

    private static String joinRawValues(Collection<RawType> rawTypes, ItemPath itemPath) {
        if (rawTypes == null || rawTypes.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (RawType raw : rawTypes) {
            if (raw == null) {
                continue;
            }
            String s = formatRawOrValue(itemPath, raw);
            if (StringUtils.isNotBlank(s)) {
                parts.add(s);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return truncate(String.join("; ", parts));
    }

    private static String joinFormatted(Collection<?> values, ItemPath itemPath) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Object value : values) {
            String s = formatDeltaValue(itemPath, value);
            if (StringUtils.isNotBlank(s)) {
                parts.add(s);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return truncate(String.join("; ", parts));
    }

    private static String formatRawOrValue(ItemPath itemPath, Object value) {
        return formatDeltaValue(itemPath, value);
    }

    private static String formatDeltaValue(ItemPath itemPath, Object value) {
        if (value instanceof PrismValue pv) {
            value = pv.getRealValue();
        }
        if (value instanceof MetadataType) {
            return "";
        }
        if (value instanceof RawType raw) {
            try {
                Object parsed = raw.getParsedRealValue(null, itemPath);
                if (parsed instanceof Containerable c) {
                    return truncate(PrettyPrinter.prettyPrint(c.asPrismContainerValue()));
                }
                return truncate(PrettyPrinter.prettyPrint(parsed));
            } catch (Exception e) {
                LOGGER.trace("Could not parse RawType at {}: {}", itemPath, e.getMessage());
                return truncate("[unparsed: " + e.getClass().getSimpleName() + "]");
            }
        }
        return truncate(PrettyPrinter.prettyPrint(value));
    }

    /**
     * Focal iteration bookkeeping — omit from MCP audit delta rows (noise for human readers).
     */
    private static boolean isOmittedDeltaDetailPath(ItemPath path) {
        if (path == null || path.size() != 1) {
            return false;
        }
        Object seg = path.first();
        if (!ItemPath.isName(seg)) {
            return false;
        }
        var name = ItemPath.toName(seg);
        return AssignmentHolderType.F_ITERATION.equals(name)
                || AssignmentHolderType.F_ITERATION_TOKEN.equals(name);
    }

    private static boolean isMetadataItemPath(ItemPath path) {
        if (path == null) {
            return false;
        }
        for (Object seg : path.getSegments()) {
            if (ItemPath.isName(seg)
                    && ObjectType.F_METADATA.getLocalPart().equals(ItemPath.toName(seg).getLocalPart())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSensitiveItemPath(ItemPath path) {
        if (path == null) {
            return false;
        }
        for (Object seg : path.getSegments()) {
            if (!ItemPath.isName(seg)) {
                continue;
            }
            String lp = ItemPath.toName(seg).getLocalPart().toLowerCase(Locale.ROOT);
            if ("credentials".equals(lp)
                    || "password".equals(lp)
                    || "protectedstring".equals(lp)
                    || lp.contains("password")
                    || lp.contains("secret")
                    || lp.contains("passphrase")
                    || lp.endsWith("token")) {
                return true;
            }
        }
        return false;
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= MAX_VALUE_CHARS) {
            return s;
        }
        return s.substring(0, MAX_VALUE_CHARS) + "…";
    }
}
