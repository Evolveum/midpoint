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

package com.evolveum.midpoint.util.logging;

import org.slf4j.Logger;

/**
 *
 *
The six logging levels used by Log are (in order):
 *
 * 1. trace (the least serious)
 * 2. debug
 * 3. info
 * 4. warn
 * 5. error
 * 6. fatal (the most serious)
 *
 * The mapping of these log levels to the concepts used by the underlying
 * logging system is implementation dependent.
 * The implemention should ensure, though, that this ordering behaves
 * as expected.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface Trace extends Logger {

    public static final String code_id = "$Id$";

    /**
    public boolean isTraceEnabled(String method);

    public boolean isDebugEnabled(String method);

    public boolean isInfoEnabled(String method);

    public boolean isWarnEnabled(String method);

    public boolean isErrorEnabled(String method);

    public boolean isFatalEnabled(String method);

    // Template start
    public void entryWarn(String method);

    public void entryWarn(String method, Object[] params);

    public void entryMsgWarn(String method, String msg);

    public void entryMsgWarn(String method, String msg, Object[] params);

    public void exitWarn(String method);

    public void exitWarn(String method, Object[] params);

    public void exitMsgWarn(String method, String msg);

    public void exitMsgWarn(String method, String msg, Object[] params);

    public void infoWarn(String method, String msg);

    public void infoWarn(String method, Object[] params);

    public void infoMsgWarn(String method, String msg, Object[] params);

    public void caughtWarn(String method, Throwable t);

    public void caughtWarn(String method, Throwable t, Object[] params);

    public void caughtMsgWarn(String method, String msg, Throwable t);

    public void caughtMsgWarn(String method, String msg, Throwable t, Object[] params);

    public void dataWarn(String method, byte[] data);

    public void dataMsgWarn(String method, String msg, byte[] data);

    public void listWarn(String method, List list);

    public void listMsgWarn(String method, String msg, List list);

    public void throwingWarn(String method, Throwable t);

    public void throwingWarn(String method, Throwable t, Object[] params);

    public void throwingMsgWarn(String method, String msg, Throwable t);

    public void throwingMsgWarn(String method, String msg, Throwable t, Object[] params);

    public void variableWarn(String method, String label, boolean value);

    public void variableWarn(String method, String label, int value);

    public void variableWarn(String method, String label, long value);

    public void variableWarn(String method, String label, Object value);

    // Template end
    public void entryTrace(String method);

    public void entryTrace(String method, Object[] params);

    public void entryMsgTrace(String method, String msg);

    public void entryMsgTrace(String method, String msg, Object[] params);

    public void exitTrace(String method);

    public void exitTrace(String method, Object[] params);

    public void exitMsgTrace(String method, String msg);

    public void exitMsgTrace(String method, String msg, Object[] params);

    public void infoTrace(String method, String msg);

    public void infoTrace(String method, Object[] params);

    public void infoMsgTrace(String method, String msg, Object[] params);

    public void caughtTrace(String method, Throwable t);

    public void caughtTrace(String method, Throwable t, Object[] params);

    public void caughtMsgTrace(String method, String msg, Throwable t);

    public void caughtMsgTrace(String method, String msg, Throwable t, Object[] params);

    public void dataTrace(String method, byte[] data);

    public void dataMsgTrace(String method, String msg, byte[] data);

    public void listTrace(String method, List list);

    public void listMsgTrace(String method, String msg, List list);

    public void throwingTrace(String method, Throwable t);

    public void throwingTrace(String method, Throwable t, Object[] params);

    public void throwingMsgTrace(String method, String msg, Throwable t);

    public void throwingMsgTrace(String method, String msg, Throwable t, Object[] params);

    public void variableTrace(String method, String label, boolean value);

    public void variableTrace(String method, String label, int value);

    public void variableTrace(String method, String label, long value);

    public void variableTrace(String method, String label, Object value);

    public void entryDebug(String method);

    public void entryDebug(String method, Object[] params);

    public void entryMsgDebug(String method, String msg);

    public void entryMsgDebug(String method, String msg, Object[] params);

    public void exitDebug(String method);

    public void exitDebug(String method, Object[] params);

    public void exitMsgDebug(String method, String msg);

    public void exitMsgDebug(String method, String msg, Object[] params);

    public void infoDebug(String method, String msg);

    public void infoDebug(String method, Object[] params);

    public void infoMsgDebug(String method, String msg, Object[] params);

    public void caughtDebug(String method, Throwable t);

    public void caughtDebug(String method, Throwable t, Object[] params);

    public void caughtMsgDebug(String method, String msg, Throwable t);

    public void caughtMsgDebug(String method, String msg, Throwable t, Object[] params);

    public void dataDebug(String method, byte[] data);

    public void dataMsgDebug(String method, String msg, byte[] data);

    public void listDebug(String method, List list);

    public void listMsgDebug(String method, String msg, List list);

    public void throwingDebug(String method, Throwable t);

    public void throwingDebug(String method, Throwable t, Object[] params);

    public void throwingMsgDebug(String method, String msg, Throwable t);

    public void throwingMsgDebug(String method, String msg, Throwable t, Object[] params);

    public void variableDebug(String method, String label, boolean value);

    public void variableDebug(String method, String label, int value);

    public void variableDebug(String method, String label, long value);

    public void variableDebug(String method, String label, Object value);

    public void entryInfo(String method);

    public void entryInfo(String method, Object[] params);

    public void entryMsgInfo(String method, String msg);

    public void entryMsgInfo(String method, String msg, Object[] params);

    public void exitInfo(String method);

    public void exitInfo(String method, Object[] params);

    public void exitMsgInfo(String method, String msg);

    public void exitMsgInfo(String method, String msg, Object[] params);

    public void infoInfo(String method, String msg);

    public void infoInfo(String method, Object[] params);

    public void infoMsgInfo(String method, String msg, Object[] params);

    public void caughtInfo(String method, Throwable t);

    public void caughtInfo(String method, Throwable t, Object[] params);

    public void caughtMsgInfo(String method, String msg, Throwable t);

    public void caughtMsgInfo(String method, String msg, Throwable t, Object[] params);

    public void dataInfo(String method, byte[] data);

    public void dataMsgInfo(String method, String msg, byte[] data);

    public void listInfo(String method, List list);

    public void listMsgInfo(String method, String msg, List list);

    public void throwingInfo(String method, Throwable t);

    public void throwingInfo(String method, Throwable t, Object[] params);

    public void throwingMsgInfo(String method, String msg, Throwable t);

    public void throwingMsgInfo(String method, String msg, Throwable t, Object[] params);

    public void variableInfo(String method, String label, boolean value);

    public void variableInfo(String method, String label, int value);

    public void variableInfo(String method, String label, long value);

    public void variableInfo(String method, String label, Object value);

    public void entryError(String method);

    public void entryError(String method, Object[] params);

    public void entryMsgError(String method, String msg);

    public void entryMsgError(String method, String msg, Object[] params);

    public void exitError(String method);

    public void exitError(String method, Object[] params);

    public void exitMsgError(String method, String msg);

    public void exitMsgError(String method, String msg, Object[] params);

    public void infoError(String method, String msg);

    public void infoError(String method, Object[] params);

    public void infoMsgError(String method, String msg, Object[] params);

    public void caughtError(String method, Throwable t);

    public void caughtError(String method, Throwable t, Object[] params);

    public void caughtMsgError(String method, String msg, Throwable t);

    public void caughtMsgError(String method, String msg, Throwable t, Object[] params);

    public void dataError(String method, byte[] data);

    public void dataMsgError(String method, String msg, byte[] data);

    public void listError(String method, List list);

    public void listMsgError(String method, String msg, List list);

    public void throwingError(String method, Throwable t);

    public void throwingError(String method, Throwable t, Object[] params);

    public void throwingMsgError(String method, String msg, Throwable t);

    public void throwingMsgError(String method, String msg, Throwable t, Object[] params);

    public void variableError(String method, String label, boolean value);

    public void variableError(String method, String label, int value);

    public void variableError(String method, String label, long value);

    public void variableError(String method, String label, Object value);

    public void entryFatal(String method);

    public void entryFatal(String method, Object[] params);

    public void entryMsgFatal(String method, String msg);

    public void entryMsgFatal(String method, String msg, Object[] params);

    public void exitFatal(String method);

    public void exitFatal(String method, Object[] params);

    public void exitMsgFatal(String method, String msg);

    public void exitMsgFatal(String method, String msg, Object[] params);

    public void infoFatal(String method, String msg);

    public void infoFatal(String method, Object[] params);

    public void infoMsgFatal(String method, String msg, Object[] params);

    public void caughtFatal(String method, Throwable t);

    public void caughtFatal(String method, Throwable t, Object[] params);

    public void caughtMsgFatal(String method, String msg, Throwable t);

    public void caughtMsgFatal(String method, String msg, Throwable t, Object[] params);

    public void dataFatal(String method, byte[] data);

    public void dataMsgFatal(String method, String msg, byte[] data);

    public void listFatal(String method, List list);

    public void listMsgFatal(String method, String msg, List list);

    public void throwingFatal(String method, Throwable t);

    public void throwingFatal(String method, Throwable t, Object[] params);

    public void throwingMsgFatal(String method, String msg, Throwable t);

    public void throwingMsgFatal(String method, String msg, Throwable t, Object[] params);

    public void variableFatal(String method, String label, boolean value);

    public void variableFatal(String method, String label, int value);

    public void variableFatal(String method, String label, long value);

    public void variableFatal(String method, String label, Object value);
     */
}
