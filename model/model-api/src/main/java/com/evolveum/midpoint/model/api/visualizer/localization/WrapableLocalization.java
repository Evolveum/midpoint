package com.evolveum.midpoint.model.api.visualizer.localization;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collector;

/**
 * Localization composed of multiple "parts", which can be "wrapped" to some other object.
 *
 * Clients of the interface may chain more "wrappings" with the {@link #wrap} method. When all the wrappings are set,
 * the {@link #combineParts(Collector)} or the {@link #combineParts(Object, LocalizationPartsCombiner)} methods could
 * be used to specify the combination function, which will "combine" all localization together after their translation.
 *
 * Implementations of this interface should implement all methods in a "lazy fashion". That means, the underlying
 * "parts" should not be manipulated in any way. They should be only "prepared" for the actual "manipulation"
 * (wrapping, combining), which may eventually happen upon the method calls on returned {@link LocalizableObject}.
 *
 * @param <T> the type of the current wrapper object
 * @param <C> the type of the context object with extra data about the individual localization parts.
 */
public interface WrapableLocalization<T, C> extends Serializable {

    /**
     * Combine individual localization parts together to form a single localization object which can be translated.
     *
     * Combination of localization parts does not happen immediately after the method call, but it will eventually
     * happen during manipulation with resulting {@link LocalizableObject}.
     *
     * @param accumulator the mutable "container" which can hold individual localization parts. Depending on the
     * combiner, it (the combiner) may or may not return the same instance.
     * @param combiner the mutable reduction function, which should combine previously combined parts with another part.
     * @return the localization object, which contains results container with all accumulated localization parts.
     * @param <R> the type of the localization parts container.
     */
    <R> LocalizableObject<R> combineParts(R accumulator, LocalizationPartsCombiner<? super T, R> combiner);

    /**
     * Combine individual localization parts together to form a single localization object which can be translated.
     *
     * Combination of localization parts does not happen immediately after the method call, but it will eventually
     * happen during manipulation with resulting {@link LocalizableObject}.
     *
     * Main difference from the {@link #combineParts(Object, LocalizationPartsCombiner)} is that thanks to the
     * {@link Collector}'s "finisher", the type of final result is not tight to the intermediate mutable container
     * used to accumulate all the parts. This allows to e.g. use collector, which internally collects all parts (e.g.
     * Strings) to a StringBuilder, but the final result will be String.
     *
     * @param collector the instance of {@link Collector}, which implement the desired mutable reduction function.
     * @return the localization object, which contains the result of the collecting using the provided collector.
     * @param <R> the type of the collecting result.
     */
    <R> LocalizableObject<R> combineParts(Collector<? super T, ?, R> collector);

    /**
     * Wrap (potentially already wrapped) localization parts to another wrapper object.
     *
     * This method can be used to wrap localization parts to another "wrapper" objects using the supplied {@param
     * wrapper}". Supplied {@code wrapper) parameter is nothing else than a set of functions which wraps individual
     * "types" of localization parts.
     *
     * There are several types of localization parts. The wrapper has to implement function to wrap each of the
     * localization part, regardless of its actual presence. This is one of the unpleasant limitation of the API.
     * @param wrapper the implementation of wrapping function of each possible localization part type.
     * @return another instance of the {@code WrapableLocalization} with set wrapping functions.
     * @param <R> the type of the wrapping object.
     */
    <R> WrapableLocalization<R, C> wrap(LocalizationPartsWrapper<? super T, C, ? extends R> wrapper);

    /**
     * Convenient factory method to create new instance of the {@link WrapableLocalization}
     * @param parts the individual localization parts.
     * @return new instance of the {@code WrapableLocalization}
     * @param <C> the type of the context object with extra data about the individual localization parts.
     */
    @SafeVarargs
    static <C> WrapableLocalization<String, C> of(LocalizationPart<C>... parts) {
        return new WrapableLocalizationImpl<>(List.of(parts),
                LocalizationPartsWrapper.from(initialIdentityWrapper(), initialIdentityWrapper(),
                        initialIdentityWrapper(), initialIdentityWrapper(), value -> value));
    }

    private static <C> LocalizationPartsWrapper.SerializableBiFunction<String, C, String> initialIdentityWrapper() {
        return (value, context) -> value;
    }
}
