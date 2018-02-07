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

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TabPanel<T> extends Component<T> {

    public TabPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SelenideElement clickTab(String resourceKey) {
        SelenideElement link = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey)).parent();
        link.shouldBe(Condition.visible);

        link.click();

        SelenideElement li = link.parent();
        li.shouldHave(Condition.cssClass("active"));

        return li.parent().parent().$(By.cssSelector(".tab-pane.active"));
    }

    public String getTabBadgeText(String resourceKey) {
        SelenideElement element = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey));
        element.shouldBe(Condition.visible);

        SelenideElement badge = element.$(Schrodinger.byDataId("small", "count"));
        badge.shouldBe(Condition.visible);

        return badge.getValue();
    }
}
