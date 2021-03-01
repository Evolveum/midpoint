/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.modal.ModalBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Created by matus on 4/26/2018.
 */
public class TestConnectionModal<T> extends ModalBox<T> {

    private static final String CONNECTOR_INITIALIZATION_LABEL = "Connector initialization";
    private static final String CONNECTOR_CONFIGURATION_LABEL = "Connector configuration";
    private static final String CONNECTOR_CONNECTION_LABEL = "Connector connection";
    private static final String CONNECTOR_CAPABILITIES_LABEL = "Connector capabilities";
    private static final String RESOURCE_SCHEMA_LABEL = "Resource schema";

    private static final String MODAL_FEEDBACK_BOX_ID = "detailsBox";

    public TestConnectionModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    // TODO Possible difficulties with checking if error for specific FeedbackBox
    public FeedbackBox<TestConnectionModal> feedbackConnectorInitialization() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_INITIALIZATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    // TODO Possible difficulties with checking if error for specific FeedbackBox
    public FeedbackBox<TestConnectionModal> feedbackConnectorConfiguration() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CONFIGURATION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    // TODO Possible difficulties with checking if error for specific FeedbackBox
    public FeedbackBox<TestConnectionModal> feedbackConnectorConnection() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CONNECTION_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    // TODO Possible difficulties with checking if error for specific FeedbackBox
    public FeedbackBox<TestConnectionModal> feedbackConnectorCapabilities() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", CONNECTOR_CAPABILITIES_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }

    // TODO Possible difficulties with checking if error for specific FeedbackBox
    public FeedbackBox<TestConnectionModal> feedbackResourceSchema() {
        SelenideElement feedback = $(Schrodinger.byElementValue("b", "data-s-id", "messageLabel", RESOURCE_SCHEMA_LABEL))
                .parent()
                .parent()
                .parent();

        return new FeedbackBox<>(this, feedback);
    }


    public boolean isTestSuccess() {
        boolean isSuccess = false;
        $(Schrodinger.byDataId("div", "messagesPanel")).waitUntil(Condition.appears, MidPoint.TIMEOUT_MEDIUM_LONG_3_M);
//        Boolean isSuccess = feedbackConnectorInitialization().isSuccess()
//                && feedbackConnectorConfiguration().isSuccess()
//                && feedbackConnectorConnection().isSuccess()
//                && feedbackConnectorCapabilities().isSuccess()
//                && feedbackResourceSchema().isSuccess();

        ElementsCollection detailBoxes = $$(Schrodinger.byDataId("div", MODAL_FEEDBACK_BOX_ID));

        for (SelenideElement element : detailBoxes) {

            element.waitUntil(Condition.appears, MidPoint.TIMEOUT_MEDIUM_LONG_3_M);

            String attr = element.attr("class");

            if (attr != null && !attr.isEmpty()) {

                if (attr.contains("box-success")) {
                    isSuccess = true;
                } else {
                    isSuccess = false;
                    break;
                }
            }
        }

        clickOk(); // Not sure if this is good practice

        return isSuccess;
    }


    public boolean isTestFailure() {

        boolean isFailure = false;

        $(Schrodinger.byDataId("div", "messagesPanel")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);

        ElementsCollection detailBoxes = $$(Schrodinger.byDataId("div", MODAL_FEEDBACK_BOX_ID));

        for (SelenideElement element : detailBoxes) {
            element.waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);

            String attr = element.attr("class");

            if (attr != null && !attr.isEmpty()) {

                if (attr.contains("box-danger")) {
                    isFailure = true;
                    break;
                }

            }
        }

        clickOk(); // Not sure if this is good practice

        return isFailure;
    }

    public T clickOk() {

        $(Schrodinger.byDataId("ok"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public T clickClose() {

        $(Schrodinger.byElementAttributeValue("a", "class", "w_close"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public TestConnectionModal<T> assertIsTestSuccess() {
        assertion.assertTrue(isTestSuccess());
        return this;
    }

    public TestConnectionModal<T> assertIsTestFailure() {
        assertion.assertTrue(isTestFailure());
        return this;
    }

}
