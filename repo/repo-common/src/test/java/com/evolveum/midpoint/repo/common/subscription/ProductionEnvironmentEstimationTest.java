/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Tests the estimation of "this is a production environment", i.e. {@link SubscriptionPolicies}
 * and the individual system features gathered by {@link SystemFeaturesEnquirer}.
 */
public class ProductionEnvironmentEstimationTest extends AbstractUnitTest {

    /** Non-demo, formally correct subscription. */
    private static final SubscriptionId REAL_SUBSCRIPTION =
            SubscriptionId.forTesting(SubscriptionId.Type.ANNUAL, "0126");

    private static final SubscriptionId DEMO_SUBSCRIPTION =
            SubscriptionId.forTesting(SubscriptionId.Type.DEMO, "0126");

    @BeforeClass
    public void initializePrism() throws Exception {
        // Needed to be able to create the system configuration beans below.
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testCustomColorsDetection() {
        assertColors("no system configuration", null, false);
        assertColors("empty system configuration", new SystemConfigurationType(), false);
        assertColors("empty deployment information", deployment(new DeploymentInformationType()), false);
        assertColors("deployment name only", deployment(new DeploymentInformationType().name("TEST")), false);
        assertColors("header color", deployment(new DeploymentInformationType().headerColor("#123456")), true);
        assertColors("blank header color", deployment(new DeploymentInformationType().headerColor("  ")), false);
        assertColors("skin", deployment(new DeploymentInformationType().skin("skin-blue")), true);
    }

    @Test
    public void testCustomLogoDetection() {
        assertLogo("no system configuration", null, false);
        assertLogo("empty system configuration", new SystemConfigurationType(), false);
        assertLogo("no logo", deployment(new DeploymentInformationType().headerColor("#123456")), false);
        assertLogo("empty logo", deployment(new DeploymentInformationType().logo(new IconType())), false);
        assertLogo("logo image", deployment(new DeploymentInformationType().logo(new IconType().imageUrl("logo.png"))), true);
        assertLogo("logo CSS class", deployment(new DeploymentInformationType().logo(new IconType().cssClass("fa fa-user"))), true);
        assertLogo("blank logo image", deployment(new DeploymentInformationType().logo(new IconType().imageUrl(" "))), false);
    }

    /** No indications at all: this is not a production environment. */
    @Test
    public void testNoIndications() {
        assertProduction("nothing", SubscriptionId.none(), features(), false);
        assertProduction("demo subscription only", DEMO_SUBSCRIPTION, features(), false);
    }

    /** A single indication is not enough; two of them are. */
    @Test
    public void testRegularIndications() {
        assertProduction("subscription only", REAL_SUBSCRIPTION, features(), false);
        assertProduction("custom logging only", SubscriptionId.none(),
                features().customLoggingDefined(true), false);
        assertProduction("subscription + custom logging", REAL_SUBSCRIPTION,
                features().customLoggingDefined(true), true);
        assertProduction("notifications + https URL", SubscriptionId.none(),
                features().realNotificationsEnabled(true).publicHttpsUrlPatternDefined(true), true);
    }

    /**
     * The customized look and feel is a single indication, no matter whether the colors, the logo, or both are set.
     * See MID-11977.
     */
    @Test
    public void testLookAndFeelIndication() {
        assertProduction("colors only", SubscriptionId.none(),
                features().customDeploymentColorsDefined(true), false);
        assertProduction("logo only", SubscriptionId.none(),
                features().customLogoDefined(true), false);
        assertProduction("colors + logo", SubscriptionId.none(),
                features().customDeploymentColorsDefined(true).customLogoDefined(true), false);
        assertProduction("colors + custom logging", SubscriptionId.none(),
                features().customDeploymentColorsDefined(true).customLoggingDefined(true), true);
        assertProduction("logo + subscription", REAL_SUBSCRIPTION,
                features().customLogoDefined(true), true);
    }

    /** In the case of an error we assume we are in production. */
    @Test
    public void testErrorState() {
        assertThat(SubscriptionState.error().isProductionEnvironment())
                .as("production environment in the error state")
                .isTrue();
    }

    private static SystemFeatures.Builder features() {
        return SystemFeatures.builder();
    }

    private static SystemConfigurationType deployment(DeploymentInformationType deploymentInformation) {
        return new SystemConfigurationType()
                .deploymentInformation(deploymentInformation);
    }

    private void assertColors(String message, SystemConfigurationType configuration, boolean expected) {
        assertThat(enquiry(configuration).isCustomDeploymentColorsDefined())
                .as("custom colors defined for '%s'", message)
                .isEqualTo(expected);
    }

    private void assertLogo(String message, SystemConfigurationType configuration, boolean expected) {
        assertThat(enquiry(configuration).isCustomLogoDefined())
                .as("custom logo defined for '%s'", message)
                .isEqualTo(expected);
    }

    /** The look-and-feel related methods do not need any of the components autowired into the enquirer. */
    private SystemFeaturesEnquirer.Enquiry enquiry(SystemConfigurationType configuration) {
        return new SystemFeaturesEnquirer().new Enquiry(configuration);
    }

    private void assertProduction(
            String message, SubscriptionId subscriptionId, SystemFeatures.Builder features, boolean expected) {
        assertThat(SubscriptionPolicies.isProductionEnvironment(subscriptionId, features.build()))
                .as("production environment for '%s'", message)
                .isEqualTo(expected);
    }
}
