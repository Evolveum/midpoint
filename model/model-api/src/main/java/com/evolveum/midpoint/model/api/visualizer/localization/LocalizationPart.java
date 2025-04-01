package com.evolveum.midpoint.model.api.visualizer.localization;

import java.io.Serializable;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Single part of the potentially bigger localization
 *
 * There are several types of localization parts, each of which can be created using provided factory methods.
 *
 * @param <C> the type of the context object, which provides additional data about the localization part.
 */
public sealed interface LocalizationPart<C> extends Serializable permits LocalizationPart.ObjectPart,
        LocalizationPart.ObjectNamePart, LocalizationPart.ActionPart, LocalizationPart.HelpingWordsPart,
        LocalizationPart.AdditionalInfoPart {
    <T> LocalizableObject<T> wrap(LocalizationPartsWrapper<String, C, T> wrapper);

    /**
     * Creates localization part of the "Object" type.
     * @param object the localizable message which represent the "object" in the bigger sentence.
     * @param context the context used to provide additional data about the object.
     * @return new instance of the object localization part.
     * @param <C> the type of the context.
     */
    static <C> LocalizationPart<C> forObject(LocalizableMessage object, C context) {
        return new ObjectPart<>(object, context);
    }

    /**
     * Creates localization part of the "Object name" type.
     * @param objectName the localizable message which represent the object's name in the bigger sentence.
     * @param context the context used to provide additional data about the object.
     * @return new instance of the object's name localization part.
     * @param <C> the type of the context.
     */
    static <C> LocalizationPart<C> forObjectName(LocalizableMessage objectName, C context) {
        return new ObjectNamePart<>(objectName, context);
    }

    /**
     * Creates localization part of the "Action" type.
     * @param action the localizable message which represent the "action" in the bigger sentence.
     * @param context the context used to provide additional data about the action.
     * @return new instance of the action localization part.
     * @param <C> the type of the context.
     */
    static <C> LocalizationPart<C> forAction(LocalizableMessage action, C context) {
        return new ActionPart<>(action, context);
    }

    /**
     * Creates localization part of the "Additional info" type.
     * @param additionalInfo the localizable message which represent some the additional information about the object
     * (or possibly action).
     * @param context the context used to provide additional data about the object.
     * @return new instance of the additional info localization part.
     * @param <C> the type of the context.
     */
    static <C> LocalizationPart<C> forAdditionalInfo(LocalizableMessage additionalInfo, C context) {
        return new AdditionalInfoPart<>(additionalInfo, context);
    }

    /**
     * Creates localization part of the "Helping words" type.
     * @param helpingWords the localizable message which represent some kind of "helping words" in the bigger
     * sentence. It may be any words form simple "is" or "has been" to whole sentence.
     * @return new instance of the helping words localization part.
     * @param <C> the type of the context (which is not used in this particular case).
     */
    static <C> LocalizationPart<C> forHelpingWords(LocalizableMessage helpingWords) {
        return new HelpingWordsPart<>(helpingWords);
    }

    final class ObjectPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        private ObjectPart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapObject(localizationService.translate(this.message, locale), this.context);
        }

    }

    final class ObjectNamePart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        private ObjectNamePart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapObjectName(localizationService.translate(this.message, locale), this.context);
        }

    }

    final class ActionPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        private ActionPart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapAction(localizationService.translate(this.message, locale), this.context);
        }

    }

    final class HelpingWordsPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;

        private HelpingWordsPart(LocalizableMessage message) {
            this.message = message;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapHelpingWords(localizationService.translate(this.message, locale));
        }

    }

    final class AdditionalInfoPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        private AdditionalInfoPart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapAdditionalInfo(localizationService.translate(this.message, locale), this.context);
        }

    }

}

