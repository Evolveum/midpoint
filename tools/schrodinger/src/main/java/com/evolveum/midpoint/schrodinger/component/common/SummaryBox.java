package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 3/21/2018.
 */
public class SummaryBox<T> extends Component<T> {
    public SummaryBox(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String fetchDisplayName() {

        return $(Schrodinger.byDataId("summaryDisplayName")).getText();

    }

//    public String fetchCategory(){
//
//        return $(Schrodinger.byDataId("category")).getValue();
//
//    }
}
