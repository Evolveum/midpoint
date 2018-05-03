package com.evolveum.midpoint.schrodinger.component.common;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.task.TaskBasicTab;
import com.evolveum.midpoint.schrodinger.page.task.EditTaskPage;
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

    public Boolean isSuccess() {

        return getParentElement().$(By.cssSelector("div.feedback-message.box.box-solid.box-success")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).exists();

    }

    public Boolean isWarning() {

        return getParentElement().$(By.cssSelector("div.feedback-message.box.box-solid.box-warning")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).exists();

    }

    public Boolean isError() {

        return getParentElement().$(By.cssSelector("div.feedback-message.box.box-solid.box-danger")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).exists();

    }

    public Boolean isInfo() {

        return getParentElement().$(By.cssSelector("div.feedback-message.box.box-solid.box-info")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).exists();

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
        SelenideElement taskBasicTab = $(Schrodinger.byDataResourceKey("pageTaskEdit.basic"));
        return new TaskBasicTab(new EditTaskPage(), taskBasicTab);
    }

    public Boolean isFeedbackBoxPresent() {

        return getParentElement().isDisplayed();
    }

}
