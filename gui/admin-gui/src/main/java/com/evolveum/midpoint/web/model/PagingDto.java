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

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;


/**
 *
 * @author Katuska
 */
public class PagingDto {

    private String orderBy;
    private int offset;
    private int maxSize;
    private OrderDirectionType direction;

    public PagingDto() {
    }

    public PagingDto(String orderBy, int offset, int maxSize, OrderDirectionType direction) {
        this.orderBy = orderBy;
        this.offset = offset;
        this.maxSize = maxSize;
        this.direction = direction;
    }

    
    
    public OrderDirectionType getDirection() {
        return direction;
    }

    public void setDirection(OrderDirectionType direction) {
        this.direction = direction;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }


}
