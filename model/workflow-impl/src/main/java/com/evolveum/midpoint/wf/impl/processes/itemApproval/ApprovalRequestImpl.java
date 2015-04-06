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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.impl.util.SerializationSafeContainer;
import com.evolveum.midpoint.wf.impl.util.SingleItemSerializationSafeContainerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.List;

public class ApprovalRequestImpl<I extends Serializable> implements ApprovalRequest<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalRequestImpl.class);

    private static final long serialVersionUID = 5111362449970050179L;

    SerializationSafeContainer<Serializable> itemToApprove;

    // used for value serialization/deserialization of SerializationSafeContainer'ed items
    // set by using SpringApplicationContextHolder when unknown
    private transient PrismContext prismContext;

    private ApprovalSchema approvalSchema;

    private ApprovalRequestImpl(I itemToApprove, PrismContext prismContext) {
        setPrismContext(prismContext);
        setItemToApprove(itemToApprove);
    }

    private ApprovalRequestImpl(SerializationSafeContainer wrappedValue, PrismContext prismContext) {
        setPrismContext(prismContext);
        setItemToApprove(wrappedValue);
    }

    public ApprovalRequestImpl(SerializationSafeContainer itemToApproveWrapped, PcpAspectConfigurationType config, PrismContext prismContext) {
        this(itemToApproveWrapped, prismContext);
        setSchemaFromConfig(config, prismContext);
    }

    public ApprovalRequestImpl(I itemToApprove, PcpAspectConfigurationType config, PrismContext prismContext) {
        this(itemToApprove, prismContext);
        setSchemaFromConfig(config, prismContext);
    }

    protected void setSchemaFromConfig(PcpAspectConfigurationType config, PrismContext prismContext) {
        if (config != null) {
            setApprovalSchema(new ApprovalSchemaImpl(config.getApprovalSchema(), config.getApproverRef(),
                    config.getApproverExpression(), config.getAutomaticallyApproved(), prismContext));
        }
    }

    public ApprovalRequestImpl(SerializationSafeContainer itemToApproveWrapped, PcpAspectConfigurationType config, ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved, PrismContext prismContext) {
        this(itemToApproveWrapped, prismContext);
        setSchemaFromConfigAndParameters(config, approvalSchema, approverRef, approverExpression, automaticallyApproved, prismContext);
    }

    public ApprovalRequestImpl(I itemToApprove, PcpAspectConfigurationType config, ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved, PrismContext prismContext) {
        this(itemToApprove, prismContext);
        setSchemaFromConfigAndParameters(config, approvalSchema, approverRef, approverExpression, automaticallyApproved, prismContext);
    }

    protected void setSchemaFromConfigAndParameters(PcpAspectConfigurationType config, ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved, PrismContext prismContext) {
        if (config != null &&
                (!config.getApproverRef().isEmpty() ||
                config.getApprovalSchema() != null ||
                !config.getApproverExpression().isEmpty() ||
                config.getAutomaticallyApproved() != null)) {
            setApprovalSchema(new ApprovalSchemaImpl(config.getApprovalSchema(), config.getApproverRef(),
                    config.getApproverExpression(), config.getAutomaticallyApproved(), prismContext));
        } else {
            setApprovalSchema(new ApprovalSchemaImpl(approvalSchema, approverRef,
                    approverExpression, automaticallyApproved, prismContext));
        }
    }

    @Override
    public ApprovalSchema getApprovalSchema() {
        return approvalSchema;
    }

    public void setApprovalSchema(ApprovalSchemaImpl approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    public void setItemToApprove(I itemToApprove) {
        this.itemToApprove = new SingleItemSerializationSafeContainerImpl<Serializable>(itemToApprove, prismContext);
    }

    public void setItemToApprove(SerializationSafeContainer wrappedValue) {
        this.itemToApprove = wrappedValue;
    }

    @Override
    public I getItemToApprove() {
        if (prismContext == null) {     // quite a hack, but...
            setPrismContext(SpringApplicationContextHolder.getPrismContext());
        }
        return (I) itemToApprove.getValue();
    }

    @Override
    public String toString() {
        return "ApprovalRequest: [itemToApprove=" + itemToApprove + ", approvalSchema=" + approvalSchema + "]";
    }

    @Override
    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
        if (itemToApprove != null) {
            itemToApprove.setPrismContext(prismContext);
        }
        if (approvalSchema != null) {
            approvalSchema.setPrismContext(prismContext);
        }
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }
}
