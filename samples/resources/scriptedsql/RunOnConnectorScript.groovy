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