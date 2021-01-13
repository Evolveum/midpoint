/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.task.TaskBasicTab;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;


/**
 * Created by matus on 3/20/2018.
 */
public class FeedbackBox<T> extends Component<T> {

    public FeedbackBox(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SelenideElement getChildElement(String id){
        return getParentElement().$(Schrodinger.byDataId("div", id)).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);
    }

    public SelenideElement getChildElement(){
        return getParentElement().$(Schrodinger.byDataId("div", "0")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);
    }

    public Boolean isSuccess(String idOfChild) {

        return getChildElement(idOfChild).$(By.cssSelector("div.feedback-message.box.box-solid.box-success")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M).exists();

    }

    public Boolean isSuccess() {
        return  isSuccess("0");
    }

    public Boolean isWarning(String idOfChild) {

        return getChildElement(idOfChild).$(By.cssSelector("div.feedback-message.box.box-solid.box-warning")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M).exists();

    }

    public Boolean isWarning() {
        return  isWarning("0");
    }

    public Boolean isError(String idOfChild) {

        return getChildElement(idOfChild).$(By.cssSelector("div.feedback-message.box.box-solid.box-danger")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M).exists();

    }

    public Boolean isError() {
        return  isError("0");
    }

    public Boolean isInfo(String idOfChild) {

        return getChildElement(idOfChild).$(By.cssSelector("div.feedback-message.box.box-solid.box-info")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M).exists();

    }

    public Boolean isInfo() {
        return  isInfo("0");
    }

    public FeedbackBox<T> clickShowAll() {

        $(Schrodinger.byDataId("showAll")).click();

        return this;
    }

    public FeedbackBox<T> clickClose() {

        $(Schrodinger.byDataId("close")).click();

        return this;
    }

    public TaskBasicTab clickShowTask() {

        $(Schrodinger.byDataId("backgroundTask")).click();
        SelenideElement taskBasicTab = $(Schrodinger.byDataResourceKey("pageTask.basic.title"));
        return new TaskBasicTab(new TaskPage(), taskBasicTab);
    }

    public Boolean isFeedbackBoxPresent() {

        return getParentElement().isDisplayed();
    }

    public Boolean doesMessageExist(String messageText) {
        return $(By.linkText(messageText)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).exists();
    }

    public void assertSuccess() {
        if (!isSuccess()) {
            throw new AssertionError("Feedback panel status is not success.");
        }
    }

    public void assertError() {
        if (!isError()) {
            throw new AssertionError("Feedback panel status is not error.");
        }
    }

    public void assertWarning() {
        if (!isWarning()) {
            throw new AssertionError("Feedback panel status is not warning.");
        }
    }

    public void assertInfo() {
        if (!isInfo()) {
            throw new AssertionError("Feedback panel status is not info.");
        }
    }

}
