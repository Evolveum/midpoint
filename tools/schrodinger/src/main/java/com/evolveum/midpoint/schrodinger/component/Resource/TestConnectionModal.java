package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.common.ModalBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 4/26/2018.
 */
public class TestConnectionModal<T> extends ModalBox<T> {

    private static final String CONNECTOR_INITIALIZATION_LABEL = "Connector initialization";
    private static final String CONNECTOR_CONFIGURATION_LABEL = "Connector configuration";
    private static final String CONNECTOR_CONNECTION_LABEL = "Connector connection";
    private static final String CONNECTOR_CAPABILITIES_LABEL = "Connector capabilities";
    private static final String RESOURCE_SCHEMA_LABEL = "Resource schema";

    public TestConnectionModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorInitialization() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_INITIALIZATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorConfiguration() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CONFIGURATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorConnection() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CONNECTION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackConnectorCapabilities() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CAPABILITIES_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    public FeedbackBox<TestConnectionModal> feedbackResourceSchema() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", RESOURCE_SCHEMA_LABEL))
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


        clickOk(); // Not sure if this is good practice

        return isSuccess;
    }

    public T clickOk() {

        $(Schrodinger.byDataId("ok"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T clickClose() {

        $(Schrodinger.byElementAttributeValue("a", "class", "w_close"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

}
