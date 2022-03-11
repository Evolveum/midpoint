package com.evolveum.midpoint.schema;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PrismQueryExpressionFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.google.common.base.Strings;

public class PrismQueryExpressionSupport implements PrismQueryExpressionFactory {

    private static final QName SCRIPT = SchemaConstantsGenerated.C_SCRIPT;
    private static final QName EXPRESSION = SchemaConstantsGenerated.C_EXPRESSION;
    private static final QName PATH = SchemaConstantsGenerated.C_PATH;


    @Override
    public ExpressionWrapper parseScript(Map<String, String> namespaceContext, String language, String script) {
        ExpressionType expressionT = new ExpressionType();
        var scriptValue = new ScriptExpressionEvaluatorType();
        if (!Strings.isNullOrEmpty(language)) {
            scriptValue.setLanguage(language);
        }
        scriptValue.setCode(script);
        expressionT.expressionEvaluator(new JAXBElement<>(SCRIPT, ScriptExpressionEvaluatorType.class, scriptValue));
        return new ExpressionWrapper(EXPRESSION, expressionT);
    }

    @Override
    public ExpressionWrapper parsePath(ItemPath path) {
        ExpressionType expressionT = new ExpressionType();
        expressionT.expressionEvaluator(new JAXBElement<>(PATH, ItemPathType.class, new ItemPathType(path)));
        return new ExpressionWrapper(EXPRESSION, expressionT);
    }
}
