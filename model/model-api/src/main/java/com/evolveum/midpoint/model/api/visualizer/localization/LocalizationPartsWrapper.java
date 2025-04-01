package com.evolveum.midpoint.model.api.visualizer.localization;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This interface represents set of "wrapping" functions, which are used to wrap particular localization parts types.
 *
 * There are several types of {@link LocalizationPart}. Each of them has a dedicated method in this interface, which
 * are used to "wrap" them.
 *
 * Implementations of this interface has to always implement all the methods, regardless of the actual presence of
 * particular localization parts types in the {@link WrapableLocalization}.
 *
 * WARNING do not implement wrapping functions in a way, which would destroy "meaning" of the underlying object. It's
 * called "wrapper", because it should only "wrap" objects underneath, not transform them to something else (in
 * the meaning sense)
 *
 * @param <T> the type of the object represented by the localization part.
 * @param <C> the type of the context used to provide additional data about the localization part.
 * @param <R> the resulting typ of the "wrapper" object
 */
public interface LocalizationPartsWrapper<T, C, R> extends Serializable {
    R wrapObject(T object, C context);
    R wrapObjectName(T objectName, C context);
    R wrapAction(T action, C context);
    R wrapAdditionalInfo(T translate, C context);
    R wrapHelpingWords(T helpingWords);

    /**
     * Convenient factory method, which can be used to create new instance of the {@code LocalizationPartsWrapper}
     * from the set of wrapping functions.
     *
     * WARNING do not implement wrapping functions in a way, which would destroy "meaning" of the underlying object.
     * It's called "wrapper", because it should only "wrap" objects underneath, not to transform them to something else
     * (in the "meaning" sense).
     *
     * @param objectWrapper  the function, which is used to wrap "object" localization part type.
     * @param objectNameWrapper the function, which is used to wrap "object name" localization part type.
     * @param actionWrapper the function, which is used to wrap "action" localization part type.
     * @param additionalInfoWrapper the function, which is used to wrap "additional info" localization part type.
     * @param helpingWordsWrapper the function, which is used to wrap "helping words" localization part type.
     * @return new instance of the {@code LocalizationPartsWrapper}
     * @param <T> the type of the object represented by the localization part.
     * @param <C> the type of the context used to provide additional data about the localization part.
     * @param <R> the resulting typ of the "wrapper" object
     */
    static <T, C, R> LocalizationPartsWrapper<T, C, R> from(
            SerializableBiFunction<T, C, R> objectWrapper,
            SerializableBiFunction<T, C, R> objectNameWrapper,
            SerializableBiFunction<T, C, R> actionWrapper,
            SerializableBiFunction<T, C, R> additionalInfoWrapper,
            SerializableFunction<T, R> helpingWordsWrapper
    ) {
        return new LocalizationPartsWrapper<>() {
            @Override
            public R wrapObject(T object, C context) {
                return objectWrapper.apply(object, context);
            }

            @Override
            public R wrapObjectName(T objectName, C context) {
                return objectNameWrapper.apply(objectName, context);
            }

            @Override
            public R wrapAction(T action, C context) {
                return actionWrapper.apply(action, context);
            }

            @Override
            public R wrapAdditionalInfo(T additionalInfo, C context) {
                return additionalInfoWrapper.apply(additionalInfo, context);
            }

            @Override
            public R wrapHelpingWords(T helpingWords) {
                return helpingWordsWrapper.apply(helpingWords);
            }
        };
    }

    /**
     * Compose this instance of {@code LocalizationPartsWrapper} with another wrapper.
     *
     * @param next the wrapper which will be applied as next in the wrapping chain.
     * @return new composed instance of the {@code LocalizationPartsWrapper}.
     * @param <S> the type of the new wrapper object.
     */
    default <S> LocalizationPartsWrapper<T, C, S> compose(
            LocalizationPartsWrapper<? super R, C, ? extends S> next) {
        return from(
                (object, context) -> next.wrapObject(this.wrapObject(object, context), context),
                (objectName, context) -> next.wrapObjectName(this.wrapObjectName(objectName, context), context),
                (action, context) -> next.wrapAction(this.wrapAction(action, context), context),
                (additionalInfo, context) -> next.wrapAction(this.wrapAction(additionalInfo, context), context),
                helpingWords -> next.wrapHelpingWords(this.wrapHelpingWords(helpingWords))
        );
    }

    // These are workarounds in order to have Wrapable Localizations serializable on the Wicket Layer
    interface SerializableBiFunction<P1, P2, R> extends BiFunction<P1, P2, R>, Serializable {}
    interface SerializableFunction<P1, R> extends Function<P1, R>, Serializable {}

}
