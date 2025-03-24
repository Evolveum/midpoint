package com.evolveum.midpoint.model.api.visualizer.localization;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface LocalizationPartsWrapper<T, C, R> {
    R wrapObject(T object, C context);
    R wrapObjectName(T objectName, C context);
    R wrapAction(T action, C context);
    R wrapHelpingWords(T helpingWords);

    static <T, C, R> LocalizationPartsWrapper<T, C, R> from(
            BiFunction<T, C, R> objectWrapper,
            BiFunction<T, C, R> objectNameWrapper,
            BiFunction<T, C, R> actionWrapper,
            Function<T, R> helpingWordsWrapper
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
            public R wrapHelpingWords(T helpingWords) {
                return helpingWordsWrapper.apply(helpingWords);
            }
        };
    }

    default <S> LocalizationPartsWrapper<T, C, S> compose(
            LocalizationPartsWrapper<? super R, C, ? extends S> next) {
        return from(
                (object, context) -> next.wrapObject(this.wrapObject(object, context), context),
                (objectName, context) -> next.wrapObjectName(this.wrapObjectName(objectName, context), context),
                (action, context) -> next.wrapAction(this.wrapAction(action, context), context),
                helpingWords -> next.wrapHelpingWords(this.wrapHelpingWords(helpingWords))
        );
    }
}
