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

package com.evolveum.midpoint.web.page.admin.workflow;

import org.apache.wicket.model.IModel;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 28.9.2012
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */
public class PageProcessInstancesRequestedFor extends PageProcessInstances {

    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageProcessInstancesRequestedFor.title");
    }

    public PageProcessInstancesRequestedFor() {
        super(false);
    }
}
