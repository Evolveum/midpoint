package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.user.NewUserPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserHistoryTab extends Component<NewUserPage> {

    public UserHistoryTab(NewUserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
