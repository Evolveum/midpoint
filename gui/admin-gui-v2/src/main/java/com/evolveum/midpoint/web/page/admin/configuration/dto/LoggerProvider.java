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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Iterator;

/**
 * @author lazyman
 */
public class LoggerProvider extends SortableDataProvider<LoggerConfiguration> {

    private IModel<LoggingDto> logging;

    public LoggerProvider(IModel<LoggingDto> logging) {
        Validate.notNull(logging, "Logging dto model must not be null.");
        this.logging = logging;
    }

    @Override
    public Iterator<? extends LoggerConfiguration> iterator(int first, int count) {
        LoggingDto dto = logging.getObject();
        return dto.getLoggers().iterator();
    }

    @Override
    public int size() {
        LoggingDto dto = logging.getObject();
        return dto.getLoggers().size();
    }

    @Override
    public IModel<LoggerConfiguration> model(LoggerConfiguration object) {
        return new Model<LoggerConfiguration>(object);
    }
}
