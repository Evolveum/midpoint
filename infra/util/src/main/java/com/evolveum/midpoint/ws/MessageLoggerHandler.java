/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.ws;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.logging.TraceManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;

/**
 * SOAPHandler to log outgoing and ingoing request content.
 *
 * A HandlerChain végére érdemes rakni.
 * 
 * @author elek
 */
public class MessageLoggerHandler implements SOAPHandler<SOAPMessageContext> {

    private boolean writeToFile = true;

    Logger logger = TraceManager.getTrace(MessageLoggerHandler.class);

    /**
     * Headers to handle.
     *
     * @return
     */
    @Override
    public Set<QName> getHeaders() {
        return new HashSet();
    }

    /**
     * Print out the SOAP MESSAGES.
     * @param messageContext
     * @return
     */
    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        SOAPMessage msg = messageContext.getMessage();
        try {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            msg.writeTo(bas);
            logger.info(bas.toString());
            if (writeToFile){
                Date d = new Date();
                File outDir = new File(System.getProperties().getProperty("java.io.tmpdir"),"openidm.log");
                if (!outDir.exists()){
                    outDir.mkdirs();
                }
                File outFile = new File(outDir,""+d.getTime()+".xml");
                FileOutputStream out = new FileOutputStream(outFile);
                out.write(bas.toByteArray());
                out.close();
            }
        } catch (Exception ex) {
            throw new MidPointException("Error on logging WS message", ex);
        }
        return true;
    }

    /**
     * Handle fault.
     *
     * @param context
     * @return
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    /**
     * NOOP deconstructor.
     * 
     * @param context
     */
    @Override
    public void close(MessageContext context) {
    }
}
