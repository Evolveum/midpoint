package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.FocusPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.resource.ResourceWizardPage;
import com.evolveum.midpoint.schrodinger.page.role.RolePage;
import com.evolveum.midpoint.schrodinger.page.service.ServicePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import javax.management.relation.Role;

public class ChooseFocusTypeAndRelationModal<T> extends Component<T> {

    public ChooseFocusTypeAndRelationModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ChooseFocusTypeAndRelationModal<T> setType(String type) {
        getParentElement().$(Schrodinger.byDataId("type")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .selectOption(type);
        return this;
    }

    public String getType() {
        return getParentElement().$(Schrodinger.byDataId("type")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getSelectedOption().getText();
    }

    public ChooseFocusTypeAndRelationModal<T> setRelation(String relation) {
        getParentElement().$(Schrodinger.byDataId("relation")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .selectOption(relation);
        return this;
    }

    public BasicPage clickOk() {
        String selectedType = getType();
        if ("User".equals(selectedType)) {
            return new UserPage();
        } else if ("Organization".equals(selectedType)) {
            return new OrgPage();
        } else if ("Role".equals(selectedType)) {
            return new RolePage();
        } else if ("Service".equals(selectedType)) {
            return new ServicePage();
        } else if ("Resource".equals(selectedType)) {
            return new ResourceWizardPage();
        }
        return null;
    }

    public T clickCancel() {
        getParentElement().$(Schrodinger.byDataId("cancel")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return getParent();
    }
}
