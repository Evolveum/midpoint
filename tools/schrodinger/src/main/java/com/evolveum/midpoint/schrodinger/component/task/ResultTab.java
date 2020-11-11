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
        TableRow row = getResultsTableRowByToken(tokenValue);
        return row.getColumnCellElementByColumnName("Operation").getValue();
    }

    public String getStatusValueByToken(String tokenValue){
        TableRow row = getResultsTableRowByToken(tokenValue);
        return row.getColumnCellElementByColumnName("Status").getValue();
    }

    public String getTimestampValueByToken(String tokenValue){
        TableRow row = getResultsTableRowByToken(tokenValue);
        return row.getColumnCellElementByColumnName("Timestamp").getValue();
    }

    public String getMessageValueByToken(String tokenValue){
        TableRow row = getResultsTableRowByToken(tokenValue);
        return row.getColumnCellElementByColumnName("Message").getValue();
    }

    public TableRow<ResultTab, Table<ResultTab>> getResultsTableRowByToken(String tokenValue) {
        return getResultsTable().rowByColumnResourceKey("pageTaskEdit.opResult.token", tokenValue);
    }

    public Table<ResultTab> getResultsTable() {
        return new Table<>(this, $(Schrodinger.byDataId("operationResult")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
