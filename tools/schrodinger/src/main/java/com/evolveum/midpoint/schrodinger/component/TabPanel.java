package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TabPanel<T> extends Component<T> {

    public TabPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SelenideElement clickTab(String resourceKey) {
        SelenideElement link = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey)).parent();
        link.shouldBe(Condition.visible);

        link.click();

        SelenideElement li = link.parent();
        li.shouldHave(Condition.cssClass("active"));

        return li.parent().parent().$(By.cssSelector(".tab-pane.active"));
    }

    public String getTabBadgeText(String resourceKey) {
        SelenideElement element = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey));
        element.shouldBe(Condition.visible);

        SelenideElement badge = element.$(Schrodinger.byDataId("small", "count"));
        badge.shouldBe(Condition.visible);

        return badge.getValue();
    }
}
