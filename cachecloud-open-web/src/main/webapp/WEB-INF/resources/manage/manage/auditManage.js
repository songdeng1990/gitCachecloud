//驳回应用请求
function appRefuse(appAuditId, type){
	//驳回
	var status = -1;
	//驳回理由
	var refuseReason = document.getElementById("refuseReason" + appAuditId);
	if(refuseReason.value == ""){
		alert("驳回理由不能为空");
		refuseReason.focus();
		return false;
	}
	
	var appRefuseBtn = document.getElementById("appRefuseBtn" + appAuditId);
	appRefuseBtn.disabled = true;
	
	var url = "";
	if(type == 0 || type == 1 || type == 2){
		url = "/manage/app/addAuditStatus.do";
	//用户申请
	}else if(type == 3){
		url = "/manage/user/addAuditStatus.do";
	}
	$.post(
		url,
		{
			appAuditId: appAuditId,
			refuseReason: refuseReason.value,
			status: status
		},
        function(data){
            if(data==1){
            	$("#appRefuseInfo"+appAuditId).html("<div class='alert alert-error' ><button class='close' data-dismiss='alert'>×</button><strong>Success!</strong>更新成功，窗口会自动关闭</div>");
                $('appRefuseModal'+appAuditId).modal('hide');
            	setTimeout("window.location.reload()",1000);
            }else{
            	appRefuseBtn.disabled = false;
                $("#appRefuseInfo"+appAuditId).html("<div class='alert alert-error' ><button class='close' data-dismiss='alert'>×</button><strong>Error!</strong>更新失败！</div>");
            }
        }
     );
}

//检查配置项
function checkAppConfig(){
	//配置项
	var appConfigKey = document.getElementById("appConfigKey");
	if(appConfigKey.value == ""){
		alert("配置项不能为空");
		appConfigKey.focus();
		return false;
	}
	
	//配置值
	var appConfigValue = document.getElementById("appConfigValue");
	if(appConfigValue.value == ""){
		alert("配置值不能为空");
		appConfigValue.focus();
		return false;
	}
	return true;
}

//检查配置项
function checkInstanceConfig(){
	//配置项
	var instanceConfigKey = document.getElementById("instanceConfigKey");
	if(instanceConfigKey.value == ""){
		alert("配置项不能为空");
		instanceConfigKey.focus();
		return false;
	}
	
	//配置值
	var instanceConfigValue = document.getElementById("instanceConfigValue");
	if(instanceConfigValue.value == ""){
		alert("配置值不能为空");
		instanceConfigValue.focus();
		return false;
	}
	return true;
}


//检查扩容配置
function checkAppScaleText(){
	var appScaleText = document.getElementById("appScaleText");
	if(appScaleText.value == ""){
		alert("配置不能为空");
		appScaleText.focus();
		return false;
	}
	return true;
}

function startShowDeployLabel(){
	var startDeployLabel = document.getElementById("startDeployLabel");
	startDeployLabel.innerHTML += '.';
}

function checkHorizontalScaleText(textId,checkBtnId,subBtnId,textForSubmitId){
	var checkTextArea = document.getElementById(textId);
	if(checkTextArea.value == ""){
		alert("提交信息不能为空");
		checkTextArea.focus();
		return false;
	}
	var appAuditId = document.getElementById("appAuditId");	
	$.get(
		'/manage/app/horizontalScaleCheck.json',
		{
			appAuditId: appAuditId.value,
			appDeployText: checkTextArea.value	
		},
        function(data){
			var status = data.status;
			alert(data.message);
			if (status == 1) {
				var appDeployBtn = document.getElementById(subBtnId);
				appDeployBtn.disabled = false;
	    		
	    		var checkBtn = document.getElementById(checkBtnId);
	    		checkBtn.disabled = true;    		
	    		
	    		checkTextArea.disabled = true;
	    		var subText = document.getElementById(textForSubmitId);
	    		subText.value = document.getElementById(textId).value;
	    		var deployConfigId = document.getElementById("deployConfigId");
	    		deployConfigId.value = new Date().getTime();
			} else {
				appDeployText.focus();
			}
        }
     );
}

//检查应用部署配置
function checkAppDeployText(){
	var appDeployText = document.getElementById("appDeployText");
	if(appDeployText.value == ""){
		alert("应用部署信息不能为空");
		appDeployText.focus();
		return false;
	}
	var appAuditId = document.getElementById("appAuditId");
	var appId = document.getElementById("appId");
	var redisType = document.getElementById("redisType");
	var templateId = document.getElementById("templateId");
	$.post(
		'/manage/app/appDeployCheck.json',
		{
			appAuditId: appAuditId.value,
			appDeployText: appDeployText.value,
			appId: appId.value,
			redisType: redisType.value,
			templateId: templateId.value
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

				redisType.disabled = true;

				templateId.disabled = true;
			} else {
				appDeployText.focus();
			}
        }
     );
}

function addAppDeployText() {
	var appDeployBtn = document.getElementById("appDeployBtn");
	appDeployBtn.disabled = true;
	
	var appDeployText = document.getElementById("appDeployText");
	var appAuditId = document.getElementById("appAuditId");
	
	var startDeployLabel = document.getElementById("startDeployLabel");
	startDeployLabel.innerHTML = '正在部署,请等待.';
	
	$.post(
		'/manage/app/addAppDeploy.json',
		{
			appAuditId: appAuditId.value,
			appDeployText: appDeployText.value
		},
        function(data){
			var status = data.status;
			if (status == 1) {
				alert("应用部署成功,确认后将跳转到审核界面,点击[通过]按钮即可!");
			} else {
				alert("应用部署失败,请查看系统日志确认相关原因!");
			}
			window.location.href="/manage/app/auditList";
        }
     );
	//展示简单的进度条
	setInterval(startShowDeployLabel,500);
}

function addAppShardingText() {
	var addNewNodeBtn = document.getElementById("addNewNodeBtn");
	addNewNodeBtn.disabled = true;
	
	var addNewNodeInputText = document.getElementById("addNewNodeInputText");
	var appAuditId = document.getElementById("appAuditId");
	
	var deployConfigId = document.getElementById("deployConfigId");
	
	var startDeployLabel = document.getElementById("startDeployLabel");
	startDeployLabel.innerHTML = '正在部署,请等待.';
	
	$.post(
		'/manage/app/addAppClusterSharding.json',
		{
			appAuditId: appAuditId.value,
			addNewNodeInputText: addNewNodeInputText.value,
			deployConfigId: deployConfigId.value
		},
        function(data){
			var status = data.status;
			clearInterval(processbar);
			startDeployLabel.innerHTML = '部署停止';
			if (status == "0"){
				alert(data.msg);
			}else if (status == "1"){
				alert(data.msg);
				window.location.href="/manage/app/handleHorizontalScale?appAuditId=" + data.auditId;
			}
        }
     );
	
	//展示简单的进度条
	processbar=setInterval(startShowDeployLabel,500);
}

//添加分片验证
function checkAddShardParam(){
	var masterSizeSlave = document.getElementById("masterSizeSlave");
	if(masterSizeSlave.value == ""){
		alert("主从分片配置不能为空");
		masterSizeSlave.focus();
		return false;
	}
	
	return true;
}

function checkOnlineScaleParam(inputTextId,subTextId){
	var onlineText = document.getElementById(inputTextId);
	if (checkHorizontalScaleParam(onlineText.value)){
		var subText = document.getElementById(subTextId);
		subText.value = onlineText.value
		return true;
	}
	
	alert("输入参数非法，请检查");
	return false;
}


//添加水平扩容验证
function checkHorizontalScaleParam(ipPortText){
		
	try	{
		var addrArray = ipPortText.split('\n');
		for (var i=0;i<addrArray.length;i++){
			ipPort = addrArray[i];
			var ipPortPair = ipPort.split(':');
			if (ipPortPair.length != 2){
				return false;
			}
			else{
				var ip = ipPortPair[0];
				var port = ipPortPair[1];			
				if (!checkIp(ip) || !checkPort(port))
				{
					return false;
				}
			}		
		}
	}catch(e){
		alert(e.message);
		return false;
	}
	
	return true;
}

function checkNodeMigrateParam(inputTextId,subTextId){
	var onlineText = document.getElementById(inputTextId);
	if (checkMigrateParamText(onlineText.value)){
		var subText = document.getElementById(subTextId);
		subText.value = onlineText.value
		return true;
	}
	
	alert("输入参数非法，请检查");
	return false;
}

function checkMigrateParamText(migrateText){
	try	{
		var addrArray = migrateText.split('\n');
		for (var i=0;i<addrArray.length;i++){
			var migrateItemStr = addrArray[i];
			var migrateItem = migrateItemStr.split(',');
			if (migrateItem.length != 3){
				return false;
			}
			else{
				var src = migrateItem[0];
				var dst = migrateItem[1];
				var threadNum = migrateItem[2];
				
				if (!checkIpAndPort(src) || !checkIpAndPort(dst) || !checkNum(threadNum)){
					return false;
				}
			}		
		}
	}catch(e){
		alert(e.message);
		return false;
	}
	
	return true;
}

function checkIpAndPort(ipAndPort){
	
	var ipPortPair = ipAndPort.split(':');
	
	if (ipPortPair.length != 2){
		return false;
	}	
	
	var ip = ipPortPair[0];
	var port = ipPortPair[1];		
	if (!checkIp(ip) || !checkPort(port)) 
	{
		return false;
	}
	
	return true;
}

function checkIp(ip){	  
	var re=new RegExp('^(\\d+)\.(\\d+)\.(\\d+)\.(\\d+)$');
	 if(re.test(ip)){
	      if( RegExp.$1<256 && RegExp.$2<256 && RegExp.$3<256 && RegExp.$4<256) 
	      return true; 
	 }
	 return false;
}

function checkPort(port){
	var re=new RegExp('^(\\d+)$');
	if(re.test(port)){			
		if (port<65535 && port>0)	{
			return true;
		}
	}
	return false;
}

function checkNum(num){
	var re=new RegExp('^(\\d+)$');
	if(re.test(num)){	
		if (num<200 && num>0)	{
			return true;
		}
	}
	return false;
}

//添加下线分片验证
function checkOffLineInstanceParam(){
	var ip = document.getElementById("dropIp");
	if(ip.value == ""){
		alert("ip不能为空");
		ip.focus();
		return false;
	}
	
	var port = document.getElementById("dropPort");
	if(port.value == ""){
		alert("port不能为空");
		port.focus();
		return false;
	}
	return true;
}




