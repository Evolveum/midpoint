package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * Created by honchar
 */
public class RoleManagementTab extends Component<SystemPage> {

    public RoleManagementTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<RoleManagementTab> form(){
        return new PrismForm<>(this, null);
    }
}
