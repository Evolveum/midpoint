package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class QuickSearchDropDown<T> extends DropDown<T> {
    public QuickSearchDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickUsers(){
        $(Schrodinger.byElementAttributeValue("a","value","Users"))
                .click();

        return this.getParent();
    }

    public T clickResources(){
        $(Schrodinger.byElementAttributeValue("a","value","Resources"))
                .click();

        return this.getParent();
    }

    public T clickTasks(){
        $(Schrodinger.byElementAttributeValue("a","value","Tasks")).
                click();

        return this.getParent();
    }
}
