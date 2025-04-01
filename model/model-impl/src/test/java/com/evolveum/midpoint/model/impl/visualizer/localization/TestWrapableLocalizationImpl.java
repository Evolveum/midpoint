package com.evolveum.midpoint.model.impl.visualizer.localization;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPartsWrapper;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

public class TestWrapableLocalizationImpl {

    private LocalizationService localizationService;

    @BeforeTest
    void setupLocalizationService() {
        final LocalizationServiceImpl localizationService = new LocalizationServiceImpl();
        localizationService.init();
        localizationService.setOverrideLocale(Locale.US);
        this.localizationService = localizationService;
    }

    @Test
    void localizationPartsAreWrappedOnce_suppliedWrapperShouldBeCorrectlyApplied() {

        final WrapableLocalization<String, Void> parts =
                WrapableLocalization.of(
                        LocalizationPart.forObject(new SingleLocalizableMessage("object "), null),
                        LocalizationPart.forObjectName(new SingleLocalizableMessage("object name "), null),
                        LocalizationPart.forAction(new SingleLocalizableMessage("action"), null),
                        LocalizationPart.forAdditionalInfo(new SingleLocalizableMessage("additional info"), null),
                        LocalizationPart.forHelpingWords(new SingleLocalizableMessage("helping words ")));

        final LocalizationPartsWrapper<String, Void, String> caseWrapper = LocalizationPartsWrapper.from(
                this::toUpperCase, this::toUpperCase, this::toUpperCase, this::toUpperCase, String::toUpperCase);

        final String combinedParts = parts.wrap(caseWrapper)
                .combineParts(new StringBuilder(), StringBuilder::append)
                .translate(this.localizationService, Locale.US)
                .toString();

        Assertions.assertThat(combinedParts).isEqualTo("OBJECT OBJECT NAME HELPING WORDS ACTION");
    }

    @Test
    void localizationPartsAreWrappedMultipleTimes_wrappingsShouldHappenInTheSameOrderAsWereDefinedIn() {

        final WrapableLocalization<String, Void> parts =
                WrapableLocalization.of(
                        LocalizationPart.forObject(new SingleLocalizableMessage("object "), null),
                        LocalizationPart.forObjectName(new SingleLocalizableMessage("object name "), null),
                        LocalizationPart.forAction(new SingleLocalizableMessage("action"), null),
                        LocalizationPart.forAdditionalInfo(new SingleLocalizableMessage("additional info"), null),
                        LocalizationPart.forHelpingWords(new SingleLocalizableMessage("helping words ")));

        final LocalizationPartsWrapper<String, Void, String> trimmingWrapper =
                LocalizationPartsWrapper.from(this::trim, this::trim, this::trim, this::trim, String::trim);

        final LocalizationPartsWrapper<String, Void, Integer> wordLengthWrapper =
                LocalizationPartsWrapper.from(this::length, this::length, this::length, this::length, String::length);

        final String combinedParts = parts.wrap(trimmingWrapper)
                .wrap(wordLengthWrapper)
                .combineParts(new StringBuilder(), StringBuilder::append)
                .translate(this.localizationService, Locale.US)
                .toString();

        Assertions.assertThat(combinedParts).isEqualTo("611136");
    }

    @Test
    void localizationPartsAreWrapped_contextShouldBePassedToWrapper() {

        final WrapableLocalization<String, String> parts =
                WrapableLocalization.of(
                        LocalizationPart.forObject(new SingleLocalizableMessage("object "), "context"),
                        LocalizationPart.forObjectName(new SingleLocalizableMessage("object name "), "context"),
                        LocalizationPart.forAction(new SingleLocalizableMessage("action"), "context"),
                        LocalizationPart.forAdditionalInfo(new SingleLocalizableMessage("additional info"), "context"),
                        LocalizationPart.forHelpingWords(new SingleLocalizableMessage("helping words ")));

        final LocalizationPartsWrapper<String, String, String> replacingWrapper =
                LocalizationPartsWrapper.from(this::replaceWithContext, this::replaceWithContext,
                        this::replaceWithContext, this::replaceWithContext, String::trim);

        final String combinedParts = parts.wrap(replacingWrapper)
                .combineParts(new StringBuilder(), StringBuilder::append)
                .translate(this.localizationService, Locale.US)
                .toString();

        Assertions.assertThat(combinedParts).isEqualTo("contextcontexthelping wordscontext");
    }

    @Test
    void localizationPartsAreCombined_localizationPartsShouldBeCombinedInTheSameOrderAsWereDefinedIn() {

        final WrapableLocalization<String, String> parts =
                WrapableLocalization.of(
                        LocalizationPart.forAction(new SingleLocalizableMessage("action "), null),
                        LocalizationPart.forObjectName(new SingleLocalizableMessage("object name "), null),
                        LocalizationPart.forObject(new SingleLocalizableMessage("object "), null),
                        LocalizationPart.forHelpingWords(new SingleLocalizableMessage("helping words")));

        final String combinedParts = parts.combineParts(new StringBuilder(), StringBuilder::append)
                .translate(this.localizationService, Locale.US)
                .toString();

        Assertions.assertThat(combinedParts).isEqualTo("action object name object helping words");
    }

    // !NOTE, bellow wrappers are totally made up just to test things. In real world, the value should never be wrapped
    // (or transformed) in a way which either destroys the original value, or changes its meaning or grammar.
    private String replaceWithContext(String value, String context) {
        return context;
    }

    private <C> String trim(String value, C context) {
        return value.trim();
    }

    private <C> int length(String value, C context) {
        return value.length();
    }

    private <C> String toUpperCase(String value, C context) {
        return value.toUpperCase();
    }

}
