package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Table<T> extends Component<T> {

    public Table(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Search<T> search() {
        SelenideElement searchElement = getParentElement().$(By.cssSelector(".form-inline.pull-right.search-form"));

        return new Search(this, searchElement);
    }

    public Paging<T> paging() {
        SelenideElement pagingElement = getParentElement().$(By.className("boxed-table-footer-paging"));

        return new Paging(this, pagingElement);
    }
}
