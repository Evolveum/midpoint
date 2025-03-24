package com.evolveum.midpoint.model.api.visualizer.localization;

/**
 * Interface providing mutable combination of result of previous combination with a new element.
 *
 * Combination is mutable in a sense, that the returned result is kind of a container which accumulate all the elements.
 * @param <T>
 * @param <R>
 */
public interface LocalizationPartsCombiner<T, R> {
    R combine(R accumulatedParts, T localizationPart);
}
