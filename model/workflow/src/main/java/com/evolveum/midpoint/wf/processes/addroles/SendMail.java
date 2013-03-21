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

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.opends.server.core.Workflow;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @author mederly
 */
public class SendMail implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(SendMail.class);

    private Expression to;
    private Expression subject;
    private Expression html;

    public void execute(DelegateExecution execution) {

//        String toVal = (String) to.getValue(execution);
//        String subjectVal = (String) subject.getValue(execution);
//        String htmlVal = (String) html.getValue(execution);
//
//        WfConfiguration configuration = getWfConfiguration();
//
//        // ugly hack - in unit tests the configuration is not available, so we have to provide defaults here (todo fix this)
//        String from = configuration.getMailServerDefaultFrom() != null ? configuration.getMailServerDefaultFrom() : "nobody@nowhere.org";
//        String host = configuration.getMailServerHost();        // null means we do not want to send mail!
//        if (host == null) {
//            LOGGER.info("Mail server is not defined, mail notification to " + toVal + " will not be sent.");
//            return;
//        }
//
//        Properties properties = System.getProperties();
//        properties.setProperty("mail.smtp.host", host);
//        Session session = Session.getDefaultInstance(properties);
//
//        try {
//            MimeMessage message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(from));
//            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toVal));
//            message.setSubject(subjectVal);
//            message.setContent(htmlVal, "text/html");
//            Transport.send(message);
//            LOGGER.info("Message sent successfully to " + toVal + ".");
//        } catch (MessagingException mex) {
//            LoggingUtils.logException(LOGGER, "Couldn't send mail message to " + toVal, mex);
//        }
    }

    public WfConfiguration getWfConfiguration() {
        WorkflowManager workflowManager = SpringApplicationContextHolder
                .getApplicationContext().
                        getBean("workflowManager", WorkflowManager.class);
        if (workflowManager == null) {
            throw new SystemException("Couldn't get workflow manager spring bean.");
        }
        return workflowManager.getWfConfiguration();
    }
}