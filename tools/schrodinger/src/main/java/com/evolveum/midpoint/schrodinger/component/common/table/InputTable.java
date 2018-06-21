package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/22/2018.
 */
public class InputTable<T> extends Component<T>{
    public InputTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public InputTable<T> addAttributeValue(String attributeName, String attributeValue){
        System.out.println("The inner html: " + getParentElement().innerHtml());

        SelenideElement element = $(Schrodinger.byAncestorPrecedingSiblingElementValue("input","type","text",null,null,attributeName))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).setValue(attributeValue);

        return this;
    }


    public InputTable<T> clickCheckBox(String attributeName){

    $(Schrodinger.byAncestorPrecedingSiblingElementValue("input","type","checkbox",null,null,attributeName))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this;
    }


}
