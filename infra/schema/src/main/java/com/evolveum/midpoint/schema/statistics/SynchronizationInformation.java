/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import javax.xml.namespace.QName;
import java.util.Date;

/**
 * @author Pavol Mederly
 */
public class SynchronizationInformation {
    
    private SynchronizationInformationType startValue;

    protected int countProtected;
    protected int countNoSynchronizationPolicy;
    protected int countSynchronizationDisabled;
    protected int countNotApplicableForTask;
    protected int countDeleted;
    protected int countDisputed;
    protected int countLinked;
    protected int countUnlinked;
    protected int countUnmatched;

    public SynchronizationInformation(SynchronizationInformationType value) {
        startValue = value;
    }

    public SynchronizationInformation() {
        this(null);
    }

    public SynchronizationInformationType getStartValue() {
        return (SynchronizationInformationType) startValue;
    }

    public SynchronizationInformationType getDeltaValue() {
        SynchronizationInformationType rv = toSynchronizationInformationType();
        rv.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        return rv;
    }

    public SynchronizationInformationType getAggregatedValue() {
        SynchronizationInformationType delta = toSynchronizationInformationType();
        SynchronizationInformationType rv = aggregate(startValue, delta);
        rv.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        return rv;
    }

    private SynchronizationInformationType aggregate(SynchronizationInformationType startValue, SynchronizationInformationType delta) {
        if (startValue == null) {
            return delta;
        }
        SynchronizationInformationType rv = new SynchronizationInformationType();
        addTo(rv, startValue);
        addTo(rv, delta);
        return rv;
    }

    public static void addTo(SynchronizationInformationType sum, SynchronizationInformationType delta) {
        sum.setCountProtected(sum.getCountProtected() + delta.getCountProtected());
        sum.setCountNoSynchronizationPolicy(sum.getCountNoSynchronizationPolicy() + delta.getCountNoSynchronizationPolicy());
        sum.setCountSynchronizationDisabled(sum.getCountSynchronizationDisabled() + delta.getCountSynchronizationDisabled());
        sum.setCountNotApplicableForTask(sum.getCountNotApplicableForTask() + delta.getCountNotApplicableForTask());
        sum.setCountDeleted(sum.getCountDeleted() + delta.getCountDeleted());
        sum.setCountDisputed(sum.getCountDisputed() + delta.getCountDisputed());
        sum.setCountLinked(sum.getCountLinked() + delta.getCountLinked());
        sum.setCountUnlinked(sum.getCountUnlinked() + delta.getCountUnlinked());
        sum.setCountUnmatched(sum.getCountUnmatched() + delta.getCountUnmatched());
    }

    protected SynchronizationInformationType toSynchronizationInformationType() {
        SynchronizationInformationType rv = new SynchronizationInformationType();
        toJaxb(rv);
        return rv;
    }

    private void toJaxb(SynchronizationInformationType rv) {
        rv.setCountProtected(countProtected);
        rv.setCountNoSynchronizationPolicy(countNoSynchronizationPolicy);
        rv.setCountSynchronizationDisabled(countSynchronizationDisabled);
        rv.setCountNotApplicableForTask(countNotApplicableForTask);
        rv.setCountDeleted(countDeleted);
        rv.setCountDisputed(countDisputed);
        rv.setCountLinked(countLinked);
        rv.setCountUnlinked(countUnlinked);
        rv.setCountUnmatched(countUnmatched);
    }

    public int getCountProtected() {
        return countProtected;
    }

    public void setCountProtected(int countProtected) {
        this.countProtected = countProtected;
    }

    public int getCountNoSynchronizationPolicy() {
        return countNoSynchronizationPolicy;
    }

    public void setCountNoSynchronizationPolicy(int countNoSynchronizationPolicy) {
        this.countNoSynchronizationPolicy = countNoSynchronizationPolicy;
    }

    public int getCountSynchronizationDisabled() {
        return countSynchronizationDisabled;
    }

    public void setCountSynchronizationDisabled(int countSynchronizationDisabled) {
        this.countSynchronizationDisabled = countSynchronizationDisabled;
    }

    public int getCountNotApplicableForTask() {
        return countNotApplicableForTask;
    }

    public void setCountNotApplicableForTask(int countNotApplicableForTask) {
        this.countNotApplicableForTask = countNotApplicableForTask;
    }

    public int getCountDeleted() {
        return countDeleted;
    }

    public void setCountDeleted(int countDeleted) {
        this.countDeleted = countDeleted;
    }

    public int getCountDisputed() {
        return countDisputed;
    }

    public void setCountDisputed(int countDisputed) {
        this.countDisputed = countDisputed;
    }

    public int getCountLinked() {
        return countLinked;
    }

    public void setCountLinked(int countLinked) {
        this.countLinked = countLinked;
    }

    public int getCountUnlinked() {
        return countUnlinked;
    }

    public void setCountUnlinked(int countUnlinked) {
        this.countUnlinked = countUnlinked;
    }

    public int getCountUnmatched() {
        return countUnmatched;
    }

    public void setCountUnmatched(int countUnmatched) {
        this.countUnmatched = countUnmatched;
    }

    public void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid, long started, Throwable exception, SynchronizationInformation increment) {
        countProtected += increment.countProtected;
        countNoSynchronizationPolicy += increment.countNoSynchronizationPolicy;
        countSynchronizationDisabled += increment.countSynchronizationDisabled;
        countNotApplicableForTask += increment.countNotApplicableForTask;
        countDeleted += increment.countDeleted;
        countDisputed += increment.countDisputed;
        countLinked += increment.countLinked;
        countUnlinked += increment.countUnlinked;
        countUnmatched += increment.countUnmatched;
    }

    public void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
        // noop
    }

}
