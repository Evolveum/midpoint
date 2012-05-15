/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

ace.define('ace/theme/eclipse', ['require', 'exports', 'module' , 'ace/lib/dom'], function(require, exports, module) {
"use strict";

exports.isDark = false;
exports.cssText = ".ace-eclipse .ace_editor {\
  border: 2px solid rgb(159, 159, 159);\
}\
\
.ace-eclipse .ace_editor.ace_focus {\
  border: 2px solid #327fbd;\
}\
\
.ace-eclipse .ace_gutter {\
  background: rgb(227, 227, 227);\
  color: rgb(136, 136, 136);\
}\
\
.ace-eclipse .ace_print_margin {\
  width: 1px;\
  background: #b1b4ba;\
}\
\
.ace-eclipse .ace_fold {\
    background-color: rgb(60, 76, 114);\
}\
\
.ace-eclipse .ace_text-layer {\
  cursor: text;\
}\
\
.ace-eclipse .ace_cursor {\
  border-left: 2px solid black;\
}\
\
.ace-eclipse .ace_line .ace_storage,\
.ace-eclipse .ace_line .ace_keyword,\
.ace-eclipse .ace_line .ace_variable {\
  color: rgb(127, 0, 85);\
}\
\
.ace-eclipse .ace_line .ace_constant.ace_buildin {\
  color: rgb(88, 72, 246);\
}\
\
.ace-eclipse .ace_line .ace_constant.ace_library {\
  color: rgb(6, 150, 14);\
}\
\
.ace-eclipse .ace_line .ace_function {\
  color: rgb(60, 76, 114);\
}\
\
.ace-eclipse .ace_line .ace_string {\
  color: rgb(42, 0, 255);\
}\
\
.ace-eclipse .ace_line .ace_comment {\
  color: rgb(63, 127, 95);\
}\
\
.ace-eclipse .ace_line .ace_comment.ace_doc {\
  color: rgb(63, 95, 191);\
}\
\
.ace-eclipse .ace_line .ace_comment.ace_doc.ace_tag {\
  color: rgb(127, 159, 191);\
}\
\
.ace-eclipse .ace_line .ace_constant.ace_numeric {\
}\
\
.ace-eclipse .ace_line .ace_tag {\
  color: rgb(63, 127, 127);\
}\
\
.ace-eclipse .ace_line .ace_type {\
  color: rgb(127, 0, 127);\
}\
\
.ace-eclipse .ace_line .ace_xml_pe {\
  color: rgb(104, 104, 91);\
}\
\
.ace-eclipse .ace_marker-layer .ace_selection {\
  background: rgb(181, 213, 255);\
}\
\
.ace-eclipse .ace_marker-layer .ace_bracket {\
  margin: -1px 0 0 -1px;\
  border: 1px solid rgb(192, 192, 192);\
}\
\
.ace-eclipse .ace_line .ace_meta.ace_tag {\
  color:rgb(63, 127, 127);\
}\
\
.ace-eclipse .ace_entity.ace_other.ace_attribute-name {\
  color:rgb(127, 0, 127);\
}\
\
.ace-eclipse .ace_marker-layer .ace_active_line {\
  background: #E3E3E3;\
}";

exports.cssClass = "ace-eclipse";

var dom = require("../lib/dom");
dom.importCssString(exports.cssText, exports.cssClass);
});
