$(document).ready(function() {
		$("div.groupSelect").hide();
		
		$("table.groupSelect tr td").click(function() {
			text = $(this).text();
			$("input#extraDesc").val(text);
			$("div.groupSelect").hide();
		});

		$("input#extraDesc").attr("autocomplete","off");
		$("input#extraDesc").click(function() {
			offset = $("input#extraDesc").offset();
			height = $("input#extraDesc").height();
			width = $("input#extraDesc").width();
			padding = $("input#extraDesc").css("padding");
			$("div.groupSelect").css({
				"position" : "fixed",
				"left" : offset.left,
				"top" : offset.top + height + padding,
				"width" : width,
				"border" : "1px solid",
				"background-color" : "white",
				"z-index" : 2
			});
			display=$("div.groupSelect").css("display");
			  if (display == "none"){
				  $("div.groupSelect").show();
			  }
			  else{
				  $("div.groupSelect").hide();
			  }
		});
		
		$("input#extraDesc").keyup(function(){
			input=$(this).val();
			forSearch=new RegExp(input,"i");
			
			$("table.groupSelect tr td").each(function(){
				 optionText=$(this).html();
				 if (optionText.search(forSearch) != -1){
					 $(this).show();
				 }else{
					 $(this).hide();
				 }					 
			});
		});
		
	});



$(document).ready(function() {
    $("div.machineGroup").hide();

    $("table.machineGroup tr td").click(function() {
        text = $(this).text();
        $("input#groupName").val(text);
        $("div.machineGroup").hide();
    });

    $("input#groupName").attr("autocomplete","off");
    $("input#groupName").click(function() {
        offset = $("input#groupName").offset();
        height = $("input#groupName").height();
        width = $("input#groupName").width();
        padding = $("input#groupName").css("padding");
        $("div.machineGroup").css({
            "position" : "fixed",
            "left" : offset.left,
            "top" : offset.top + height + padding,
            "width" : width,
            "border" : "1px solid",
            "background-color" : "white",
            "z-index" : 2
        });
        display=$("div.machineGroup").css("display");
        if (display == "none"){
            $("div.machineGroup").show();
        }
        else{
            $("div.machineGroup").hide();
        }
    });

    $("input#groupName").keyup(function(){
        input=$(this).val();
        forSearch=new RegExp(input,"i");

        $("table.machineGroup tr td").each(function(){
            optionText=$(this).html();
            if (optionText.search(forSearch) != -1){
                $(this).show();
            }else{
                $(this).hide();
            }
        });
    });

});