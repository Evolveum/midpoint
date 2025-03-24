package com.evolveum.midpoint.model.api.visualizer.localization;

import java.util.List;

public class WrapableLocalizationImpl<T, C> implements WrapableLocalization<T, C> {
    private final List<LocalizationPart<C>> parts;
    private final LocalizationPartsWrapper<String, C, T> wrapper;

    WrapableLocalizationImpl(List<LocalizationPart<C>> parts, LocalizationPartsWrapper<String, C, T> wrapper) {
        this.parts = parts;
        this.wrapper = wrapper;
    }

    @Override
    public <R> LocalizableObject<R> combineParts(R accumulator,
            LocalizationPartsCombiner<? super T, R> combiner) {
        return (localizationService, locale) -> {
            R accumulatedParts = accumulator;
            for (final LocalizationPart<C> part : this.parts) {
                final T translatedPart = part.wrap(this.wrapper).translate(localizationService, locale);
                accumulatedParts = combiner.combine(accumulatedParts, translatedPart);
            }

            return accumulatedParts;
        };
    }

    @Override
    public <R> WrapableLocalization<R, C> wrap(LocalizationPartsWrapper<? super T, C, ? extends R> nextWrapper) {
        return new WrapableLocalizationImpl<>(this.parts, this.wrapper.compose(nextWrapper));

    }

}
