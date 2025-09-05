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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StatusAwareDataProvider<C extends Containerable>
        extends MultivalueContainerListDataProvider<C> {

    private final Map<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionByWrapper =
            new IdentityHashMap<>();

    private final SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver;

    String resourceOid;

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
    protected void postProcessWrapper(@NotNull PrismContainerValueWrapper<C> valueWrapper) {
        StatusInfo<?> info = suggestionResolver.apply(valueWrapper);
        if (info != null) {
            suggestionByWrapper.put(valueWrapper, info);
        }
    }

    public @Nullable StatusInfo<?> getSuggestionInfo(PrismContainerValueWrapper<C> value) {
        Task task = getPageBase().createSimpleTask("Load correlation suggestion");
        SmartIntegrationService smartService = getPageBase().getSmartIntegrationService();
        StatusInfo<?> statusInfo = suggestionByWrapper.get(value);
        if (statusInfo != null) {
            try {
                return smartService.getSuggestCorrelationOperationStatus(statusInfo.getToken(), task, task.getResult());
            } catch (Throwable e) {
                getPageBase().error("Couldn't get correlation suggestion status: " + e.getMessage());
            }
        }
        return null;
    }

    protected String getResourceOid(){
        return resourceOid;
    };

    @Override
    public void clearCache() {
        super.clearCache();
        suggestionByWrapper.clear();
    }
}
