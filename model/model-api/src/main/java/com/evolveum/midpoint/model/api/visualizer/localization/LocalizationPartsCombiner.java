package com.evolveum.midpoint.model.api.visualizer.localization;

import java.io.Serializable;
import java.util.stream.Collector;

/**
 * Interface providing mutable combination of previous combination result with a new element.
 *
 * Combination is sort of a mutable reduction operation. That means, the returned result as well as previously combined
 * results are some kind of mutable container which accumulates all the elements (e.g. {@link StringBuilder} or
 * {@link java.util.Collection} etc.).
 *
 * @param <T> Type of the element which should be combined with previous combination results
 * @param <R> Type of the results, i.e. type of the accumulating container.
 */
public interface LocalizationPartsCombiner<T, R> extends Serializable {
    R combine(R accumulatedParts, T localizationPart);

    /**
     * Creates collector, which joins strings with space.
     *
     * Strings are joined with space only if they are not empty. Otherwise, the extra space is not added.
     *
     * @return the Collector, which joins strings with space.
     */
    static Collector<String, ?, String> joiningWithSpaceIfNotEmpty() {
        return Collector.of(
                StringBuilder::new,
                (first, second) -> {
                    if (first.isEmpty()) {
                        first.append(second);
                    } else if (!second.isEmpty()) {
                        first.append(" ").append(second);
                    }
                },
                StringBuilder::append,
                StringBuilder::toString
        );
    }
}
