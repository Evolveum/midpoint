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
// The connector sends the following:
// connection: handler to the SQL connection
// action: a string describing the action ("DELETE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// options: a handler to the OperationOptions Map
// uid: String for the unique id that specifies the object to delete

log.info("Entering "+action+" Script");
def sql = new Sql(connection);

assert uid != null

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.execute("DELETE FROM Users where uid= ?",[uid])
    break

    case "__GROUP__":
    sql.execute("DELETE FROM Groups where name= ?",[uid])
    break

    case "organization":
    sql.execute("DELETE FROM Organizations where name= ?",[uid])
    break

    default:
    uid;
}