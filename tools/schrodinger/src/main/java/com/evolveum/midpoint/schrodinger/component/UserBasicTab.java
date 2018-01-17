package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.user.NewUserPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserBasicTab extends Component<NewUserPage> {

    public UserBasicTab(NewUserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<UserBasicTab> form() {
        SelenideElement element = null;
        return new PrismForm<>(this, element);
    }
}
