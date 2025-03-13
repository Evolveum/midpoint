package com.evolveum.midpoint.notifications.impl.formatters;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.visualizer.output.NameImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemValueImpl;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PlainTextPropertiesFormatterTest {

    private static final String FAMILY_NAME = "Moon";
    private static final String GIVEN_NAME = "George";

    private PropertiesFormatter<VisualizationItem> formatter;

    @BeforeTest
    void setupLocalizationService() {
        final URL midPointHomeDir = this.getClass().getResource("/midpoint-home");
        if (midPointHomeDir == null) {
            throw new IllegalStateException("Unable to find test midpoint home directory.");
        }
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midPointHomeDir.getPath());
        final LocalizationServiceImpl localizationService = new LocalizationServiceImpl();
        localizationService.init();
        localizationService.setOverrideLocale(Locale.US);
        this.formatter = new PlainTextPropertiesFormatter(new IndentationGenerator("|", "\t"),
                new PropertyFormatter(localizationService, " ", "\n"));
    }

    @Test
    void listOfItemsIsEmpty_formatPropertiesIsCalled_emptyStringShouldBeReturned() {
        final String formattedProperties = this.formatter.formatProperties(Collections.emptyList(), 0);

        Assertions.assertThat(formattedProperties)
                .withFailMessage("Empty collection of properties should be formatted to empty string.")
                .isEmpty();
    }

    @Test
    void itemHasBothDisplayNameAndSimpleName_formatPropertiesIsCalled_itemShouldBeFormattedAndContainDisplayName() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        name.setDisplayName("Given name");
        final VisualizationItemImpl givenName = new VisualizationItemImpl(name);
        givenName.setNewValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String expectedItemFormat = "%s: %s"
                .formatted(name.getDisplayName().getFallbackMessage(), GIVEN_NAME);

        final String formattedProperties = this.formatter.formatProperties(List.of(givenName), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemHasOnlySimpleName_formatPropertiesIsCalled_itemShouldBeFormattedAndContainSimpleName() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        final VisualizationItemImpl givenName = new VisualizationItemImpl(name);
        givenName.setNewValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String expectedItemFormat = "%s: %s"
                .formatted(name.getSimpleName().getFallbackMessage(), GIVEN_NAME);

        final String formattedProperties = this.formatter.formatProperties(List.of(givenName), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemDoesNotHaveSimpleNorDisplayName_formatPropertiesIsCalled_itemShouldBeFormattedAndContainUnknownName() {
        final NameImpl name = new NameImpl((String) null);
        final VisualizationItemImpl givenName = new VisualizationItemImpl(name);
        givenName.setNewValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String expectedItemFormat = "Unknown: " + GIVEN_NAME;

        final String formattedProperties = this.formatter.formatProperties(List.of(givenName), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemHasLocalizableSimpleName_formatPropertiesIsCalled_itemShouldBeFormattedAndContainLocalizedName() {
        final NameImpl name = new NameImpl(new SingleLocalizableMessage(UserType.F_FAMILY_NAME.getLocalPart()));
        final VisualizationItemImpl givenName = new VisualizationItemImpl(name);
        givenName.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String expectedItemFormat = "Family name: " + FAMILY_NAME;

        final String formattedProperties = this.formatter.formatProperties(List.of(givenName), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemHasLocalizableDisplayName_formatPropertiesIsCalled_itemShouldBeFormattedAndContainLocalizedName() {
        final NameImpl name = new NameImpl(new SingleLocalizableMessage("surname"));
        name.setDisplayName(new SingleLocalizableMessage("lastName"));
        final VisualizationItemImpl lastName = new VisualizationItemImpl(name);
        lastName.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String expectedItemFormat = "Last name: " + FAMILY_NAME;

        final String formattedProperties = this.formatter.formatProperties(List.of(lastName), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void nestingLevelIsBiggerThanZero_formatPropertiesIsCalled_itemShouldBeFormattedWithCorrectPrefixAndIndentation() {
        final NameImpl name = new NameImpl(new SingleLocalizableMessage(UserType.F_FAMILY_NAME.getLocalPart()));
        final VisualizationItemImpl familyName = new VisualizationItemImpl(name);
        familyName.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String expectedItemFormat = "|\tFamily name: " + FAMILY_NAME;

        final String formattedProperties = this.formatter.formatProperties(List.of(familyName), 1);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void nestingLevelIsBiggerThanOne_formatPropertiesIsCalled_itemShouldBeFormattedWithCorrectPrefixAndIndentation() {
        final NameImpl name = new NameImpl(new SingleLocalizableMessage(UserType.F_FAMILY_NAME.getLocalPart()));
        final VisualizationItemImpl familyName = new VisualizationItemImpl(name);
        familyName.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String expectedItemFormat = "|\t|\t|\tFamily name: " + FAMILY_NAME;

        final String formattedProperties = this.formatter.formatProperties(List.of(familyName), 3);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void nestingLevelIsLessThanZero_formatPropertiesIsCalled_exceptionShouldBeThrown() {
        final NameImpl name = new NameImpl(new SingleLocalizableMessage(UserType.F_FAMILY_NAME.getLocalPart()));
        final VisualizationItemImpl familyName = new VisualizationItemImpl(name);
        familyName.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        Assertions.assertThatThrownBy(() -> formatter.formatProperties(List.of(familyName), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nesting level can not be negative");
    }

    @Test
    void moreItemsArePresent_formatPropertiesIsCalled_allPresentItemsShouldBeFormatted() {
        final NameImpl givenName = new NameImpl(new SingleLocalizableMessage(UserType.F_GIVEN_NAME.getLocalPart()));
        // givenName specified above is not in our localization file so provide it as a display name
        givenName.setDisplayName("Given name");
        final VisualizationItemImpl givenNameItem = new VisualizationItemImpl(givenName);
        givenNameItem.setNewValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final NameImpl familyName = new NameImpl(new SingleLocalizableMessage(UserType.F_FAMILY_NAME.getLocalPart()));
        final VisualizationItemImpl familyNameItem = new VisualizationItemImpl(familyName);
        familyNameItem.setNewValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String expectedItemFormat = """
                |\tGiven name: %s
                |\tFamily name: %s""".formatted(GIVEN_NAME, FAMILY_NAME);

        final String formattedProperties = this.formatter.formatProperties(List.of(givenNameItem, familyNameItem), 1);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemHasMultipleValues_formatPropertiesIsCalled_itemShouldBeFormattedWithAllItsValues() {
        final NameImpl organizationName = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        final VisualizationItemImpl organizationItem = new VisualizationItemImpl(organizationName);
        final String hwuOrganization = "Hard Workers Union";
        final String bwfOrganization = "Better World Federation";
        organizationItem.setNewValues(List.of(
                new VisualizationItemValueImpl(hwuOrganization),
                new VisualizationItemValueImpl(bwfOrganization)));

        final String expectedItemFormat = """
                %s:
                |\t%s
                |\t%s""".formatted(UserType.F_ORGANIZATION.getLocalPart(), hwuOrganization, bwfOrganization);

        final String formattedProperties = this.formatter.formatProperties(List.of(organizationItem), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }

    @Test
    void itemHasLocalizableValue_formatPropertiesIsCalled_itemShouldBeFormattedWithLocalizedValue() {
        final NameImpl descriptionName = new NameImpl(UserType.F_DESCRIPTION.getLocalPart());
        final VisualizationItemImpl descriptionItem = new VisualizationItemImpl(descriptionName);
        final String descriptionTranslation = "Translated description";
        final PolyString descriptionPolyString = new PolyString("This is original description", null, null,
                Map.of("en", descriptionTranslation));
        // This is a bit of a trick, where the poly string with translation is passed to LocalizableMessage as a
        // message parameter (check translation value of the "singleValue" key in properties file). I haven't found
        // other way and this was used also in another place.
        descriptionItem.setNewValues(List.of(new VisualizationItemValueImpl(new SingleLocalizableMessage("singleValue",
                new Object[]{descriptionPolyString}, "This is fallback message"))));

        final String expectedItemFormat = "%s: %s"
                .formatted(UserType.F_DESCRIPTION.getLocalPart(), descriptionTranslation);

        final String formattedProperties = this.formatter.formatProperties(List.of(descriptionItem), 0);

        Assertions.assertThat(formattedProperties).isEqualTo(expectedItemFormat);
    }
}