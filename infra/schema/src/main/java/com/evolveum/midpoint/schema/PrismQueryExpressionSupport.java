package com.evolveum.midpoint.schema;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.query.PrismQueryExpressionFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.google.common.base.Strings;

public class PrismQueryExpressionSupport implements PrismQueryExpressionFactory {

    private static final QName SCRIPT = new QName(ExpressionType.COMPLEX_TYPE.getNamespaceURI(), "script");
    private static final QName EXPRESSION = new QName(ExpressionType.COMPLEX_TYPE.getNamespaceURI(), "expression");
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
}
