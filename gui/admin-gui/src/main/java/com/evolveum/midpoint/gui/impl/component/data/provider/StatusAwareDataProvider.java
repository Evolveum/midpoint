package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.web.component.util.SerializableFunction;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    private final Map<String, StatusInfo<?>> statusByToken = new HashMap<>();

    /** Cache of wrappers mapped to their status token (identity-based). */
    private final Map<PrismContainerValueWrapper<C>, String> tokenByWrapper = new IdentityHashMap<>();

    private final SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver;

    private final String resourceOid;

    public StatusAwareDataProvider(
            Component component,
            String resourceOid,
            @NotNull IModel<Search<C>> search,
            IModel<List<PrismContainerValueWrapper<C>>> model,
            SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver) {
        super(component, search, model);
        this.resourceOid = resourceOid;
        this.suggestionResolver = Objects.requireNonNull(suggestionResolver, "suggestionResolver must not be null");
    }

    @Override
    protected void postProcessWrapper(@NotNull PrismContainerValueWrapper<C> vw) {
        StatusInfo<?> info = suggestionResolver.apply(vw);
        if (info != null) {
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
            if (info == null) {return null;}
            token = info.getToken();
            tokenByWrapper.put(vw, token);
            statusByToken.putIfAbsent(token, info);
        }
        Task task = getPageBase().createSimpleTask("Load correlation suggestion");
        SmartIntegrationService smart = getPageBase().getSmartIntegrationService();
        try {
            return smart.getSuggestCorrelationOperationStatus(token, task, task.getResult());
        } catch (Throwable e) {
            getPageBase().error("Couldn't get correlation suggestion status: " + e.getMessage());
            return null;
        }
    }

    protected String getResourceOid() {
        return resourceOid;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        tokenByWrapper.clear();
        statusByToken.clear();
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
}
