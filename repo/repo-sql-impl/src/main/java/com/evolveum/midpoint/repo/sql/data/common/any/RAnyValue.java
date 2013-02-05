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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author lazyman
 */
public interface RAnyValue<T> extends Serializable {

    void setAnyContainer(RAnyContainer anyContainer);

    QName getName();

    QName getType();

    RValueType getValueType();

    boolean isDynamic();

    T getValue();

    void setName(QName name);

    void setType(QName type);

    void setValueType(RValueType valueType);

    void setDynamic(boolean dynamic);
}
