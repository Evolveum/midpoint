package com.evolveum.midpoint.schrodinger.page.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.Popover;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.self.QuickSearch;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class HomePage extends BasicPage {

    public QuickSearch<HomePage> search() {
        SelenideElement searchElement = $(By.cssSelector("div.quicksearch-panel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new QuickSearch<HomePage>(this, searchElement);
    }
}
