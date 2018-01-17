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

package schrodinger;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageTest extends TestBase {

    @Test
    public void changeLanguage() {
        basicPage.loggedUser().logout();
        LoginPage login = midPoint.login();

        login.changeLanguage("de");

        $(By.cssSelector(".btn.btn-primary")).shouldHave(Condition.value("Anmelden"));
    }
}
