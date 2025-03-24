package com.evolveum.midpoint.model.api.visualizer.localization;

import com.evolveum.midpoint.util.LocalizableMessage;

public interface LocalizationPart<C> {
    <T> LocalizableObject<T> wrap(LocalizationPartsWrapper<String, C, T> wrapper);

    static <C> LocalizationPart<C> forObject(LocalizableMessage object, C context) {
        return new ObjectPart<>(object, context);
    }

    static <C> LocalizationPart<C> forObjectName(LocalizableMessage objectName, C context) {
        return new ObjectNamePart<>(objectName, context);
    }

    static <C> LocalizationPart<C> forAction(LocalizableMessage action, C context) {
        return new ActionPart<>(action, context);
    }

    static <C> LocalizationPart<C> forHelpingWords(LocalizableMessage helpingWords) {
        return new HelpingWordsPart<>(helpingWords);
    }

    class ObjectPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        public ObjectPart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapObject(localizationService.translate(this.message, locale), this.context);
        }

    }

    class ObjectNamePart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        public ObjectNamePart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapObjectName(localizationService.translate(this.message, locale), this.context);
        }

    }

    class ActionPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;
        private final C context;

        public ActionPart(LocalizableMessage message, C context) {
            this.message = message;
            this.context = context;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapAction(localizationService.translate(this.message, locale), this.context);
        }

    }

    class HelpingWordsPart<C> implements LocalizationPart<C> {
        private final LocalizableMessage message;

        public HelpingWordsPart(LocalizableMessage message) {
            this.message = message;
        }

        @Override
        public <R> LocalizableObject<R> wrap(LocalizationPartsWrapper<String, C, R> wrapper) {
            return (localizationService, locale) ->
                    wrapper.wrapHelpingWords(localizationService.translate(this.message, locale));
        }

    }

}

