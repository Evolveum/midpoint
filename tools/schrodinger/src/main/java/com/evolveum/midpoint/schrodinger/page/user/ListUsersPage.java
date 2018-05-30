package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListUsersPage extends BasicPage {

    public UsersPageTable<ListUsersPage> table() {
        SelenideElement box = $(By.cssSelector(".box.boxed-table.object-user-box"));

        return new UsersPageTable<>(this, box);
    }

    public FeedbackBox<ListUsersPage> feedback() {
        SelenideElement feedback = $(By.cssSelector("div.feedbackContainer"));

        return new FeedbackBox<>(this, feedback);
    }
}
