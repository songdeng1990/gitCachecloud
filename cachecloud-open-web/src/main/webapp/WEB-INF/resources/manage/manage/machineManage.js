function removeMachine(id, ip) {
	var removeMachineBtn = document.getElementById(id);
	removeMachineBtn.disabled = true;
	$.get(
		'/manage/machine/checkMachineInstances.json',
		{
			ip: ip,
		},
        function(data){
			var machineHasInstance = data.machineHasInstance;
			var alertMsg;
			if (machineHasInstance == true) {
				alertMsg = "该机器ip=" + ip + "还有运行中的Redis节点,禁止删除！请先下线相关应用后再操作！";
                alert(alertMsg);
                removeMachineBtn.disabled = false;
                return;
			} else {
				alertMsg = "确认要删除ip=" + ip + "吗?";
			}
			if (confirm(alertMsg)) {
				location.href = "/manage/machine/delete.do?machineIp="+ip;
			} else {
				removeMachineBtn.disabled = false;
			}
        }
     );
}
/*$(document).ready(function () {
    var $pop_window=$('#123456');
    $('#getMachineGroupStat').click(function () {
        $.get('/manage/machine/machineGroupStat.json',
        function (data) {
            $pop_window.show().html(data);
        });
    })

})*/


function initMachine(id,ip,installed) {
    var initMachineBtn = $("#initMachineBtn" + id);
    var installedFlag = $("#installedFlag" + id);
    var alertMsg;
    if (installed == 1) {
        alertMsg = "该机器已经安装redis,是否继续安装";
        if (confirm(alertMsg)) {
            initMachineBtn.text("安装中。。。");
            initMachineBtn.attr("disabled", true);
            $.get(
                '/manage/machine/installRedis.json',
                {
                    ip: ip,
                },
                function (data) {
                    alert(data.rsp.message);
                    initMachineBtn.attr("disabled", false);
                    initMachineBtn.text("安装Redis");
                    if (data.rsp.code == 1) {
                        installedFlag.text("已安装");
                    }
                }
            );
        }
    }else {
        initMachineBtn.text("安装中。。。");
        initMachineBtn.attr("disabled", true);
        $.get(
            '/manage/machine/installRedis.json',
            {
                ip: ip,
            },
            function (data) {
                alert(data.rsp.message);
                initMachineBtn.attr("disabled", false);
                initMachineBtn.text("安装Redis");
                if (data.rsp.code == 1) {
                    installedFlag.text("已安装");
                }
            }
        );
    }
}

function saveOrUpdateMachine(machineId){
	var ip = document.getElementById("ip" + machineId);
	var room = document.getElementById("room" + machineId);
	var mem = document.getElementById("mem" + machineId);
	var cpu = document.getElementById("cpu" + machineId);
	var virtual = document.getElementById("virtual" + machineId);
    var realIp = document.getElementById("realIp" + machineId);
    var machineType = document.getElementById("machineType" + machineId);
    var extraDesc = document.getElementById("extraDesc" + machineId);
    var groupName = document.getElementById("machinegroupIdSingle" + machineId);
    var sshUser = document.getElementById("sshUser" + machineId);
    var sshPasswd = document.getElementById("sshPasswd" + machineId);

	if(ip.value == ""){
    	alert("IP不能为空!");
        ip.focus();
		return false;
    }
    if(room.value == ""){
        alert("机房不能为空!");
        room.focus();
        return false;
    }
    if(mem.value == ""){
        alert("内存不能为空!");
        mem.focus();
        return false;
    }
    if(cpu.value == ""){
        alert("CPU不能为空!");
        cpu.focus();
        return false;
    }
    if(virtual.value == ""){
        alert("是否虚机为空!");
        virtual.focus();
        return false;
    }
    
    if(sshUser.value == ""){
        alert("ssh用户为空!");
        sshUser.focus();
        return false;
    }
    
    if(sshPasswd.value == ""){
        alert("ssh密码为空!");
        sshPasswd.focus();
        return false;
    }
    var addMachineBtn = document.getElementById("addMachineBtn" + machineId);
    addMachineBtn.disabled = true;

	$.post(
		'/manage/machine/add.json',
		{
            ip: ip.value,
            room: room.value,
            mem: mem.value,
            cpu: cpu.value,
            virtual: virtual.value,
            realIp: realIp.value,
            id:machineId,
            machineType: machineType.value,
            extraDesc: extraDesc.value,
            group: groupName.value,
            sshUser: sshUser.value,
            sshPasswd: sshPasswd.value
		},
        function(data){
            if(data.result){
                $("#machineInfo" + machineId).html("<div class='alert alert-error' ><button class='close' data-dismiss='alert'>×</button><strong>Success!</strong>更新成功，窗口会自动关闭</div>");
                var targetId = "#addMachineModal" + machineId;
                setTimeout("$('" + targetId +"').modal('hide');window.location.reload();",1000);
            }else{
                addMachineBtn.disabled = false;
                $("#machineInfo" + machineId).html("<div class='alert alert-error' ><button class='close' data-dismiss='alert'>×</button><strong>Error!</strong>更新失败！</div>");
            }
        }
     );
}

function getMachineInfo(machineId){
    var ip = document.getElementById("ip" + machineId);
    var room = document.getElementById("room" + machineId);

    if(ip.value == ""){
        alert("IP不能为空!");
        ip.focus();
        return false;
    }

    if(room.value == ""){
        alert("机房不能为空!");
        room.focus();
        return false;
    }

    $.post(
        '/manage/machine/get.json',
        {
            ip: ip.value,
            room: room.value
        },
        function(data){
            if(data.info.code == 1) {
                document.getElementById("mem" + machineId).value = data.info.body[0]["memtotal"];
                document.getElementById("cpu" + machineId).value = data.info.body[0]["cpu"];
                document.getElementById("virtual" + machineId).value = data.info.body[0]["devtype"] ^ 1;
                document.getElementById("realIp" + machineId).value = data.info.body[0]["realIp"];
                document.getElementById("room" + machineId).value = data.info.body[0]["idcId"];
                alert("机器信息获取成功！");
            } else {
                alert("机器信息获取失败！message=" + data.info.message);
            }
        }
    );
}

$("#sample_editable_1_new").click(function () {
    $("#ip").attr("disabled", false);
});

function addBatchMachine() {
    var machineInfoText = document.getElementById("machineInfoText");
    var group = document.getElementById("machinegroupId");
    if(machineInfoText.value == ""){
        alert("机器信息不能为空");
        machineInfoText.focus();
        return false;
    }
    $.post(
        '/manage/machine/addAll.json',
        {
            machineInfoText: machineInfoText.value,
            group: group.value
        },
        function(data){
            if(!data.check) {
                alert("格式有误！");
                return false;
            }
            
            if (!data.success){
            	alert("向iportal获取机器信息失败");
            	return false;
            }
            if(data.info.code == 1) {
                $("#addBatchResult").html("<div class='alert alert-error' ><button class='close' data-dismiss='alert'>×</button><strong>Success!</strong>机器添加成功，窗口会自动关闭</div>");
                setTimeout("$('#addBatchMachineModal').modal('hide');window.location.reload();",1000);
            } else {
                alert("机器信息获取失败！message=" + data.info.message);
            }
        }
    );
}

function update_monitor() {
	if(confirm("确认要更新向所有机器上的监控脚本和信息吗？"))
	{
		$.get(
			'/manage/machine/updateMonitor.json',
			{},
			function(data){
				alert(data.rsp.message);
			}		
		);				
	}	
}

function update_machine_info(){
	if(confirm("确认要和运维平台同步所有机器上基本信息吗？"))
	{
		$.get(
			'/manage/machine/updateMachineInfo.json',
			{},
			function(data){
				alert(data.rsp.message);
			}		
		);				
	}
}