package com.evolveum.midpoint.notifications.impl.formatters;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.impl.visualizer.output.NameImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationDeltaItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemValueImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ContainerPropertiesModificationFormatterTest {

    private static final String FAMILY_NAME = "Moon";
    private static final String GIVEN_NAME = "George";

    private PropertiesFormatter<VisualizationDeltaItem> formatter;

    @BeforeTest
    void setupLocalizationServiceAndFormatter() {
        final URL midPointHomeDir = this.getClass().getResource("/midpoint-home");
        if (midPointHomeDir == null) {
            throw new IllegalStateException("Unable to find test midpoint home directory.");
        }
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midPointHomeDir.getPath());
        final LocalizationServiceImpl localizationService = new LocalizationServiceImpl();
        localizationService.init();
        localizationService.setOverrideLocale(Locale.US);
        final IndentationGenerator indentationGenerator = new IndentationGenerator("|", "\t");
        final PropertyFormatter propertyFormatter = new PropertyFormatter(localizationService, " ", "\n");
        final PlainTextPropertiesFormatter propertiesFormatter = new PlainTextPropertiesFormatter(indentationGenerator,
                propertyFormatter);
        final ModifiedPropertiesFormatter modifiedPropertiesFormatter =
                new ModifiedPropertiesFormatter(propertyFormatter, indentationGenerator);
        this.formatter = new ContainerPropertiesModificationFormatter(localizationService, propertiesFormatter,
                indentationGenerator, modifiedPropertiesFormatter);
    }

    @Test
    void listOfDeltasIsEmpty_formatModificationsIsCalled_emptyStringShouldBeReturned() {
        final String formattedModifications = this.formatter.formatProperties(Collections.emptyList(), 0);

        Assertions.assertThat(formattedModifications)
                .withFailMessage("Empty collection of modifications should be formatted to empty string")
                .isEmpty();
    }

    @Test
    void deltaHasAddedProperties_formatModificationsIsCalled_addedPropertiesShouldBeProperlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        name.setDisplayName("Given name");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        modification.setAddedValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Added properties:
                        |\tGiven name: %s""".formatted(GIVEN_NAME));
    }

    @Test
    void deltaHasDeletedProperties_formatModificationsIsCalled_deletedPropertiesShouldBeProperlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        name.setDisplayName("Given name");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        modification.setDeletedValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Deleted properties:
                        |\tGiven name: %s""".formatted(GIVEN_NAME));
    }

    @Test
    void deltaHasDeletedAndAddedProperties_formatModificationsIsCalled_propertiesShouldBeProperlyFormatted() {
        final NameImpl givenName = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        givenName.setDisplayName("Given name");
        final VisualizationDeltaItemImpl deletedGivenName = new VisualizationDeltaItemImpl(givenName);
        deletedGivenName.setDeletedValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final NameImpl familyName = new NameImpl(UserType.F_FAMILY_NAME.getLocalPart());
        familyName.setDisplayName("Family name");
        final VisualizationDeltaItemImpl addedFamilyName = new VisualizationDeltaItemImpl(familyName);
        addedFamilyName.setAddedValues(List.of(new VisualizationItemValueImpl(FAMILY_NAME)));

        final String formattedModifications = this.formatter.formatProperties(
                List.of(addedFamilyName, deletedGivenName), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Added properties:
                        |\tFamily name: %s
                        Deleted properties:
                        |\tGiven name: %s""".formatted(FAMILY_NAME, GIVEN_NAME));
    }

    @Test
    void nestingLevelIsBiggerThanZero_formatModificationsIsCalled_correctPrefixAndIndentationShouldBeUsed() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        name.setDisplayName("Given name");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        modification.setAddedValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 1);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        |\tAdded properties:
                        |\t|\tGiven name: %s""".formatted(GIVEN_NAME));
    }

    @Test
    void deltaAddsValueToExistingProperty_formatModificationsIsCalled_addedValueShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues((List.of(new VisualizationItemValueImpl("Existing Organization"))));
        final String addedOrganizationName = "New Organization";
        modification.setAddedValues(List.of(new VisualizationItemValueImpl(addedOrganizationName)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tAdded values: %s""".formatted(addedOrganizationName));
    }

    @Test
    void deltaAddsMoreValuesToExistingProperty_formatModificationsIsCalled_addedValuesShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues((List.of(new VisualizationItemValueImpl("Existing Organization"))));
        final String addedOrganizationName = "New Organization";
        final String secondAddedOrganizationName = "Even Newer Organization";
        modification.setAddedValues(List.of(
                new VisualizationItemValueImpl(addedOrganizationName),
                new VisualizationItemValueImpl(secondAddedOrganizationName)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tAdded values:
                        |\t|\t|\t%s
                        |\t|\t|\t%s""".formatted(addedOrganizationName, secondAddedOrganizationName));
    }

    @Test
    void deltaDeletesValueFromExistingProperty_formatModificationsIsCalled_removedValueShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues((List.of(new VisualizationItemValueImpl("Existing Organization"))));
        final String deletedOrganizationName = "Unpopular Organization";
        modification.setDeletedValues(List.of(new VisualizationItemValueImpl(deletedOrganizationName)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tDeleted values: %s""".formatted(deletedOrganizationName));
    }

    @Test
    void deltaDeletesMoreValuesToExistingProperty_formatModificationsIsCalled_deletedValuesShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues((List.of(new VisualizationItemValueImpl("Existing Organization"))));
        final String deletedOrganizationName = "Unpopular Organization";
        final String secondDeletedOrganizationName = "Unpopular Organization";
        modification.setDeletedValues(List.of(
                new VisualizationItemValueImpl(deletedOrganizationName),
                new VisualizationItemValueImpl(secondDeletedOrganizationName)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tDeleted values:
                        |\t|\t|\t%s
                        |\t|\t|\t%s""".formatted(deletedOrganizationName, secondDeletedOrganizationName));
    }

    @Test
    void deltaReplacesSingleValuedProperty_formatModificationsIsCalled_replacedValueShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        name.setDisplayName("Given name");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate" replacement of single valued property (or multivalued property, which had
        // and still has only one value)
        modification.setDeletedValues((List.of(new VisualizationItemValueImpl(GIVEN_NAME))));
        final String replacedGivenName = "Camino";
        modification.setAddedValues(List.of(new VisualizationItemValueImpl(replacedGivenName)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\t%s: %s -> %s""".formatted(name.getDisplayName().getFallbackMessage(), GIVEN_NAME,
                        replacedGivenName));
    }

    @Test
    void deltaDeletesAndAddValuesToExistingProperty_formatModificationsIsCalled_valuesShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues(List.of(new VisualizationItemValueImpl("Remaining Organization")));
        final String deletedOrganization = "Unwanted Organization";
        final String secondDeletedOrganization = "Even More Unwanted Organization";
        modification.setDeletedValues((List.of(
                new VisualizationItemValueImpl(deletedOrganization),
                new VisualizationItemValueImpl(secondDeletedOrganization))));
        final String addedOrganization = "New Organization";
        final String secondAddedOrganization = "Even Newer Organization";
        modification.setAddedValues(List.of(
                new VisualizationItemValueImpl(addedOrganization),
                new VisualizationItemValueImpl(secondAddedOrganization)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tAdded values:
                        |\t|\t|\t%s
                        |\t|\t|\t%s
                        |\t|\tDeleted values:
                        |\t|\t|\t%s
                        |\t|\t|\t%s""".formatted(addedOrganization, secondAddedOrganization, deletedOrganization,
                        secondDeletedOrganization));
    }

    @Test
    void deltaDeletesOneAndAddsTwoValuesToExistingProperty_formatModificationsIsCalled_valuesShouldBeCorrectlyFormatted() {
        final NameImpl name = new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        name.setDisplayName("Organizations");
        final VisualizationDeltaItemImpl modification = new VisualizationDeltaItemImpl(name);
        // this is how we can "simulate", that multivalued property already had some value.
        modification.setUnchangedValues(List.of(new VisualizationItemValueImpl("Remaining Organization")));
        final String deletedOrganization = "Unwanted Organization";
        modification.setDeletedValues((List.of(new VisualizationItemValueImpl(deletedOrganization))));
        final String addedOrganization = "New Organization";
        final String secondAddedOrganization = "Even Newer Organization";
        modification.setAddedValues(List.of(
                new VisualizationItemValueImpl(addedOrganization),
                new VisualizationItemValueImpl(secondAddedOrganization)));

        final String formattedModifications = this.formatter.formatProperties(List.of(modification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tAdded values:
                        |\t|\t|\t%s
                        |\t|\t|\t%s
                        |\t|\tDeleted values: %s""".formatted(addedOrganization, secondAddedOrganization,
                        deletedOrganization));
    }

    @Test
    void deltaContainsChangesOfMoreExistingProperties_formatModificationsIsCalled_propertiesShouldBeCorrectlyFormatted() {
        final NameImpl organization= new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        organization.setDisplayName("Organizations");
        final NameImpl givenName = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        givenName.setDisplayName("Given name");

        final VisualizationDeltaItemImpl organizationsModification = new VisualizationDeltaItemImpl(organization);
        // this is how we can "simulate", that multivalued property already had some value.
        organizationsModification.setUnchangedValues(List.of(new VisualizationItemValueImpl("Remaining Organization")));
        final String deletedOrganization = "Unwanted Organization";
        organizationsModification.setDeletedValues((List.of(new VisualizationItemValueImpl(deletedOrganization))));

        final VisualizationDeltaItemImpl givenNameModification = new VisualizationDeltaItemImpl(givenName);
        // this is how we can "simulate", that multivalued property already had some value.
        givenNameModification.setUnchangedValues(List.of(new VisualizationItemValueImpl(GIVEN_NAME)));
        final String addedGivenName = "Chuck";
        givenNameModification.setAddedValues((List.of(new VisualizationItemValueImpl(addedGivenName))));

        final String formattedModifications = this.formatter.formatProperties(List.of(
                organizationsModification, givenNameModification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Modified properties:
                        |\tOrganizations:
                        |\t|\tDeleted values: %s
                        |\tGiven name:
                        |\t|\tAdded values: %s""".formatted(deletedOrganization, addedGivenName));
    }

    @Test
    void deltaAddsMoreProperties_formatModificationsIsCalled_propertiesShouldBeCorrectlyFormatted() {
        final NameImpl organization= new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        organization.setDisplayName("Organizations");
        final NameImpl givenName = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        givenName.setDisplayName("Given name");

        final VisualizationDeltaItemImpl organizationsModification = new VisualizationDeltaItemImpl(organization);
        final String addedOrganization = "New Organization";
        organizationsModification.setAddedValues((List.of(new VisualizationItemValueImpl(addedOrganization))));

        final VisualizationDeltaItemImpl givenNameModification = new VisualizationDeltaItemImpl(givenName);
        final String addedGivenName = "Chuck";
        givenNameModification.setAddedValues((List.of(new VisualizationItemValueImpl(addedGivenName))));

        final String formattedModifications = this.formatter.formatProperties(List.of(
                organizationsModification, givenNameModification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Added properties:
                        |\tOrganizations: %s
                        |\tGiven name: %s""".formatted(addedOrganization, addedGivenName));
    }

    @Test
    void deltaDeletesMoreProperties_formatModificationsIsCalled_propertiesShouldBeCorrectlyFormatted() {
        final NameImpl organization= new NameImpl(UserType.F_ORGANIZATION.getLocalPart());
        organization.setDisplayName("Organizations");
        final NameImpl givenName = new NameImpl(UserType.F_GIVEN_NAME.getLocalPart());
        givenName.setDisplayName("Given name");

        final VisualizationDeltaItemImpl organizationsModification = new VisualizationDeltaItemImpl(organization);
        final String deletedOrganization = "Old Organization";
        organizationsModification.setDeletedValues((List.of(new VisualizationItemValueImpl(deletedOrganization))));

        final VisualizationDeltaItemImpl givenNameModification = new VisualizationDeltaItemImpl(givenName);
        final String deletedGivenName = "Chuck";
        givenNameModification.setDeletedValues((List.of(new VisualizationItemValueImpl(deletedGivenName))));

        final String formattedModifications = this.formatter.formatProperties(List.of(
                organizationsModification, givenNameModification), 0);

        Assertions.assertThat(formattedModifications)
                .isEqualTo("""
                        Deleted properties:
                        |\tOrganizations: %s
                        |\tGiven name: %s""".formatted(deletedOrganization, deletedGivenName));
    }
}