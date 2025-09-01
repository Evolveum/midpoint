package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.web.component.util.SerializableFunction;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadCorrelationTypeSuggestion;

public abstract class StatusAwareDataProvider<C extends Containerable>
        extends MultivalueContainerListDataProvider<C> {

    private final Map<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionByWrapper =
            new IdentityHashMap<>();

    private final SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver;

    public StatusAwareDataProvider(
            Component component,
            @NotNull IModel<Search<C>> search,
            IModel<List<PrismContainerValueWrapper<C>>> model,
            SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver) {

        super(component, search, model);
        this.suggestionResolver = Objects.requireNonNull(suggestionResolver, "suggestionResolver must not be null");
    }

    @Override
    protected void postProcessWrapper(@NotNull PrismContainerValueWrapper<C> valueWrapper) {
        StatusInfo<?> info = suggestionResolver.apply(valueWrapper);
        if (info != null) {
            suggestionByWrapper.put(valueWrapper, info);
        }
    }

    public boolean isSuggestion(@NotNull PrismContainerValueWrapper<C> wrapper) {
        return suggestionByWrapper.containsKey(wrapper.getRealValue());
    }

    public StatusInfo<?> getSuggestionInfo(PrismContainerValueWrapper<C> value) {
        Task task = getPageBase().createSimpleTask("Load correlation suggestion");
        StatusInfo<?> statusInfo = suggestionByWrapper.get(value);
        return statusInfo == null
                ? null
                : loadCorrelationTypeSuggestion(getPageBase(), statusInfo.getToken(), getResourceOid(), task, task.getResult());
    }

    protected abstract String getResourceOid();

    @Override
    public void clearCache() {
        super.clearCache();
        suggestionByWrapper.clear();
    }
}
