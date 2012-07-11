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
	var cssSelectedRow = {
      'background' : '#d8f4d8',
      'border-color' : '#FFFFFF'
    };
	
	$(document).find(".sortedTable").each(function(index){
		var row =  $(this).find("table tbody tr");
		if(row.find(".tableCheckbox").length > 0) {
			row.find(".tableCheckbox").find("input[type='checkbox']:checked").parents(row).find("td").css(cssSelectedRow);
			checkAllChecked($(this));
		}
	});
	
	
	
	$("thead input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).parents(".sortedTable").find("tbody").find("td").css("background","#d8f4d8");
			$(this).parents(".sortedTable").find("tbody").find("td").css("border-color","#FFFFFF");
		} else {
			$(this).parents(".sortedTable").find("tbody").find("td").css("background","#FFFFFF");
			$(this).parents(".sortedTable").find("tbody").find("td").css("border-color","#F2F2F2");
			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
		}
	});
	
	function checkAllChecked(parent) {
		if(parent.find("tbody tr").find(".tableCheckbox").length > 0) {
			alert(parent.parent().attr("id"));
			var isAllChecked = false;
			
			parent.find("tbody tr").find(".tableCheckbox").find("input[type='checkbox']").each(function(index){
				if($(this).is(":checked")){
					isAllChecked = true;
				} else {
					isAllChecked = false;
					return false;
				}
			});
			if(isAllChecked) {
				parent.find("thead").find("input[type='checkbox']").attr("checked", true);
			} else {
				parent.find("thead").find("input[type='checkbox']").attr("checked", false);
			}
		}
	}
	
	
	
	
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
	}).find(".tableCheckbox").find("input[type='checkbox']").click(function(){
		checkAllChecked($(this).parents(".sortedTable"));
	});

}