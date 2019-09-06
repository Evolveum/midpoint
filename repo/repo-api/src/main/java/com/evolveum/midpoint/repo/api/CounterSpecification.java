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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 *
 */
public class CounterSpecification implements DebugDumpable {
	
	private int count = 0;
	private long counterStart;
	
	private TaskType task;
	private PolicyRuleType policyRule;
	private String policyRuleId;
	
	public CounterSpecification(TaskType task, String policyRuleId, PolicyRuleType policyRule) {
		this.task = task;
		this.policyRuleId = policyRuleId;
		this.policyRule = policyRule;
	}
	
	public int getCount() {
		return count;
	}
	public long getCounterStart() {
		return counterStart;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public void setCounterStart(long counterStart) {
		this.counterStart = counterStart;
	}
	
	public PolicyThresholdType getPolicyThreshold() {
		return policyRule.getPolicyThreshold();
	}
	
	public String getTaskName() {
		return task.getName().getOrig();
	}
	
	public String getPolicyRuleName() {
		return policyRule.getName();
	}
	
	public String getTaskOid() {
		return task.getOid();
	}
	
	public String getPolicyRuleId() {
		return policyRuleId;
	}
	
	
	public void reset(long currentTimeMillis) {
		count = 0;
		counterStart = currentTimeMillis;
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("Counter for: ").append(task).append(", policy rule: ").append(policyRule).append("\n");
		sb.append("Current count: ").append(count).append("\n");
		sb.append("Counter start: ").append(XmlTypeConverter.createXMLGregorianCalendar(counterStart)).append("\n");
		
		sb.append("Thresholds: \n").append(getPolicyThreshold().toString());
		return sb.toString();
	}
	
}
