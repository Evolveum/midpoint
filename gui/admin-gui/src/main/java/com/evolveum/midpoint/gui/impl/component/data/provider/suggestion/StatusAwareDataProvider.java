/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider.suggestion;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SerializableFunction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data provider that augments {@link MultivalueContainerListDataProvider}
 * with awareness of {@link StatusInfo} for each wrapped value.
 * <p>
 * It maintains two caches:
 * <ul>
 *   <li>{@code tokenByWrapper} – maps each {@link PrismContainerValueWrapper}
 *       to the status token it belongs to</li>
 *   <li>{@code statusByToken} – maps a token string to the corresponding
 *       {@link StatusInfo}</li>
 * </ul>
 * These caches allow efficient lookup of suggestion status for both
 * the current page and all loaded items.
 *
 * @param <C> type of container values this provider supplies
 */
public class StatusAwareDataProvider<C extends Containerable>
        extends MultivalueContainerListDataProvider<C> {

    /** Cache of status information keyed by token. */
    protected final Map<String, StatusInfo<?>> statusByToken = new HashMap<>();
    private final Class<?> resultClass;

    /** Cache of wrappers mapped to their status token (identity-based). */
    protected final Map<PrismContainerValueWrapper<C>, String> tokenByWrapper = new IdentityHashMap<>();

    private final SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver;

    private final String resourceOid;

    private int pageSuggestionCount = 0;

    public StatusAwareDataProvider(
            @NotNull Component component,
            @NotNull IModel<Search<C>> search,
            @NotNull StatusAwareDataFactory.SuggestionsModelDto<C> suggestionsModelDto,
            @NotNull Class<?> resultClass,
            boolean sortable) {
        super(component, search, suggestionsModelDto.getModel(), sortable);
        this.resultClass = resultClass;
        this.suggestionResolver = suggestionsModelDto.getSuggestionResolver();
        this.resourceOid = suggestionsModelDto.getResourceOid();
        applyInitialSorting();
    }

    @Override
    protected void postProcessWrapper(@NotNull PrismContainerValueWrapper<C> vw) {
        StatusInfo<?> info = suggestionResolver.apply(vw);
        if (info != null) {
            this.pageSuggestionCount++;
            tokenByWrapper.put(vw, info.getToken());
            statusByToken.putIfAbsent(info.getToken(), info);
        }
    }

    /**
     * Returns the current {@link StatusInfo} for the given value wrapper.
     * <p>
     * If the wrapper is not yet cached, the {@code suggestionResolver} is applied,
     * and the token/status are stored. The status is then refreshed using the
     * {@link SmartIntegrationService}, ensuring up-to-date information.
     *
     * @param vw wrapper whose suggestion status should be resolved
     * @return the latest {@link StatusInfo}, or {@code null} if none is available
     */
    public @Nullable StatusInfo<?> getSuggestionInfo(PrismContainerValueWrapper<C> vw) {
        String token = tokenByWrapper.get(vw);
        if (token == null) {
            StatusInfo<?> info = suggestionResolver.apply(vw);
            if (info == null) {
                return null;
            }

            token = info.getToken();
            tokenByWrapper.put(vw, token);
            statusByToken.putIfAbsent(token, info);
        }

        StatusInfo<?> statusInfo = statusByToken.get(token);

        if (statusInfo == null
                || statusInfo.isExecuting()
                || OperationResultStatusType.UNKNOWN.equals(statusInfo.getStatus())) {
            return fetchStatus(token);
        }

        return statusInfo;
    }

    private @Nullable StatusInfo<?> fetchStatus(String token) {
        Task task = getPageBase().createSimpleTask("Load suggestion");
        SmartIntegrationService smart = getPageBase().getSmartIntegrationService();

        try {
            StatusInfo<?> status = loadStatusInfo(token, smart, task);
            if (status != null) {
                statusByToken.put(token, status);
            }
            return status;
        } catch (Exception e) {
            getPageBase().error("Couldn't get suggestion status: " + e.getMessage());
            return null;
        }
    }

    private @Nullable StatusInfo<?> loadStatusInfo(
            @NotNull String token,
            @NotNull SmartIntegrationService smart,
            @NotNull Task task)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        if (MappingsSuggestionType.class.equals(resultClass)) {
            return smart.getSuggestMappingsOperationStatus(token, task, task.getResult());
        }
        if (CorrelationSuggestionsType.class.equals(resultClass)) {
            return smart.getSuggestCorrelationOperationStatus(token, task, task.getResult());
        }
        if (ObjectTypesSuggestionType.class.equals(resultClass)) {
            return smart.getSuggestObjectTypesOperationStatus(token, task, task.getResult());
        }
        if (AssociationsSuggestionType.class.equals(resultClass)) {
            return smart.getSuggestAssociationsOperationStatus(token, task, task.getResult());
        }
        if (FocusTypeSuggestionType.class.equals(resultClass)) {
            return smart.getSuggestFocusTypeOperationStatus(token, task, task.getResult());
        }

        throw new IllegalStateException("Unsupported suggestion result class: " + resultClass);
    }

    protected String getResourceOid() {
        return resourceOid;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        this.tokenByWrapper.clear();
        this.statusByToken.clear();
        this.pageSuggestionCount = 0;
    }

    @Override
    public void detach() {
        super.detach();
        this.pageSuggestionCount = 0;
    }

    /** Optionally warm the cache for all rows (not just current page). */
    public void primeSuggestionCacheForAll() {
        List<PrismContainerValueWrapper<C>> all = getModel().getObject();
        if (all == null) {return;}
        for (PrismContainerValueWrapper<C> vw : all) {
            StatusInfo<?> info = suggestionResolver.apply(vw);
            if (info != null) {
                tokenByWrapper.put(vw, info.getToken());
                statusByToken.putIfAbsent(info.getToken(), info);
            }
        }
    }

    public List<PrismContainerValueWrapper<C>> getAllSelected() {
        List<PrismContainerValueWrapper<C>> all = getModel().getObject();
        if (all == null) {return List.of();}
        return all.stream().filter(PrismContainerValueWrapper::isSelected).toList();
    }

    /**
     * Configures the initial sort state for this data provider.
     * <p>
     * By default, no explicit sorting is applied ({@code setSort(null)}),
     * meaning items are presented in their natural or source-defined order.
     * <br><br>
     * Subclasses may override this method to define a custom default sort,
     * for example to prioritize suggested or existing mappings:
     * <pre>{@code
     * @Override
     * protected void applyInitialSorting() {
     *     // Sort by "name" property in ascending order
     *     setSort(new SortParam<>("name", true));
     * }
     * }</pre>
     * <p>
     * In the context of mapping tables, different item types (e.g. suggestions
     * vs. existing mappings) may define distinct default sorting strategies.
     */
    protected void applyInitialSorting() {
        this.setSort(null);
    }

    public int getPageSuggestionCount() {
        return pageSuggestionCount;
    }

    public boolean isSuggestion(@NotNull PrismContainerValueWrapper<C> wrapper) {
        if (tokenByWrapper.containsKey(wrapper)) {
            return true;
        }

        StatusInfo<?> info = suggestionResolver.apply(wrapper);
        if (info == null) {
            return false;
        }

        tokenByWrapper.put(wrapper, info.getToken());
        statusByToken.putIfAbsent(info.getToken(), info);
        return true;
    }
}
