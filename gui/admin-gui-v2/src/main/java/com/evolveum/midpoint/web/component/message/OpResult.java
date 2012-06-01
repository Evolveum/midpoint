/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
public class OpResult implements Serializable {

    private OperationResultStatus status;
    private String operation;
    private String message;
    private List<Param> params;
    private List<Context> contexts;
    private String exceptionMessage;
    private String exceptionsStackTrace;
    private List<OpResult> subresults;
    private int count;

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
            for (Map.Entry<String, Object> entry : result.getParams().entrySet()) {
                String paramValue = null;
                Object value = entry.getValue();
                if (value != null) {
                    paramValue = value.toString();
                }

                getParams().add(new Param(entry.getKey(), paramValue));
            }
        }
        
        if(result.getContext() != null){
        	for (Map.Entry<String, Object> entry : result.getContext().entrySet()) {
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
}
