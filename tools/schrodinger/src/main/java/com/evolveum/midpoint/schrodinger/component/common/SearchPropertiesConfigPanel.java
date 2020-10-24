package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.common.table.TableRow;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byText;

public class SearchPropertiesConfigPanel<T> extends Component<T> {

    public SearchPropertiesConfigPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SearchPropertiesConfigPanel<T> addPropertyToTable(String propertyName) {
        getPropertyChoiceElement().selectOption(propertyName);
        getParentElement().$(Schrodinger.byDataId("a", "addButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        getPropertiesTable().getParentElement().$(byText(propertyName)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public SearchPropertiesConfigPanel<T> removePropertyFromTable(String propertyName) {
        TableRow rowToDelete = getPropertiesTable().rowByColumnResourceKey("SearchPropertiesConfigPanel.table.column.property", propertyName);
        rowToDelete.getInlineMenu().clickInlineMenuButtonByTitle("Delete");
        getPropertiesTable().getParentElement().$(byText(propertyName)).waitUntil(Condition.disappear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    private DropDown<SearchPropertiesConfigPanel> getPropertyChoiceElement(){
        SelenideElement propertyChoiceElement = getParentElement().$(Schrodinger.bySelfOrAncestorElementAttributeValue("select",
                "class", "form-control input-sm", "data-s-id", "propertyChoice"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        DropDown<SearchPropertiesConfigPanel> dropDown = new DropDown<>(this, propertyChoiceElement);
        return dropDown;
    }

    public Table<SearchPropertiesConfigPanel> getPropertiesTable() {
        SelenideElement tableElement = getParentElement().$(By.tagName("table")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        Table propertiesTable = new Table(this, tableElement);
        return propertiesTable;
    }

    public SearchPropertiesConfigPanel<T> setPropertyTextValue(String propertyName, String value, boolean addPropertyIfAbsent) {
        TableRow propertyRow = getTableRowForProperty(propertyName, addPropertyIfAbsent);
        propertyRow.setTextToInputFieldByColumnName("Value", value);
        return this;
    }

    public SearchPropertiesConfigPanel<T> setPropertyFilterValue(String propertyName, String filterValue, boolean addPropertyIfAbsent) {
        TableRow propertyRow = getTableRowForProperty(propertyName, addPropertyIfAbsent);
        propertyRow.setValueToDropDownByColumnName("Filter", filterValue);
        return this;
    }

    public SearchPropertiesConfigPanel<T> setPropertyMatchingTuleValue(String propertyName, String filterValue, boolean addPropertyIfAbsent) {
        TableRow propertyRow = getTableRowForProperty(propertyName, addPropertyIfAbsent);
        propertyRow.setValueToDropDownByColumnName("Filter", filterValue);
        return this;
    }

    public SearchPropertiesConfigPanel<T> clickNegotiateCheckbox(String propertyName, boolean addPropertyIfAbsent) {
        TableRow propertyRow = getTableRowForProperty(propertyName, addPropertyIfAbsent);
        propertyRow.clickCheckBoxByColumnName("Negotiate");
        return this;
    }

    public T confirmConfiguration() {
        getParentElement().$(Schrodinger.byDataId("okButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        getParentElement().waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return getParent();
    }

    private TableRow getTableRowForProperty(String propertyName, boolean addPropertyIfAbsent) {
        TableRow propertyRow = getPropertiesTable().rowByColumnLabel("Property", propertyName);
        if (propertyRow == null && !addPropertyIfAbsent) {
            return null;
        }
        if (propertyRow == null && addPropertyIfAbsent) {
            addPropertyToTable(propertyName);
            propertyRow = getPropertiesTable().rowByColumnLabel("Property", propertyName);
        }
        return propertyRow;
    }
}
