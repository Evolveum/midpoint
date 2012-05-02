/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

function initTable(){
	$("thead input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("td").css("background","#d8f4d8");
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("td").css("border-color","#FFFFFF");
		} else {
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("td").css("background","#FFFFFF");
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("td").css("border-color","#F2F2F2");
			$(this).parent().parent().parent().parent().parent().parent().parent().find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
		}
	});
	
	$(".sortedTable table tbody tr").mouseenter(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
		}
	}).mouseleave(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		}
	});

}