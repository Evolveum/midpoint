package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class QuickSearch<T> extends Component<T> {
    public QuickSearch(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public QuickSearch<T> inputValue(String name){
        $(Schrodinger.byElementAttributeValue("input","name","searchInput")).setValue(name);

        return this;
    }

    //TODO rethink
    public Table clickSearch(){
        $(Schrodinger.byElementAttributeValue("button","data-s-id","searchButton"))
                .click();

        return new Table("null",null);
    }

    public QuickSearchDropDown<QuickSearch<T>> clickSearchFor(){
        $(Schrodinger.bySelfOrDescendantElementAttributeValue("button","data-toggle","dropdown","class","sr-only"))
                .waitUntil(Condition.appears,MidPoint.TIMEOUT_DEFAULT).click();
        SelenideElement dropDown = $(Schrodinger.byElementAttributeValue("ul","role","menu"))
                .waitUntil(Condition.visible,MidPoint.TIMEOUT_DEFAULT);

        return new QuickSearchDropDown<>(this,dropDown);
    }
}
