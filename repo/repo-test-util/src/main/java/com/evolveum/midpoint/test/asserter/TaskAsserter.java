/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.testng.AssertJUnit.assertEquals;

public class TaskAsserter<RA> extends PrismObjectAsserter<TaskType, RA> {

    private TaskAsserter(PrismObject<TaskType> object) {
        super(object);
    }

    private TaskAsserter(PrismObject<TaskType> object, String details) {
        super(object, details);
    }

    @SuppressWarnings("unused")
    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object) {
        return new TaskAsserter<>(object);
    }

    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object, String details) {
        return new TaskAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public TaskAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public TaskAsserter<RA> display() {
        super.display();
        return this;
    }

    public TaskAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    public TaskAsserter<RA> assertProgress(long expected) {
        long actual = defaultIfNull(getObject().asObjectable().getProgress(), 0L);
        assertEquals("Wrong progress", expected, actual);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        return (TaskAsserter<RA>) super.assertPolyStringProperty(propName, expectedOrig);
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> synchronizationInformation() {
        OperationStatsType operationStats = getObject().asObjectable().getOperationStats();
        SynchronizationInformationType synchronizationInfo = operationStats != null ?
                operationStats.getSynchronizationInformation() : new SynchronizationInformationType();
        SynchronizationInfoAsserter<TaskAsserter<RA>> asserter = new SynchronizationInfoAsserter<>(synchronizationInfo, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public IterativeTaskInfoAsserter<TaskAsserter<RA>> iterativeTaskInformation() {
        OperationStatsType operationStats = getObject().asObjectable().getOperationStats();
        IterativeTaskInformationType iterativeTaskInfo = operationStats != null ?
                operationStats.getIterativeTaskInformation() : new IterativeTaskInformationType();
        IterativeTaskInfoAsserter<TaskAsserter<RA>> asserter = new IterativeTaskInfoAsserter<>(iterativeTaskInfo, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
