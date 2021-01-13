/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.common.table.TableRow;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author honchar
 */
public class ResultTab extends Component<TaskPage> {

    public ResultTab(TaskPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String getOperationValueByToken(String tokenValue){
        return getColumnValueByToken(tokenValue, "Operation");
    }

    public String getStatusValueByToken(String tokenValue){
        return getColumnValueByToken(tokenValue, "Status");
    }

    public String getTimestampValueByToken(String tokenValue){
        return getColumnValueByToken(tokenValue, "Timestamp");
    }

    public String getMessageValueByToken(String tokenValue){
        return getColumnValueByToken(tokenValue, "Message");
    }

    public String getColumnValueByToken(String tokenValue, String columnName) {
        TableRow row = getResultsTableRowByToken(tokenValue);
        if (row == null) {
            return null;
        }
        SelenideElement cell = row.getColumnCellElementByColumnName(columnName);
        if (cell == null) {
            return null;
        }
        return cell.getText();
    }

    public TableRow<ResultTab, Table<ResultTab>> getResultsTableRowByToken(String tokenValue) {
        return getResultsTable().rowByColumnResourceKey("pageTaskEdit.opResult.token", tokenValue);
    }

    public Table<ResultTab> getResultsTable() {
        return new Table<>(this, $(Schrodinger.byDataId("operationResult")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public ResultTab assertOperationValueByTokenMatch(String tokenValue, String expectedValue) {
        String realValue = getOperationValueByToken(tokenValue);
        if ((expectedValue == null && realValue != null) || (realValue == null && expectedValue != null) ||
                (expectedValue != null && !expectedValue.equals(realValue))) {
            throw new AssertionError("'Operation' value doesn't match to " + expectedValue);
        }
        return this;
    }

    public ResultTab assertStatusValueByTokenMatch(String tokenValue, String expectedValue) {
        String realValue = getStatusValueByToken(tokenValue);
        if ((expectedValue == null && realValue != null) || (realValue == null && expectedValue != null) ||
                (expectedValue != null && !expectedValue.equals(realValue))) {
            throw new AssertionError("'Status' value doesn't match to " + expectedValue);
        }
        return this;
    }

    public ResultTab assertTimestampValueByTokenMatch(String tokenValue, String expectedValue) {
        String realValue = getTimestampValueByToken(tokenValue);
        if ((expectedValue == null && realValue != null) || (realValue == null && expectedValue != null) ||
                (expectedValue != null && !expectedValue.equals(realValue))) {
            throw new AssertionError("'Timestamp' value doesn't match to " + expectedValue);
        }
        return this;
    }

    public ResultTab assertMessageValueByTokenMatch(String tokenValue, String expectedValue) {
        String realValue = getMessageValueByToken(tokenValue);
        if ((expectedValue == null && realValue != null) || (realValue == null && expectedValue != null) ||
                (expectedValue != null && !expectedValue.equals(realValue))) {
            throw new AssertionError("'Message' value doesn't match to " + expectedValue);
        }
        return this;
    }

}
