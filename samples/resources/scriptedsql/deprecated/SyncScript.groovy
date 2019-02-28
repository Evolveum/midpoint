/*
 * Copyright (c) 2010-2013 Evolveum
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
    // XXX the following line is probably fine for MySQL
    // row = sql.firstRow("select timestamp from Users order by timestamp desc")

    // the following line is for PostgreSQL with TIMESTAMP columns. We will
    // "truncate" the timestamp to milliseconds
    row = sql.firstRow("select date_trunc('milliseconds', timestamp) as timestamp from users order by timestamp desc;")

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

    // XXX the following line is probably fine for MySQL
    // sql.eachRow("select * from Users where timestamp > ${tstamp}",
    //    {result.add([operation:"CREATE_OR_UPDATE", uid:Integer.toString(it.id), token:it.timestamp.getTime(), attributes:[__NAME__:it.login, firstname:it.firstname, lastname:it.lastname, fullname:it.fullname, organization:it.organization, email:it.email, __ENABLE__:!(it.disabled as Boolean)]])}

    // the following line (the select statement) is for PostgreSQL with
    // timestamp in microseconds - we truncate the timestamp to milliseconds
    sql.eachRow("select id,login,firstname,lastname,fullname,email,organization,disabled,date_trunc('milliseconds', timestamp) as timestamp from Users where date_trunc('milliseconds',timestamp) > ${tstamp}",
        {result.add([operation:"CREATE_OR_UPDATE", uid:Integer.toString(it.id), token:it.timestamp.getTime(), attributes:[__NAME__:it.login, firstname:it.firstname, lastname:it.lastname, fullname:it.fullname, organization:it.organization, email:it.email, __ENABLE__:!(it.disabled as Boolean)]])}
    )
    log.ok("Sync script: found "+result.size()+" events to sync")
    return result;
    }
else { // action not implemented
    log.error("Sync script: action '"+action+"' is not implemented in this script")
    return null;
}
