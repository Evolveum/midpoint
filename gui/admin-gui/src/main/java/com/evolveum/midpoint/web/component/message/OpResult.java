/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;

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
public class OpResult extends PageAdmin implements Serializable {

    private OperationResultStatus status;
    private String operation;
    private String message;
    private List<Param> params;
    private List<Context> contexts;
    private String exceptionMessage;
    private String exceptionsStackTrace;
    private List<OpResult> subresults;
    private int count;
    private String xml;

    public OpResult(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");
        
        this.message = result.getMessage();
        this.operation = result.getOperation();
        this.status = result.getStatus();
        this.count = result.getCount();

        if (result.getCause() != null) {
            Throwable cause = result.getCause();
            this.exceptionMessage = cause.getMessage();

            Writer writer = new StringWriter();
            cause.printStackTrace(new PrintWriter(writer));
            this.exceptionsStackTrace = writer.toString();
        }

        if (result.getParams() != null) {
            for (Map.Entry<String, Serializable> entry : result.getParams().entrySet()) {
                String paramValue = null;
                Object value = entry.getValue();
                if (value != null) {
                    paramValue = value.toString();
                }

                getParams().add(new Param(entry.getKey(), paramValue));
            }
        }
        
        if(result.getContext() != null){
        	for (Map.Entry<String, Serializable> entry : result.getContext().entrySet()) {
                String contextValue = null;
                Object value = entry.getValue();
                if (value != null) {
                	contextValue = value.toString();
                }

                getContexts().add(new Context(entry.getKey(), contextValue));
            }
        }

        if (result.getSubresults() != null) {
            for (OperationResult subresult : result.getSubresults()) {
                getSubresults().add(new OpResult(subresult));
            }
        }

        try {
        	OperationResultType resultType = result.createOperationResultType();
        	ObjectFactory of = new ObjectFactory();
			xml = getPrismContext().serializeAtomicValue(of.createOperationResult(resultType), PrismContext.LANG_XML);
		} catch (SchemaException|RuntimeException ex) {
            String m = "Can't create xml: " + ex;
			error(m);
            xml = "<?xml version='1.0'?><message>" + StringEscapeUtils.escapeXml(m) + "</message>";
        }
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
}
