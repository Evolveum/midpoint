package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTable;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/9/2018.
 */
public class FocusTableWithChoosableElements<T> extends AbstractTable<T> {
    public FocusTableWithChoosableElements(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public PrismForm<AbstractTable<T>> clickByName(String name) {
        return null;
    }

    @Override
    public AbstractTable<T> selectCheckboxByName(String name) {
        SelenideElement parent = $(Schrodinger.byElementValue(null, "data-s-id", "cell", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).parent();

        String row = parent.getAttribute("data-s-id").toString();

        System.out.println("The Parent + " + parent.innerHtml() + "                    The row" + row);

        parent.$(Schrodinger.byElementAttributeValue("input", "name", constructCheckBoxIdBasedOnRow(row)))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this;
    }


    private String constructCheckBoxIdBasedOnRow(String row) {
        StringBuilder constructCheckboxName = new StringBuilder("table:box:tableContainer:table:body:rows:")
                .append(row).append(":cells:1:cell:check");

        return constructCheckboxName.toString();
    }

    @Override
    public Search<FocusTableWithChoosableElements<T>> search() {
        SelenideElement searchElement = $(By.cssSelector(".form-inline.pull-right.search-form"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new Search<>(this, searchElement);
    }
}
