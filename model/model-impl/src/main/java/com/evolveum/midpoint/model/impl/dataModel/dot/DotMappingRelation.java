/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import jakarta.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.dataModel.model.MappingRelation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class DotMappingRelation implements DotRelation {

    private static final int MAX_CONSTANT_WIDTH = 50;

    @NotNull private final MappingRelation mappingRelation;

    DotMappingRelation(@NotNull MappingRelation mappingRelation) {
        this.mappingRelation = mappingRelation;
    }

    @Override
    public String getEdgeLabel() {
        return getLabel("", false);
    }

    @Nullable
    private String getLabel(String defaultLabel, boolean showConstant) {
        ExpressionType expression = getMapping().getExpression();
        if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
            return defaultLabel;
        }
        if (expression.getExpressionEvaluator().size() > 1) {
            return "> 1 evaluator";        // TODO multivalues
        }
        JAXBElement<?> evalElement = expression.getExpressionEvaluator().get(0);
        Object eval = evalElement.getValue();
        if (QNameUtil.match(evalElement.getName(), SchemaConstants.C_VALUE)) {
            if (showConstant) {
                String str = getStringConstant(eval);
                return '\'' + StringUtils.abbreviate(str, MAX_CONSTANT_WIDTH) + '\'';
            } else {
                return "constant";
            }
        } else if (eval instanceof AsIsExpressionEvaluatorType) {
            return defaultLabel;
        } else if (eval instanceof ScriptExpressionEvaluatorType) {
            ScriptExpressionEvaluatorType script = (ScriptExpressionEvaluatorType) eval;
            if (script.getLanguage() == null) {
                return "groovy";
            } else {
                return StringUtils.substringAfter(script.getLanguage(), "#");
            }
        } else {
            return evalElement.getName().getLocalPart();
        }
    }

    private MappingType getMapping() {
        return mappingRelation.getMapping();
    }

    private String getStringConstant(Object eval) {
        if (eval instanceof RawType) {
            return ((RawType) eval).extractString();
        } else {
            return String.valueOf(eval);
        }
    }

    @Override
    public String getNodeLabel(String defaultLabel) {
        return getLabel(defaultLabel, true);
    }

    @Override
    public String getEdgeStyle() {
        switch (getMapping().getStrength() != null ? getMapping().getStrength() : MappingStrengthType.NORMAL) {
            case NORMAL:
                return "dashed";
            case STRONG:
                return "solid";
            case WEAK:
                return "dotted";
        }
        return "";
    }

    @Override
    public String getNodeTooltip() {
        String lines = getTooltipString().trim();
        lines = lines.replace("\n", DotModel.LF);
        lines = lines.replace("\"", "\\\"");
        return lines;
    }

    private String getTooltipString() {
        ExpressionType expression = getMapping().getExpression();
        if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
            return "asIs";
        }
        JAXBElement<?> evalElement = expression.getExpressionEvaluator().get(0);
        Object eval = evalElement.getValue();
        if (QNameUtil.match(evalElement.getName(), SchemaConstants.C_VALUE)) {
            return getStringConstant(eval);
        } else if (eval instanceof AsIsExpressionEvaluatorType) {
            return "asIs";
        } else if (eval instanceof ScriptExpressionEvaluatorType) {
            return ((ScriptExpressionEvaluatorType) eval).getCode();
        } else if (eval instanceof ItemPathType) {
            return String.valueOf(((ItemPathType) eval).getItemPath());
        } else {
            return "";
        }
    }

    @Override
    public String getEdgeTooltip() {
        return getNodeTooltip();
    }

    @Override
    public String getNodeStyleAttributes() {
        ExpressionType expression = getMapping().getExpression();
        if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
            return "";
        }
        JAXBElement<?> evalElement = expression.getExpressionEvaluator().get(0);
        Object eval = evalElement.getValue();
        if (QNameUtil.match(evalElement.getName(), SchemaConstants.C_VALUE)) {
            return "style=filled, fillcolor=ivory";
        } else if (eval instanceof AsIsExpressionEvaluatorType) {
            return "";
        } else if (eval instanceof ScriptExpressionEvaluatorType) {
            return "style=filled, fillcolor=wheat";
        } else {
            return "";
        }
    }

}
