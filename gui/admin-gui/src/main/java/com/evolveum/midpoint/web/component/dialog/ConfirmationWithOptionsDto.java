/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.util.Describable;

/**
 * Configuration of the {@link ConfirmationWithOptionsPanel}, holding various messages and a list of confirmation
 * options.
 */
public class ConfirmationWithOptionsDto<T extends Describable> implements Serializable {

    private final StringResourceModel confirmationTitle;
    private final String titleIconCssClass;
    private final StringResourceModel confirmationSubtitle;
    private final StringResourceModel confirmationInfoMessage;
    private final StringResourceModel externalLinkButtonLabel;
    private final String externalLinkUrl;
    private final StringResourceModel confirmationButtonLabel;
    private final String confirmationButtonCssClass;
    private final StringResourceModel cancelButtonLabel;
    private final String cancelButtonCssClass;
    private final StringResourceModel confirmationOptionsTitle;
    private final List<ConfirmationOption<T>> confirmationOptions;

    private ConfirmationWithOptionsDto(Builder<T> builder) {
        confirmationTitle = builder.confirmationTitle;
        titleIconCssClass = builder.titleIconCssClass;
        confirmationSubtitle = builder.confirmationSubtitle;
        confirmationOptionsTitle = builder.confirmationOptionsTitle;
        confirmationInfoMessage = builder.confirmationInfoMessage;
        externalLinkButtonLabel = builder.externalLinkButtonLabel;
        externalLinkUrl = builder.externalLinkUrl;
        confirmationButtonLabel = builder.confirmationButtonLabel;
        confirmationButtonCssClass = builder.confirmationButtonClass;
        cancelButtonLabel = builder.cancelButtonLabel;
        cancelButtonCssClass = builder.cancelButtonClass;
        confirmationOptions = builder.confirmationOptions;
    }

    public IModel<String> getConfirmationTitle() {
        return confirmationTitle;
    }

    public IModel<String> getConfirmationSubtitle() {
        return this.confirmationSubtitle;
    }

    public IModel<String> getConfirmationOptionsTitle() {
        return this.confirmationOptionsTitle;
    }

    public StringResourceModel getConfirmationInfoMessage() {
        return this.confirmationInfoMessage;
    }

    public String getExternalLinkUrl() {
        return this.externalLinkUrl;
    }

    public List<ConfirmationOption<T>> getConfirmationOptions() {
        return confirmationOptions;
    }

    public StringResourceModel getExternalLinkButtonLabel() {
        return this.externalLinkButtonLabel;
    }

    public String getTitleIconCssClass() {
        return this.titleIconCssClass;
    }

    public StringResourceModel getConfirmationButtonLabel() {
        return this.confirmationButtonLabel;
    }

    public String getConfirmationButtonCssClass() {
        return this.confirmationButtonCssClass;
    }

    public StringResourceModel getCancelButtonLabel() {
        return this.cancelButtonLabel;
    }

    public String getCancelButtonCssClass() {
        return this.cancelButtonCssClass;
    }

    public static <T extends Describable> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T extends Describable> {
        private StringResourceModel confirmationTitle;
        private String titleIconCssClass = "text-info";
        private StringResourceModel confirmationSubtitle =
                new StringResourceModel("SmartSuggestConfirmationPanel.subtitle");
        private StringResourceModel confirmationInfoMessage;
        private StringResourceModel externalLinkButtonLabel =
                new StringResourceModel("SmartSuggestConfirmationPanel.learnMore");
        private String externalLinkUrl = "https://docs.evolveum.com/";
        private StringResourceModel confirmationButtonLabel =
                new StringResourceModel("SmartSuggestConfirmationPanel.allowAndContinue");
        private String confirmationButtonClass = "btn btn-primary";
        private StringResourceModel cancelButtonLabel =
                new StringResourceModel("SmartSuggestConfirmationPanel.cancel");
        private String cancelButtonClass = "btn btn-default";
        private StringResourceModel confirmationOptionsTitle;
        private List<ConfirmationOption<T>> confirmationOptions;

        /**
         * Used as a confirmation popup title.
         */
        public Builder<T> confirmationTitle(StringResourceModel confirmationTitle) {
            this.confirmationTitle = confirmationTitle;
            return this;
        }

        public Builder<T> titleIconCssClass(String titleIconCssClass) {
            this.titleIconCssClass = titleIconCssClass;
            return this;
        }

        /**
         * Used as a subtitle or description of a confirmation panel.
         */
        public Builder<T> confirmationSubtitle(StringResourceModel confirmationSubtitle) {
            this.confirmationSubtitle = confirmationSubtitle;
            return this;
        }

        /**
         * Used as a title for the list of confirmation options, if they are present.
         */
        public Builder<T> confirmationOptionsTitle(StringResourceModel confirmationOptionsTitle) {
            this.confirmationOptionsTitle = confirmationOptionsTitle;
            return this;
        }

        /**
         * Used as a message shown in the info panel.
         *
         * If it is set to null, the info panel will not be shown.
         */
        public Builder<T> confirmationInfoMessage(StringResourceModel confirmationInfoMessage) {
            this.confirmationInfoMessage = confirmationInfoMessage;
            return this;
        }

        /**
         * Used as a label on the external link button.
         */
        public Builder<T> externalLinkButtonLabel(StringResourceModel externalLinkButtonLabel) {
            this.externalLinkButtonLabel = externalLinkButtonLabel;
            return this;
        }

        /**
         * Used as a url for the external link button.
         *
         * If it is set to null, the external link button will not be shown.
         */
        public Builder<T> externalLinkUrl(String externalLinkUrl) {
            this.externalLinkUrl = externalLinkUrl;
            return this;
        }

        /**
         * Used as a label for the confirmation button.
         */
        public Builder<T> confirmationButtonLabel(StringResourceModel confirmationButtonLabel) {
            this.confirmationButtonLabel = confirmationButtonLabel;
            return this;
        }

        /**
         * Used as additional css class for the confirmation button.
         */
        public Builder<T> confirmationButtonClass(String confirmationButtonClass) {
            this.confirmationButtonClass = confirmationButtonClass;
            return this;
        }

        /**
         * Used as a label for the cancel button.
         */
        public Builder<T> cancelButtonLabel(StringResourceModel cancelButtonLabel) {
            this.cancelButtonLabel = cancelButtonLabel;
            return this;
        }

        /**
         * Used as additional css class for the cancel button.
         */
        public Builder<T> cancelButtonClass(String cancelButtonClass) {
            this.cancelButtonClass = cancelButtonClass;
            return this;
        }

        /**
         * Used to show panel with the list of options, which user can select.
         *
         * Selected options are confirmed byt the confirmation button.
         */
        public Builder<T> confirmationOptions(List<ConfirmationOption<T>> confirmationOptions) {
            this.confirmationOptions = confirmationOptions;
            return this;
        }

        public ConfirmationWithOptionsDto<T> build() {
            return new ConfirmationWithOptionsDto<>(this);
        }

    }

}
