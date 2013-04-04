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

package com.evolveum.midpoint.wf;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.activiti.engine.impl.util.LogUtil;
//import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testng.annotations.BeforeClass;

public abstract class AbstractTest {
//    @BeforeClass
//    public static void routeLoggingToSlf4j() {
//        LogUtil.readJavaUtilLoggingConfigFromClasspath();
//        Logger rootLogger =
//                LogManager.getLogManager().getLogger("");
//        Handler[] handlers = rootLogger.getHandlers();
//        for (int i = 0; i < handlers.length; i++) {
//            rootLogger.removeHandler(handlers[i]);
//        }
//        SLF4JBridgeHandler.install();
//    }
}