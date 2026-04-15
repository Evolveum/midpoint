/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Stores session-level UI state for the Resource wizard.
 * Tracks which resources had Preview Data visited in the current session.
 */
public class ResourceWizardStorage implements Serializable, DebugDumpable {

    @Serial private static final long serialVersionUID = 1L;

    /** Resource OIDs for which Preview data was clicked in this session. */
    private final Set<String> previewDataClickedForResource = new HashSet<>();

    public boolean isPreviewDataClicked(@NotNull String resourceOid) {
        return previewDataClickedForResource.contains(resourceOid);
    }

    public void markPreviewDataClicked(@NotNull String resourceOid) {
        if (StringUtils.isNotBlank(resourceOid)) {
            previewDataClickedForResource.add(resourceOid);
        }
    }

    public void clearPreviewDataClicked(@NotNull String resourceOid) {
        previewDataClickedForResource.remove(resourceOid);
    }

    public void clearAll() {
        previewDataClickedForResource.clear();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceWizardStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "previewDataClickedForResource", previewDataClickedForResource, indent + 1);
        return sb.toString();
    }
}
