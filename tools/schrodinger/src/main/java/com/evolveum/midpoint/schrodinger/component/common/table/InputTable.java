/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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

        SelenideElement element = $(Schrodinger.byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue("input","type","text",null,null,attributeName))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(attributeValue);

        return this;
    }


    public InputTable<T> clickCheckBox(String attributeName){

    $(Schrodinger.byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue("input","type","checkbox",null,null,attributeName))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }


}
