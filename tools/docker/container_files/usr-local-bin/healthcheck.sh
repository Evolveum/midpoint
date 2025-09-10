#!/bin/bash

(set -o pipefail ; curl -k -f http://localhost:8080/midpoint/actuator/health | tr -d '[:space:]' | grep -q "\"status\":\"UP\"") || exit 1
