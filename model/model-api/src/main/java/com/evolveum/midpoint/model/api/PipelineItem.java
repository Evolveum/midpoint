/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class PipelineItem implements DebugDumpable, Serializable {

    @NotNull private PrismValue value;
    @NotNull private OperationResult result;
    // variables here are to be cloned-on-use (if they are not immutable)
    @NotNull private final VariablesMap variables = new VariablesMap();

    public PipelineItem(@NotNull PrismValue value, @NotNull OperationResult result) {
        this.value = value;
        this.result = result;
    }

    public PipelineItem(@NotNull PrismValue value, @NotNull OperationResult result, @NotNull VariablesMap variables) {
        this.value = value;
        this.result = result;
        this.variables.putAll(variables);
    }

    @NotNull
    public PrismValue getValue() {
        return value;
    }

    public void setValue(@NotNull PrismValue value) {
        this.value = value;
    }

    @NotNull
    public OperationResult getResult() {
        return result;
    }

    public void setResult(@NotNull OperationResult result) {
        this.result = result;
    }

    @NotNull
    public VariablesMap getVariables() {
        return variables;
    }

    // don't forget to clone on use
//    @SuppressWarnings("unchecked")
//    public <X> X getVariable(String name) {
//        return (X) variables.get(name);
//    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "value", value, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "result", result, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "variables", result, indent+1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "PipelineItem{" +
                "value=" + value +
                ", result status is " + result.getStatus() +
                ", variables: " + variables.size() +
                '}';
    }

    public void computeResult() {
        result.computeStatus();
    }

    public PipelineItem cloneMutableState() {
        // note that variables are cloned on use
        return new PipelineItem(value, result.clone(), variables);
    }
}
