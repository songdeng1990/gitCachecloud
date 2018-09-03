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
	function setPwd(appId) {
		var pwd = $("#pwd").val();
		if (confirm("确认要设置密码为 :" + pwd)) {
			$.ajax({
				type : "get",
				url : "/manage/app/setPwd.json",
				data : {
					pwd : pwd,
					appId : appId
				},
				success : function(result) {
					alert(result.msg);
				}
			});
		}
	}
</script>
</head>
<body>
	<div class="container" align="center" style="padding-top: 50px">	
			<form role="form" >
				<div class="form-group">
					<label for="pwd">密码</label> <input id="pwd" name="pwd" value=""><br/>
					<span>这里的密码设置只是保存密码到数据库中，不会对redis执行设置密码的操作</span>
				</div>
				<button type="button" class="btn btn-small btn-primary" data-toggle="modal" onclick="setPwd('${appId}')">提交</button>
			</form>
	</div>
</body>
</html>