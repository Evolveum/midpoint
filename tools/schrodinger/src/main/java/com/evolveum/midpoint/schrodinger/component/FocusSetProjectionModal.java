package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.ModalBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/2/2018.
 */
public class FocusSetProjectionModal<T> extends ModalBox<T> {
    public FocusSetProjectionModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FocusTableWithChoosableElements<FocusSetProjectionModal<T>> projectionsTable() {
        SelenideElement resourcesBox = $(By.cssSelector("box boxed-table"));

        return new FocusTableWithChoosableElements<>(this, resourcesBox);
    }

    public T clickAdd() {

        $(Schrodinger.byDataResourceKey("userBrowserDialog.button.addButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

}
