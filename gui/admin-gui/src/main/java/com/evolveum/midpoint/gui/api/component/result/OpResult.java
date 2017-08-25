/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.component.result;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class OpResult implements Serializable, Visitable {

	private static final Trace LOGGER = TraceManager.getTrace(OpResult.class);

	private static final String OPERATION_CHECK_TASK_VISIBILITY = OpResult.class.getName() + ".checkTaskVisibility";

    private OperationResultStatus status;
    private String operation;
    private String message;
    private List<Param> params;
    private List<Context> contexts;
    private String exceptionMessage;
    private String exceptionsStackTrace;
    private List<OpResult> subresults;
	private OpResult parent;
    private int count;
    private String xml;

	// we assume there is at most one background task created (TODO revisit this assumption)
	private String backgroundTaskOid;
	private Boolean backgroundTaskVisible;			// available on root opResult only
    
    private boolean showMore;
    private boolean showError;
    
    private boolean alreadyShown;
    
    public boolean isAlreadyShown() {
		return alreadyShown;
	}
    
    public void setAlreadyShown(boolean alreadyShown) {
		this.alreadyShown = alreadyShown;
	}
    
        public static OpResult getOpResult(PageBase page, OperationResult result){
        OpResult opResult = new OpResult();
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        if (result.getCause() != null && result.getCause() instanceof CommonException){
        	LocalizableMessage localizableMessage = ((CommonException) result.getCause()).getUserFriendlyMessage();
        	if (localizableMessage != null) {
        		String key = localizableMessage.getKey() != null ? localizableMessage.getKey() : localizableMessage.getFallbackMessage();
        		StringResourceModel stringResourceModel = new StringResourceModel(key, page).setModel(new Model<String>()).setDefaultValue(localizableMessage.getFallbackMessage())
				.setParameters(localizableMessage.getArgs());
        		opResult.message = stringResourceModel.getString();
        	}
        } 
        
        if (opResult.message == null) {
        	opResult.message = result.getMessage();
        }
        opResult.operation = result.getOperation();
        opResult.status = result.getStatus();
        opResult.count = result.getCount();

        if (result.getCause() != null) {
            Throwable cause = result.getCause();
            opResult.exceptionMessage = cause.getMessage();

            Writer writer = new StringWriter();
            cause.printStackTrace(new PrintWriter(writer));
            opResult.exceptionsStackTrace = writer.toString();
        }

        if (result.getParams() != null) {
            for (Map.Entry<String, Serializable> entry : result.getParams().entrySet()) {
                String paramValue = null;
                Object value = entry.getValue();
                if (value != null) {
                    paramValue = value.toString();
                }

                opResult.getParams().add(new Param(entry.getKey(), paramValue));
            }
        }
        
        if(result.getContext() != null){
        	for (Map.Entry<String, Serializable> entry : result.getContext().entrySet()) {
                String contextValue = null;
                Object value = entry.getValue();
                if (value != null) {
                	contextValue = value.toString();
                }

                opResult.getContexts().add(new Context(entry.getKey(), contextValue));
            }
        }

        if (result.getSubresults() != null) {
            for (OperationResult subresult : result.getSubresults()) {
				OpResult subOpResult = OpResult.getOpResult(page, subresult);
				opResult.getSubresults().add(subOpResult);
				subOpResult.parent = opResult;
				if (subOpResult.getBackgroundTaskOid() != null) {
					opResult.backgroundTaskOid = subOpResult.getBackgroundTaskOid();
				}
            }
        }

		if (result.getBackgroundTaskOid() != null) {
			opResult.backgroundTaskOid = result.getBackgroundTaskOid();
		}

        try {
        	OperationResultType resultType = result.createOperationResultType();
        	ObjectFactory of = new ObjectFactory();
			opResult.xml = page.getPrismContext().xmlSerializer().serialize(of.createOperationResult(resultType));
		} catch (SchemaException|RuntimeException ex) {
            String m = "Can't create xml: " + ex;
//			error(m);
            opResult.xml = "<?xml version='1.0'?><message>" + StringEscapeUtils.escapeXml(m) + "</message>";
//            throw ex;
        }
        return opResult;
    }

	// This method should be called along with getOpResult for root operationResult. However, it might take some time,
	// and there might be situations in which it is not required -- so we opted for calling it explicitly.
	public void determineBackgroundTaskVisibility(PageBase pageBase) {
		if (backgroundTaskOid == null) {
			return;
		}
		try {
			if (pageBase.getSecurityEnforcer().isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)) {
				backgroundTaskVisible = true;
				return;
			}
		} catch (SchemaException e) {
			backgroundTaskVisible = false;
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine background task visibility", e);
			return;
		}

		Task task = pageBase.createSimpleTask(OPERATION_CHECK_TASK_VISIBILITY);
		try {
			pageBase.getModelService().getObject(TaskType.class, backgroundTaskOid, null, task, task.getResult());
			backgroundTaskVisible = true;
		} catch (ObjectNotFoundException|SchemaException|SecurityViolationException|CommunicationException|ConfigurationException|ExpressionEvaluationException e) {
			LOGGER.debug("Task {} is not visible by the current user: {}: {}", backgroundTaskOid, e.getClass(), e.getMessage());
			backgroundTaskVisible = false;
		}
	}

	public boolean isShowMore() {
		return showMore;
	}
    
    public void setShowMore(boolean showMore) {
		this.showMore = showMore;
	}
    
    public boolean isShowError() {
		return showError;
	}
    
    public void setShowError(boolean showError) {
		this.showError = showError;
	}
    
    public List<OpResult> getSubresults() {
        if (subresults == null) {
            subresults = new ArrayList<OpResult>();
        }
        return subresults;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getExceptionsStackTrace() {
        return exceptionsStackTrace;
    }

    public String getMessage() {
        return message;
    }

    public String getOperation() {
        return operation;
    }

    public List<Param> getParams() {
        if (params == null) {
            params = new ArrayList<Param>();
        }
        return params;
    }
    
    public List<Context> getContexts() {
        if (contexts == null) {
        	contexts = new ArrayList<Context>();
        }
        return contexts;
    }

    public OperationResultStatus getStatus() {
        return status;
    }

    public int getCount() {
        return count;
    }
    
    public String getXml() {
    	return xml;
    }

	public String getBackgroundTaskOid() {
		return backgroundTaskOid;
	}

	public boolean isBackgroundTaskVisible() {
		if (backgroundTaskVisible != null) {
			return backgroundTaskVisible;
		}
		if (parent != null) {
			return parent.isBackgroundTaskVisible();
		}
		return true;			// at least as for now
	}

	@Override
	public void accept(Visitor visitor) {
		
		visitor.visit(this);
		
		for (OpResult result : this.getSubresults()){
			result.accept(visitor);
		}
		
	}
	
	public void setShowMoreAll(final boolean show) {
		Visitor visitor = new Visitor() {

			@Override
			public void visit(Visitable visitable) {
				if (!(visitable instanceof OpResult)) {
					return;
				}

				OpResult result = (OpResult) visitable;
				result.setShowMore(show);

			}
		};

		accept(visitor);
	}
}
