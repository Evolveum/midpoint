/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;


import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Table<T> extends Component<T> {

    public Table(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }


    public Search<? extends Table> search() {
        SelenideElement searchElement = getParentElement().$(By.cssSelector(".form-inline.pull-right.search-form"));

        return new Search<>(this, searchElement);
    }

    public Paging<T> paging() {
        SelenideElement pagingElement = getParentElement().$(By.className("boxed-table-footer-paging"));

        return new Paging(this, pagingElement);
    }

    public Table<T> selectAll() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("input", "type", "checkbox", "data-s-id", "topToolbars"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this;
    }

    public boolean currentTableContains(String name) {
// Small time out period, might be a problem if table loads for a long time
// TODO Catching the exception in such way might be a problem, find solution with wait+pooling interval


        FluentWait wait = MidPoint.waitWithIgnoreMissingElement();
        Boolean isPresent = (Boolean) wait.until(new Function() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Object o) {

                return $(Schrodinger.byElementValue("Span", name)).is(Condition.visible);
            }
        });

        return isPresent; //$(Schrodinger.byElementValue("Span", name)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT).is(Condition.visible);

    }
//    private boolean isDisplayed(SelenideElement e) {
//        try {
//            Predicate<WebDriver> isAppear = condition -> e.is(Condition.appears);
//            Selenide.Wait()
//                    .withTimeout(MidPoint.TIMEOUT_DEFAULT, TimeUnit.SECONDS)
//                    .until(isAppear,Boolean.class);
//            return true;
//        } catch (TimeoutException th) {
//            return false;
//        }
    //   }

}
