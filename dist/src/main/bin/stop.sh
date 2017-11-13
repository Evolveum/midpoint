#!/bin/bash
echo "Stopping midPoint"
jps -v | grep midpoint.war | awk '{print $1}'  | xargs kill -9
