package com.evolveum.midpoint.schrodinger.component.assignmentholder;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderObjectListPage<T extends AssignmentHolderObjectListTable> extends BasicPage {

    public abstract T table();

    protected SelenideElement getTableBoxElement(){
        SelenideElement box = $(By.cssSelector(".box.boxed-table.object-user-box"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return box;
    }

}
