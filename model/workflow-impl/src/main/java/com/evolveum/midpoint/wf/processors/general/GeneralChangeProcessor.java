package com.evolveum.midpoint.wf.processors.general;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.sun.org.apache.xerces.internal.util.DOMInputSource;
import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class GeneralChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralChangeProcessor.class);

    private static final String KEY_ACTIVATION_CONDITION = "activationCondition";
    private static final List<String> LOCALLY_KNOWN_KEYS = Arrays.asList(KEY_ACTIVATION_CONDITION);
    // TODO make this more robust
    public static final String ACTIVATION_CONDITION_PATH = "/configuration/midpoint/workflow/changeProcessors/generalChangeProcessor/" + KEY_ACTIVATION_CONDITION;

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    @Autowired
    private PrismContext prismContext;

    private ExpressionType startExpression;

    @PostConstruct
    public void init() {
        initializeBaseProcessor(LOCALLY_KNOWN_KEYS);

        XMLConfiguration xmlConfiguration = midpointConfiguration.getXmlConfiguration();
        Validate.notNull(xmlConfiguration, "XML version of midPoint configuration couldn't be found");

        XPath xpath = XPathFactory.newInstance().newXPath();
        Element result = null;
        try {
            Document config = xmlConfiguration.getDocument();
            result = (Element) xpath.evaluate(ACTIVATION_CONDITION_PATH, config, XPathConstants.NODE);
            startExpression = prismContext.getPrismJaxbProcessor().toJavaValue(result, ExpressionType.class);
        } catch (XPathExpressionException e) {
            throw new SystemException("Couldn't find activation condition in " + getBeanName() + " configuration due to an XPath problem", e);
        } catch (JAXBException e) {
            throw new SystemException("Couldn't find activation condition in " + getBeanName() + " configuration due to a JAXB problem", e);
        }
        LOGGER.info(getBeanName() + " initialized correctly; startExpression = " + startExpression);
    }

    @Override
    public HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException {

        Map<QName,Object> variables = new HashMap<QName,Object>();
        variables.put(new QName(SchemaConstants.NS_C, "context"), context);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Evaluating start expression, context = {}", context.debugDump());
        }

        boolean start;
        try {
            start = evaluateBooleanExpression(startExpression, variables, result);
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Couldn't evaluate generalChangeProcessor start condition", e);
        } catch (ExpressionEvaluationException e) {
            throw new SystemException("Couldn't evaluate generalChangeProcessor start condition", e);
        }

        if (!start) {
            LOGGER.trace("startExpression was evaluated to FALSE, we will not start the workflow");
            return HookOperationMode.FOREGROUND;
        }
        LOGGER.trace("Go for it!");

        return HookOperationMode.BACKGROUND;
    }

    private ExpressionFactory expressionFactory;

    private ExpressionFactory getExpressionFactory() {
        LOGGER.trace("Getting expressionFactory");
        ExpressionFactory ef = getBeanFactory().getBean("expressionFactory", ExpressionFactory.class);
        if (ef == null) {
            throw new IllegalStateException("expressionFactory bean cannot be found");
        }
        return ef;
    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, Map<QName, Object> expressionVariables, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, "workflow start condition", result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, "workflow start condition", result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = expression.evaluate(params);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Workflow start condition expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }


    @Override
    public void finishProcess(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PrismObject<? extends ObjectType> getRequestSpecificData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PrismObject<? extends ObjectType> getAdditionalData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getProcessInstanceDetailsPanelName(ProcessInstance processInstance) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
