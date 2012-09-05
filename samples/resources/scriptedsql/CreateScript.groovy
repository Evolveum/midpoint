/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// id: The entry identifier (OpenICF "Name" atribute. (most often matches the uid)
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");

def sql = new Sql(connection);
//Create must return UID. Let's return the name for now.

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.execute("INSERT INTO Users (uid, firstname,lastname,fullname,email,organization) values (?,?,?,?,?;?)",
        [
            id,
            attributes.get("firstname").get(0),
            attributes.get("lastname").get(0),
            attributes.get("fullname").get(0),
            attributes.get("email").get(0),
            attributes.get("organization").get(0)
        ])
    break

    case "__GROUP__":
    sql.execute("INSERT INTO Groups (gid,name,description) values (?,?,?)",
        [
            attributes.get("gid").get(0),
            id,
            attributes.get("description").get(0)
        ])
    break

    case "organization":
    sql.execute("INSERT INTO Organizations (name,description) values (?,?)",
        [
            id,
            attributes.get("description").get(0)
        ])
    break

    default:
    id;
}

return id;