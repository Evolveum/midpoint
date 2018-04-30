package com.evolveum.midpoint.schrodinger.component.Resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.common.ModalBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 4/26/2018.
 */
public class TestConnectionModal extends ModalBox {

    private static final String CONNECTOR_INITIALIZATION_LABEL = "Connector initialization";
    private static final String CONNECTOR_CONFIGURATION_LABEL = "Connector configuration";
    private static final String CONNECTOR_CONNECTION_LABEL = "Connector connection";
    private static final String CONNECTOR_CAPABILITIES_LABEL = "Connector capabilities";
    private static final String RESOURCE_SCHEMA_LABEL = "Resource schema";

    public TestConnectionModal(Object parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorInitialization() {
        SelenideElement feedback = $(Schrodinger.byElementEnclosedTextValue("b", "data-s-id", "messageLabel", CONNECTOR_INITIALIZATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorConfiguration() {
        SelenideElement feedback = $(Schrodinger.byElementEnclosedTextValue("b", "data-s-id", "messageLabel", CONNECTOR_CONFIGURATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorConnection() {
        SelenideElement feedback = $(Schrodinger.byElementEnclosedTextValue("b", "data-s-id", "messageLabel", CONNECTOR_CONNECTION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorCapabilities() {
        SelenideElement feedback = $(Schrodinger.byElementEnclosedTextValue("b", "data-s-id", "messageLabel", CONNECTOR_CAPABILITIES_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackResourceSchema() {
        SelenideElement feedback = $(Schrodinger.byElementEnclosedTextValue("b", "data-s-id", "messageLabel", RESOURCE_SCHEMA_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }


    public boolean isTestSuccess() {
        Boolean isSuccess = feedbackConnectorInitialization().isSuccess()
                && feedbackConnectorConfiguration().isSuccess()
                && feedbackConnectorConnection().isSuccess()
                && feedbackConnectorCapabilities().isSuccess()
                && feedbackResourceSchema().isSuccess();

        return isSuccess;
    }

}
