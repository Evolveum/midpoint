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
// action: String correponding to the action ("RUNSCRIPTONCONNECTOR" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// scriptArguments: a Map<String,Object> containing the arguments that are passed by the initial caller

log.info("Entering "+action+" Script");
def sql = new Sql(connection);

// Let's drop table if exists
try {
   sql.execute("DROP TABLE IF EXISTS Users");
   sql.execute("DROP TABLE IF EXISTS Groups");
} catch(Exception e){}

def mySQLCreateUsers = "CREATE TABLE Users ("+
"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
"uid char(32) NOT NULL,"+
"firstname varchar(32) NOT NULL default '',"+
"lastname varchar(32) NOT NULL default '',"+
"fullname varchar(32),"+
"email varchar(32),"+
"timestamp TIMESTAMP(8))";

def mySQLCreateGroups = "CREATE TABLE Groups ("+
"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
"gid char(32) NOT NULL,"+
"name varchar(32) NOT NULL default '',"+
"description varchar(32),"+
"timestamp TIMESTAMP(8))";

// create tables
sql.execute(mySQLCreateUsers)
sql.execute(mySQLCreateGroups)

// now let's populate the tables
def users = sql.dataSet("Users")
users.add( uid:"bob", firstname:"Bob", lastname:"Fleming", email:"Bob.Fleming@fast.com")
users.add( uid:"rowley", firstname:"Rowley", lastname:"Birkin", email:"Rowley.Birkin@fast.com")
users.add( uid:"louis", firstname:"Louis", lastname:"Balfour", email:"Louis.Balfour@fast.com")

def groups = sql.dataSet("Groups")
groups.add(gid:"100", name:"admin",description:"Admin group")
groups.add(gid:"101", name:"users",description:"Users group")

// do a query to check it all worked ok
def results = sql.firstRow("select firstname, lastname from Users where id=1").firstname
def expected = "Bob"
assert results == expected