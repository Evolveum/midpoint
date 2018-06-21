#!/bin/sh
#/*
 #* Copyright (c) 2010-2017 Evolveum
 #*
 #* Licensed under the Apache License, Version 2.0 (the "License");
 #* you may not use this file except in compliance with the License.
 #* You may obtain a copy of the License at
 #*
 #*     http://www.apache.org/licenses/LICENSE-2.0
 #*
 #* Unless required by applicable law or agreed to in writing, software
 #* distributed under the License is distributed on an "AS IS" BASIS,
 #* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 #* See the License for the specific language governing permissions and
 #* limitations under the License.
 #*/

## TODO: connector does not bother with exit values...
## TODO: connector does not support update for __UID__ , will ignore it. See CmdUpdate.java method.
####mv "$__UID__" "$__NAME__"
####echo "$__NAME__"
chmod "$permissions" "$__UID__"
exit 0

