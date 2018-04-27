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

import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTest extends TestBase {

    @Test
    public void testUserTablePaging() {
        ListUsersPage users = basicPage.listUsers();

        screenshot("listUsers");

        Paging paging = users
                .table()
                .paging();

        paging.pageSize(5);

        screenshot("paging");

        paging.next()
                .last()
                .previous()
                .first()
                .actualPagePlusOne()
                .actualPagePlusTwo()
                .actualPageMinusTwo()
                .actualPageMinusOne();
    }
}
