package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 3/22/2018.
 */
public class Popover<T> extends Component<T> {
    public Popover(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Popover<T> inputValue(String input) {
        getParentElement().$(By.cssSelector("input.form-control.input-sm")).setValue(input);

        return this;
    }

    public T updateSearch() {
        getParentElement().$(Schrodinger.byDataId("update")).click();
        getParentElement().$(Schrodinger.byDataId("update")).waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT);

        return this.getParent();
    }

    public T close() {
        getParentElement().$(Schrodinger.byDataId("close")).click();

        return this.getParent();
    }

    public Popover addAnotherValue() {
        //TODO

        return this;
    }

    public Popover removeValue(Integer i) {
        //TODO

        return this;
    }
}
