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
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SYNC" or "GET_LATEST_SYNC_TOKEN" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map (null if action = "GET_LATEST_SYNC_TOKEN")
// token: a handler to an Object representing the sync token (null if action = "GET_LATEST_SYNC_TOKEN")
//
//
// Returns:
// if action = "GET_LATEST_SYNC_TOKEN", it must return an object representing the last known
// sync token for the corresponding ObjectClass
// 
// if action = "SYNC":
// A list of Maps . Each map describing one update:
// Map should look like the following:
//
// [
// "token": <Object> token object (could be Integer, Date, String) , [!! could be null]
// "operation":<String> ("CREATE_OR_UPDATE"|"DELETE")  will always default to CREATE_OR_DELETE ,
// "uid":<String> uid  (uid of the entry) ,
// "previousUid":<String> prevuid (This is for rename ops) ,
// "password":<String> password (optional... allows to pass clear text password if needed),
// "attributes":Map<String,List> of attributes name/values
// ]

log.info("Entering "+action+" Script");
def sql = new Sql(connection);

if (action.equalsIgnoreCase("GET_LATEST_SYNC_TOKEN")) {
    row = sql.firstRow("select timestamp from Users order by timestamp desc")
    log.ok("Get Latest Sync Token script: last token is: "+row["timestamp"])
    // We don't wanna return the java.sql.Timestamp, it is not a supported data type
    // Get the 'long' version
    return row["timestamp"].getTime();
}

else if (action.equalsIgnoreCase("SYNC")) {
    def result = []
    def tstamp = null
    if (token != null){
        tstamp = new java.sql.Timestamp(token)
    }
    else{
        def today= new Date()
        tstamp = new java.sql.Timestamp(today.time)
    }

    sql.eachRow("select * from Users where timestamp > ${tstamp}",
        {result.add([operation:"CREATE_OR_UPDATE", uid:it.uid, token:it.timestamp.getTime(), attributes:[firstname:it.firstname, lastname:it.lastname, email:it.email]])}
    )
    log.ok("Sync script: found "+result.size()+" events to sync")
    return result;
    }
else { // action not implemented
    log.error("Sync script: action '"+action+"' is not implemented in this script")
    return null;
}