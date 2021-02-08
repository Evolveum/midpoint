package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.configuration.ListRepositoryObjectsPage;
import com.evolveum.midpoint.schrodinger.page.configuration.RepositoryObjectPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$x;

public class ListRepositoryObjectsTable extends TableWithPageRedirect<ListRepositoryObjectsPage> {

    public ListRepositoryObjectsTable(ListRepositoryObjectsPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public RepositoryObjectPage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new RepositoryObjectPage();
    }

    @Override
    public ListRepositoryObjectsTable selectCheckboxByName(String name) {
        return null;
    }

    @Override
    protected TableHeaderDropDownMenu<ListRepositoryObjectsTable> clickHeaderActionDropDown() {
        return null;
    }

    public ListRepositoryObjectsTable deleteObject(String type, String name) {
        search()
                .dropDownPanelByItemName("Type")
                .inputDropDownValue(type)
                .updateSearch()
                .byName()
                .inputValue(name)
                .updateSearch();
        clickDeleteButton()
                .clickYes();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        and()
                .feedback()
                    .assertSuccess();
        return this;
    }

    private ConfirmationModal<ListRepositoryObjectsTable>  clickDeleteButton() {
        $x(".//a[@data-s-id='delete']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ConfirmationModal<>(this, Utils.getModalWindowSelenideElement());
    }
}
