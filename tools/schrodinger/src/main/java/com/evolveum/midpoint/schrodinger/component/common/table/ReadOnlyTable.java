/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class ReadOnlyTable<T> extends Component<T> {


    public ReadOnlyTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String getParameterValue(String parameterName){

   String value = $(Schrodinger.byPrecedingSiblingEnclosedValue("td",null,null,null,null,parameterName)).getText();

   return value;
    }

}
