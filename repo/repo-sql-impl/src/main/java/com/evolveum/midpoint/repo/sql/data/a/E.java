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

package com.evolveum.midpoint.repo.sql.data.a;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/8/12
 * Time: 4:59 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class E implements Serializable {

    private String aOwner;
    private Long aId;

//    private A a;
//
//    @MapsId
//    @ManyToOne
//    @PrimaryKeyJoinColumns({
//            @PrimaryKeyJoinColumn(name = "oid", referencedColumnName = "oid"),
//            @PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
//    })
//    public A getA() {
//        return a;
//    }

    @Id
    @Column(name = "aId")
    public Long getAId() {
        return aId;
    }

    @Id
    @Column(name = "aOwner")
    public String getAOwner() {
        return aOwner;
    }

    public void setAId(Long aId) {
        this.aId = aId;
    }

    public void setAOwner(String aOwner) {
        this.aOwner = aOwner;
    }

    //    public void setA(A a) {
//        this.a = a;
//    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
