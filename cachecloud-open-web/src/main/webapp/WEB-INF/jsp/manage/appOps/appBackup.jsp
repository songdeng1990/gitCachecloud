<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>
<head>
<link rel="stylesheet"
	href="http://cdn.static.runoob.com/libs/bootstrap/3.3.7/css/bootstrap.min.css">
<script
	src="http://cdn.static.runoob.com/libs/jquery/2.1.1/jquery.min.js"></script>
<script
	src="http://cdn.static.runoob.com/libs/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<script type="text/javascript">
    function restoreBackup(appId) {
    	var date = $("#backupSelect").find("option:selected").text();
    	if(confirm("确认要恢复备份数据到时间="+date)){
            $.ajax({
                type: "get",
                url: "/manage/app/restoreBackup.json",
                data: {date: date,appId:appId},
                success: function (result) {
                    alert(result.msg);
                    //window.location.reload();
                }
            });
        }
    }
</script>
</head>
<body>
	<form role="form">
		<div class="form-group">
			<label for="name">备份选择</label> 
			<select id="backupSelect" class="form-control">
				<c:forEach var="backup" items="${backupList}" varStatus="status">
					<option>${backup}</option>
				</c:forEach>
			</select>
		</div>
		<button type="button" class="btn btn-small btn-primary" data-toggle="modal" onclick="restoreBackup('${appId}')">恢复</button>
	</form>		 
</body>
</html>