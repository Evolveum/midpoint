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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.ninja;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lazyman
 */
public class RepoValidator {

    public static final String[] CONTEXTS = {"classpath:ctx-ninja.xml", "classpath:ctx-common.xml",
            "classpath:ctx-configuration.xml", "classpath*:ctx-repository.xml", "classpath:ctx-repo-cache.xml"};

    public boolean execute() {
        System.out.println("Starting validation.");

        boolean valid = true;

        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext(CONTEXTS);
        } catch (Exception ex) {
            System.out.println("Exception occurred during context loading, reason: " + ex.getMessage());
            ex.printStackTrace();

            valid = false;
        } finally {
            if (context != null) {
                try {
                    context.destroy();
                } catch (Exception ex) {
                    System.out.println("Exception occurred during context shutdown, reason: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }

        String success = valid ? "SUCCESS" : "FAIL";
        System.out.println("Finished validation. " + success);
        return valid;
    }
}
