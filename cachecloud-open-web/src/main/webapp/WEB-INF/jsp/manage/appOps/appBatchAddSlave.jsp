<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>

<script type="text/javascript">
	
	function addSlave(appId){
		var slaveText = $("#slaveText").val();
		$.post(
				'/manage/app/batchAddSlaveDeploy.json',
				{
					appId:appId,
					slaveText:slaveText
				},
		        function(data){
					var status = data.status;
					if (status == 1) {
						alert("slave添加成功");
					} else {
						alert("部署失败,请查看系统日志确认相关原因!");
					}
					
					window.clearInterval(window.intervalId);
					clearDeployLabel();
					var appDeployBtn = document.getElementById("appDeployBtn");
					appDeployBtn.disabled = true;
		    		
		    		var appCheckBtn = document.getElementById("appCheckBtn");
		    		appCheckBtn.disabled = false;
		    		
		    		appDeployText.disabled = false;
		        }
		     );
		//展示简单的进度条
		window.intervalId = setInterval(startShowDeployLabel,5000);		
	}
	
	function startShowDeployLabel(){
		var startDeployLabel = document.getElementById("startDeployLabel");
		startDeployLabel.innerHTML += '.';
	}
	
	function clearDeployLabel(){
		var startDeployLabel = document.getElementById("startDeployLabel");
		startDeployLabel.innerHTML = '';
	}
	function checkText(appId){
		var slaveText = $("#slaveText").val();
		$.post(
				'/manage/app/batchAddSlaveCheck.json',
				{
					appId:appId,
					slaveText:slaveText
				},
				function(data){
					var status = data.status;
					alert(data.message);
					if (status == 1) {
						var appDeployBtn = document.getElementById("appDeployBtn");
						appDeployBtn.disabled = false;
			    		
			    		var appCheckBtn = document.getElementById("appCheckBtn");
			    		appCheckBtn.disabled = true;
			    		
			    		appDeployText.disabled = true;

					} else {
						$("#slaveText").focus();
					}
				}
				);
	}
</script>
</head>
<body>
	<form action="/manage/app/batchAddSlave.do" method="post"
		class="form-horizontal form-bordered form-row-stripped">
		<div class="form-group">
			<label class="control-label col-md-3"> slave详情:<font
				color='red'>(*)</font>:
			</label>
			<div class="col-md-5">
				<textarea rows="10" name="slaveText" id="slaveText"
					placeholder="masterip:port:slaveip" class="form-control"></textarea>
				<span class="help-block"> 具体格式下:<br />
					&nbsp;&nbsp;&nbsp;&nbsp;masterIp1:port:slaveIp1(例如：192.168.1.1:16380:192.168.1.2)<br />
					&nbsp;&nbsp;&nbsp;&nbsp;masterIp2:port:slaveIp2<br />
					&nbsp;&nbsp;&nbsp;&nbsp;masterIp3:port:slaveIp2
				</span>
			</div>
		</div>
		<div class="form-actions fluid">
			<div class="row">
				<div class="col-md-12">
					<div class="col-md-offset-3 col-md-9">
						<button id="appDeployBtn" type="button" class="btn green"
							disabled="disabled" onclick="addSlave('${appId}')">
							<i class="fa fa-check"></i> 开始部署
						</button>
						<button id="appCheckBtn" type="button" class="btn green"
							onclick="checkText('${appId}')">
							<i class="fa fa-check"></i> 格式检查
						</button>
						<label id="startDeployLabel">
						</label>
					</div>
				</div>
			</div>
		</div>
	</form>
</body>
</html>