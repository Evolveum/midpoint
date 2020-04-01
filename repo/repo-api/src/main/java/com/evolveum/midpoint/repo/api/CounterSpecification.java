/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author katka
 *
 */
public class CounterSpecification implements DebugDumpable {

    private final AtomicInteger count = new AtomicInteger(0);
    private long counterStart;

    private String oid;
    private PolicyRuleType policyRule;
    private String policyRuleId;

    public CounterSpecification(String oid, String policyRuleId, PolicyRuleType policyRule) {
        this.oid = oid;
        this.policyRuleId = policyRuleId;
        this.policyRule = policyRule;
    }

    public int getCount() {
        return count.intValue();
    }

    public long getCounterStart() {
        return counterStart;
    }

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    public void setCounterStart(long counterStart) {
        this.counterStart = counterStart;
    }

    public PolicyThresholdType getPolicyThreshold() {
        return policyRule.getPolicyThreshold();
    }

    public String getPolicyRuleName() {
        return policyRule.getName();
    }

    public String getOid() {
        return oid;
    }

    public String getPolicyRuleId() {
        return policyRuleId;
    }

    public void reset(long currentTimeMillis) {
        resetCount();
        counterStart = currentTimeMillis;
    }

    public void resetCount() {
        count.set(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Counter for: ").append(oid).append(", policy rule: ").append(policyRule).append("\n");
        sb.append("Current count: ").append(count).append("\n");
        sb.append("Counter start: ").append(XmlTypeConverter.createXMLGregorianCalendar(counterStart)).append("\n");

        sb.append("Thresholds: \n").append(getPolicyThreshold().toString());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CounterSpecification{" +
                "oid='" + oid + '\'' +
                ", policyRuleId='" + policyRuleId + '\'' +
                ", count=" + count +
                ", counterStart=" + counterStart +
                '}';
    }
}
