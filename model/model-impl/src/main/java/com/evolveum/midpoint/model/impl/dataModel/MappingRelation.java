package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * @author mederly
 */
public class MappingRelation extends Relation {

	public static final int MAX_CONSTANT_WIDTH = 50;
	@NotNull private final MappingType mapping;

	public MappingRelation(@NotNull List<DataItem> sources, @Nullable DataItem target, @NotNull MappingType mapping) {
		super(sources, target);
		this.mapping = mapping;
	}

	@NotNull
	public MappingType getMapping() {
		return mapping;
	}

	@Override
	public String getEdgeLabel() {
		return getLabel("", false);
	}

	@Nullable
	private String getLabel(String defaultLabel, boolean showConstant) {
		ExpressionType expression = mapping.getExpression();
		if (expression == null || expression.getExpressionEvaluator().isEmpty()) {
			return defaultLabel;
		}
		if (expression.getExpressionEvaluator().size() > 1) {
			return "> 1 evaluator";
		}
		JAXBElement<?> evalElement = expression.getExpressionEvaluator().get(0);
		Object eval = evalElement.getValue();
		if (QNameUtil.match(evalElement.getName(), SchemaConstants.C_VALUE)) {
			if (showConstant) {
				if (eval instanceof RawType) {
					XNode xnode = ((RawType) eval).getXnode();
					if (xnode instanceof PrimitiveXNode) {
						eval = ((PrimitiveXNode) xnode).getStringValue();
					} else {
						eval = xnode.toString();
					}
				}
				return "\'" + StringUtils.abbreviate(String.valueOf(eval), MAX_CONSTANT_WIDTH) + "\'";
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

	@Override
	public String getNodeLabel(String defaultLabel) {
		return getLabel(defaultLabel, true);
	}
}
