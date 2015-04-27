/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.ninja.command;

import com.beust.jcommander.Parameter;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class Delete extends Command {

    public static final String CMD_DELETE = "delete";

    public static final String P_OID = "-o";
    public static final String P_OID_LONG = "--oid";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_FORCE = "-f";
    public static final String P_FORCE_LONG = "--force";

    @Parameter(names = {P_OID, P_OID_LONG}, required = true,
            description = "Object oid which has to be deleted")
    private String oid;

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, validateValueWith = ObjectTypeConverter.class,
            converter = ObjectTypeConverter.class,
            description = "Object oid which has to be deleted")
    private QName type;

    @Parameter(names = {P_RAW, P_RAW_LONG},
            description = "Use raw flag for this delete operation")
    private boolean raw;

    @Parameter(names = {P_FORCE, P_FORCE_LONG},
            description = "Use force flag for this delete operation")
    private boolean force;

    public boolean isForce() {
        return force;
    }

    public boolean isRaw() {
        return raw;
    }

    public QName getType() {
        return type;
    }

    public String getOid() {
        return oid;
    }
}
