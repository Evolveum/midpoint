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
	
	var cssAddedValue = {
		      'background' : '#d8f4d8',
		      'border-color' : '#FFFFFF'
		    };
	
	var cssSecondaryValue = {
		      'background' : '#E0F0FF',
		      'border-color' : '#FFFFFF'
		    };
	
	var cssDeletedValue = {
		      'background' : '#FFD7CA',
		      'border-color' : '#FFFFFF'
		    };
	
	
	$(".sortedTable table tbody tr").click(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
			$(this).find("td").css("background","#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		} else {
			$(this).find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).find("td").css("background","#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
			
		}
		checkAllChecked($(this).parents(".sortedTable"));
	});
	
	$("td input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).attr("checked", false);
			$(this).parents("tr").find("td").css("background","#d8f4d8");
			$(this).parents("tr").find("td").css("border-color","#FFFFFF");
			
		} else {
			$(this).attr("checked", true);
			$(this).parents("tr").find("td").css("background","#FFFFFF");
			$(this).parents("tr").find("td").css("border-color","#F2F2F2");
		}
		checkAllChecked($(this).parents(".sortedTable"));
	});
	
//	$(document).find(".sortedTable").each(function(index){
//		var row =  $(this).find("table tbody tr");
//		if(row.find(".tableCheckbox").length > 0) {
//			row.find(".tableCheckbox").find("input[type='checkbox']:checked").parents("tr:first").find("td").css(cssSelectedRow);
//			checkAllChecked($(this));
//		}
//	});
	
	
	$(document).find(".sortedTable .secondaryValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssSecondaryValue);
	});
	
	$(document).find(".sortedTable .deletedValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssDeletedValue);
	});
	
	$(document).find(".sortedTable .addedValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssAddedValue);
	});
	
	
	$("thead input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).parents(".sortedTable").find("tbody").find("td").css("background","#d8f4d8");
			$(this).parents(".sortedTable").find("tbody").find("td").css("border-color","#FFFFFF");
		} else {		
			$(this).parents(".sortedTable").find("tbody").find("tr").each(function() {
				var deleted = false;
				$(this).find("img").each(function() {
					if($(this).attr("class") == "deletedValue") {
						deleted = true;
					}
				});
				
				$(this).find("td").each(function() {
					if($(this).attr("class") == "deletedValue") {
						deleted = true;
					}
				});
				
				if(deleted) {
					$(this).find("td").css(cssDeletedValue);
				} else {
					$(this).find("td").css("background","#FFFFFF");
					$(this).find("td").css("border-color","#F2F2F2");
				}
			});
			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
		}
	});
	
	function checkAllChecked(parent) {
		if(parent.find("tbody tr").find(".tableCheckbox").length > 0) {
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
		} else if($(this).find(".deletedValue").length > 0) {
			$(this).find("td").css("background", "#FFC2AE");
		} else if($(this).find(".secondaryValue").length > 0) {
			$(this).find("td").css("background", "#D0E0FF");
		} else if($(this).find(".addedValue").length > 0) {
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
			$(this).find("td").css("border-color","#FFFFFF");
		}
	}).mouseleave(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else if($(this).find(".deletedValue").length > 0) {
			$(this).find("td").css("background", "#FFD7CA");
		} else if($(this).find(".secondaryValue").length > 0) {
			$(this).find("td").css("background", "#E0F0FF");
		} else if($(this).find(".addedValue").length > 0) {
			$(this).find("td").css("background", "#d8f4d8");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		}
	}).find(".tableCheckbox").find("input[type='checkbox']").click(function(){
		checkAllChecked($(this).parents(".sortedTable"));
	});

}