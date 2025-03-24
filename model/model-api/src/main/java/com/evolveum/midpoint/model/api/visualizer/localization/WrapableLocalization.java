package com.evolveum.midpoint.model.api.visualizer.localization;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface WrapableLocalization<T, C> {

    <R> LocalizableObject<R> combineParts(R acumulator, LocalizationPartsCombiner<? super T, R> combiner);

    <R> WrapableLocalization<R, C> wrap(LocalizationPartsWrapper<? super T, C, ? extends R> wrapper);

    @SafeVarargs
    static <C> WrapableLocalization<String, C> of(LocalizationPart<C>... parts) {
        return new WrapableLocalizationImpl<>(List.of(parts),
                LocalizationPartsWrapper.from(initialIdentityWrapper(), initialIdentityWrapper(),
                        initialIdentityWrapper(), Function.identity()));
    }

    private static <C> BiFunction<String, C, String> initialIdentityWrapper() {
        return (value, context) -> value;
    }
}
