/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 *
 */
@Experimental
public class CompiledShadowCollectionView extends CompiledObjectCollectionView {

    private ObjectReferenceType resourceRef;
    private ShadowKindType shadowKindType;
    private String intent;

    // Only used to construct "default" view definition. May be not needed later on.
    public CompiledShadowCollectionView() {
        super();
    }

    public CompiledShadowCollectionView(String viewIdentifier) {
        super(ShadowType.COMPLEX_TYPE, viewIdentifier);
    }

    public boolean match(String resourceOid, ShadowKindType kind, String intent) {
        return resourceOid.equals(resourceRef.getOid()) && kind == shadowKindType && intent.equals(this.intent);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resourceRef", resourceRef, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowKind", shadowKindType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
        return sb.toString();
    }

    public GuiObjectListViewType toGuiObjectListViewType() {
        GuiObjectListViewType viewType = new GuiObjectListViewType();
        viewType.setIdentifier(getViewIdentifier());
        viewType.setType(getContainerType());
        viewType.setDisplay(getDisplay());
        for (GuiObjectColumnType column : getColumns()) {
            viewType.column(column.clone());
        }
        for (GuiActionType action : getActions()) {
            viewType.action(action.clone());
        }
        viewType.setDistinct(getDistinct());
        viewType.setDisableSorting(isDisableSorting());
        viewType.setDisableCounting(isDisableCounting());
        viewType.setSearchBoxConfiguration(getSearchBoxConfiguration());
        viewType.setDisplayOrder(getDisplayOrder());
        viewType.setRefreshInterval(getRefreshInterval());
        viewType.setPaging(getPaging());
        viewType.setVisibility(getVisibility());
        viewType.setApplicableForOperation(getApplicableForOperation());
        viewType.setIncludeDefaultColumns(getIncludeDefaultColumns());
        return viewType;
    }

    public void setResourceRef(ObjectReferenceType resourceRef) {
        this.resourceRef = resourceRef;
    }

    public void setShadowKindType(ShadowKindType shadowKindType) {
        this.shadowKindType = shadowKindType;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
