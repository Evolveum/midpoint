package com.evolveum.midpoint.schema;

import java.util.Map;

import com.evolveum.midpoint.prism.*;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

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

    private static final String YAML_PREAMBLE = "---\n";



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

    @Override
    public void serializeExpression(ExpressionWriter writer, ExpressionWrapper wrapper) throws SchemaException {
        PrismPropertyDefinition<ExpressionType> expressionDef =  PrismContext.get().getSchemaRegistry().findPropertyDefinitionByElementName(EXPRESSION);
        var expression = wrapper.getExpression();
        if (expression instanceof ExpressionType expressionType) {
            if (isSimple(expressionType)) {
                var evaluator = extractEvaluator(expressionType);

                //var serialized = PrismContext.get().serializerFor(YAML).serialize(wrapper.getExpression());

                // Detect simple const
                if (evaluator instanceof ConstExpressionEvaluatorType constEvaluator) {
                    writer.writeConst(constEvaluator.getValue());
                    return;
                }
                // Detect  simple variable
                if (evaluator instanceof ItemPathType path) {
                    writer.writeVariable(path.getItemPath());
                    return;
                }
                if (evaluator instanceof ScriptExpressionEvaluatorType script) {
                    // Detect simple groovy
                    if (isSimple(script)) {
                        writer.writeScript(script.getLanguage(), script.getCode());
                        return;
                    }

                }
            }
            // Fallback (YAML serialization)
            var prop = PrismContext.get().itemFactory().createProperty(EXPRESSION, expressionDef);
            prop.setRealValue(expressionType);
            var serialized = withoutYamlPreamble(PrismContext.get().serializerFor(YAML).serialize(prop));
            writer.writeScript(YAML, serialized);
        }

    }

    private String withoutYamlPreamble(String serialize) {
        if (serialize.startsWith(YAML_PREAMBLE)) {
            return serialize.substring(YAML_PREAMBLE.length());
        }
        return serialize;
    }

    private void writeCode(ExpressionWriter writer, String language, String code) {


    }

    private Object extractEvaluator(ExpressionType type) {
        var jaxbElemList = type.getExpressionEvaluator();
        if (jaxbElemList == null) {
            // Or should be fail?
            return null;
        }
        if (jaxbElemList.isEmpty()) {
            // Or should be fail?
            return null;
        }
        return jaxbElemList.get(0).getValue();
    }

    private boolean isSimple(ExpressionType type) {
        if (type.getReturnType() != null) {
            return false;
        }
        if (type.isTrace() != null) {
            return false;
        }
        if (type.getName()  != null) {
            return false;
        }
        if (type.getReturnMultiplicity() != null) {
            return false;
        }
        if (type.getQueryInterpretationOfNoValue() != null) {
            return false;
        }
        if (type.getRunAsRef() != null) {
            return false;
        }
        if (type.getDescription() != null) {
            return false;
        }
        if (!type.getParameter().isEmpty()) {
            return false;
        }
        if (type.getPrivileges() != null) {
            return false;
        }
        if (!type.getStringFilter().isEmpty()) {
            return false;
        }
        if (type.getExtension() != null) {
            return false;
        }
        if (type.getDocumentation() != null) {
            return false;
        }
        return true;
    }

    private boolean isSimple(ScriptExpressionEvaluatorType type) {
        if (type.getDescription() != null) {
            return false;
        }
        if (type.getDocumentation() != null) {
            return false;
        }
        if (type.getReturnType() != null) {
            return false;
        }
        if (type.getCondition() != null) {
            return false;
        }
        if (type.getRelativityMode() != null) {
            return false;
        }
        if (type.getObjectVariableMode() != null) {
            return false;
        }
        if (type.getValueVariableMode() != null) {
            return false;
        }
        return true;
    }
}
