/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Wrapper storage for the resource details page, which holds information about the last stored/read resource.
 *
 * Holds one {@link ResourceContentStorage} per shadow kind and search mode. The individual content storages carry
 * the actual search and paging state. The stored content is discarded whenever the user switches to a different
 * resource (see {@link #prepareForResource(String)}).
 *
 * NOTE: This class does not support any {@link PageStorage} methods and throws an exception on each of them.
 */
public class ResourceDetailsPageStorage implements PageStorage {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, ResourceContentStorage> contentStorages;
    private String lastResource;

    public ResourceDetailsPageStorage() {
        this.contentStorages = new HashMap<>();
    }

    /**
     * Returns the content storage for the given shadow kind and search mode, creating it on demand.
     *
     * @deprecated Use {@link #getResourceContentStorage(ShadowKindType)} for repository search. This overload is
     * kept for callers that must distinguish the resource search mode.
     */
    @Deprecated
    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind, String searchMode) {
        final String key = getContentStorageKey(kind, searchMode);
        return this.contentStorages.computeIfAbsent(key, k -> new ResourceContentStorage(kind));
    }

    /**
     * Returns the content storage for the given shadow kind (repository search), creating it on demand.
     */
    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind) {
        final String key = getContentStorageKey(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        return this.contentStorages.computeIfAbsent(key, k -> new ResourceContentStorage(kind));
    }

    /**
     * Prepares this storage for the given resource, discarding any content stored for a previous resource.
     *
     * @param oid The OID of the resource being displayed.
     */
    public void prepareForResource(String oid) {
        if (this.lastResource != null && !this.lastResource.equals(oid)) {
            this.contentStorages.clear();
        }
        this.lastResource = oid;
    }

    /**
     * Throws UnsupportedOperationException.
     */
    @Override
    public Search getSearch() {
        throw new UnsupportedOperationException("This storage does not support search data");
    }

    /**
     * Throws UnsupportedOperationException.
     */
    @Override
    public void setSearch(Search search) {
        throw new UnsupportedOperationException("This storage does not support search data");
    }

    /**
     * Throws UnsupportedOperationException.
     */
    @Override
    public void setPaging(ObjectPaging paging) {
        throw new UnsupportedOperationException("This storage does not support paging");
    }

    /**
     * Throws UnsupportedOperationException.
     */
    @Override
    public ObjectPaging getPaging() {
        throw new UnsupportedOperationException("This storage does not support paging");
    }

    @Override
    public String debugDump(int indent) {
        final StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceDetailsPageStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "lastResource", this.lastResource, indent + 1);
        for (Map.Entry<String, ResourceContentStorage> entry : this.contentStorages.entrySet()) {
            DebugUtil.debugDumpWithLabelLn(sb, entry.getKey(), entry.getValue(), indent + 1);
        }
        return sb.toString();
    }

    private String getContentStorageKey(ShadowKindType kind, String searchMode) {
        if (kind == null) {
            return SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;
        }

        return switch (kind) {
            case ACCOUNT -> SessionStorage.KEY_RESOURCE_ACCOUNT_CONTENT + searchMode;
            case ENTITLEMENT -> SessionStorage.KEY_RESOURCE_ENTITLEMENT_CONTENT + searchMode;
            case GENERIC -> SessionStorage.KEY_RESOURCE_GENERIC_CONTENT + searchMode;
            case WORK -> SessionStorage.KEY_RESOURCE_WORK_CONTENT + searchMode;
            default -> SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;
        };
    }

}
