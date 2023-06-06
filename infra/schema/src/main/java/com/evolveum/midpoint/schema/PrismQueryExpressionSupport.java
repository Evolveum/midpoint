package com.evolveum.midpoint.schema;

import java.util.Map;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PrismQueryExpressionFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.google.common.base.Strings;

public class PrismQueryExpressionSupport implements PrismQueryExpressionFactory {

    private static final QName SCRIPT = SchemaConstantsGenerated.C_SCRIPT;
    private static final QName EXPRESSION = SchemaConstantsGenerated.C_EXPRESSION;
    private static final QName PATH = SchemaConstantsGenerated.C_PATH;
    private static final String YAML = "yaml";
    private static final String CONST = "const";



    @Override
    public ExpressionWrapper parseScript(Map<String, String> namespaceContext, String language, String script) {
        ExpressionType expressionT = new ExpressionType();
        var scriptValue = new ScriptExpressionEvaluatorType();
        if (YAML.equals(language)) {
            return parseYamlScript(script, namespaceContext);
        }
        if (CONST.equals(language)) {
            return parseConstScript(script, namespaceContext);
        }

        if (!Strings.isNullOrEmpty(language)) {
            scriptValue.setLanguage(language);
        }
        scriptValue.setCode(script);
        expressionT.expressionEvaluator(new JAXBElement<>(SCRIPT, ScriptExpressionEvaluatorType.class, scriptValue));
        return new ExpressionWrapper(EXPRESSION, expressionT);
    }

    private ExpressionWrapper parseConstScript(String constant, Map<String, String> namespaceContext) {
        ExpressionType expressionT = new ExpressionType();
        var expr = new ConstExpressionEvaluatorType();
        expr.setValue(constant);
        expressionT.expressionEvaluator(new JAXBElement<>(SchemaConstantsGenerated.C_CONST, ConstExpressionEvaluatorType.class, expr));
        return new ExpressionWrapper(EXPRESSION, expressionT);
    }

    private ExpressionWrapper parseYamlScript(String script, Map<String, String> namespaceContext) {
        ItemDefinition<?> expression = PrismContext.get().getSchemaRegistry().findItemDefinitionByElementName(EXPRESSION);
        try {
            // FIXME: Add namespaces
            script = "expression:\n" + script;
            PrismValue value = PrismContext.get().parserFor(script).yaml().definition(expression).parseItemValue();
            return new ExpressionWrapper(EXPRESSION, value.getRealValue());
        } catch (SchemaException e) {
            throw new TunnelException(e);
        }
    }

    @Override
    public ExpressionWrapper parsePath(ItemPath path) {
        ExpressionType expressionT = new ExpressionType();
        expressionT.expressionEvaluator(new JAXBElement<>(PATH, ItemPathType.class, new ItemPathType(path)));
        return new ExpressionWrapper(EXPRESSION, expressionT);
    }
}
