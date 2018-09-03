<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>

<script type="text/javascript">
function showTemplate(select, node) {
	$.get(
		'/manage/redisConfig/getTemplateByArchitecture.json',
		{
			type: select.value
		},
		function(data){
			var src = "";
			for(var i in data.templateList) {
				src += "<option value='" + data.templateList[i].id + "'>" + data.templateList[i].name + "</option>";
			}
			if(data.templateList.length >= 1) {
				$("#extra_desc").html(data.templateList[0].extraDesc);
			}
			$(node).html(src);
		}
	);
}

function changeExtraDesc(select, node) {
	$.get(
			'/manage/redisConfig/getTemplateById.json',
			{
				id: select.value
			},
			function(data){
				$(node).html(data.desc);
			}
	);
}
</script>

<div class="page-container">
	<div class="page-content">
		
		<%@include file="machineReferList.jsp" %>
		
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">
					应用申请详情
				</h3>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption"><i class="fa fa-globe"></i>应用申请详情</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
						</div>
					</div>
					<div class="portlet-body">
						<table class="table table-striped table-bordered table-hover" id="tableDataList">
								<tr>
					                <td>应用id</td>
					                <td>${appDesc.appId}</td>
					                <td>应用名称</td>
					                <td>${appDesc.name}</td>
								</tr>
								<tr>
									<td>所属组别</td>
									<td>${appDesc.businessGroupName}</td>
									
					                <td>内存申请详情</td>
					                <td><font color="red">${appAudit.info}</font></td>
								</tr>
								<tr>
									<td>是否需要热备</td>
					                <td>
					                	<c:choose>
				                    		<c:when test="${appDesc.needHotBackUp == 1}">是</c:when>
				        		            <c:when test="${appDesc.needHotBackUp == 0}">否</c:when>
				                    	</c:choose>
					                </td>
					                <td>是否有后端数据源</td>
					                <td>
					                	<c:choose>
				                    		<c:when test="${appDesc.hasBackStore == 1}">有</c:when>
				        		            <c:when test="${appDesc.hasBackStore == 0}">无</c:when>
				                    	</c:choose>
					                </td>
								</tr>
								<tr>
					                <td>是否测试</td>
					                <td>
					                	<c:choose>
				                    		<c:when test="${appDesc.isTest == 1}">是</c:when>
				        		            <c:when test="${appDesc.isTest == 0}">否</c:when>
				                    	</c:choose>
					                </td>
					                <td>是否需要持久化</td>
					                <td>
					                	<c:choose>
				                    		<c:when test="${appDesc.needPersistence == 1}">是</c:when>
				        		            <c:when test="${appDesc.needPersistence == 0}">否</c:when>
				                    	</c:choose>
					                </td>
								</tr>
								<tr>
					                <td>预估QPS</td>
					                <td>${appDesc.forecaseQps}</td>
					                <td>预估条目数量</td>
					                <td>${appDesc.forecastObjNum}</td>
								</tr>
								<tr>
					                <td>客户端机房信息</td>
					                <td>${appDesc.clientMachineRoom}</td>
								</tr>
						</table>
					</div>
				</div>
			</div>
		</div>
		
		
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">
					应用部署
				</h3>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption">
							<i class="fa fa-globe"></i>
							填写应用部署信息
							&nbsp;
						</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
							<a href="javascript:;" class="remove"></a>
						</div>
					</div>
					<div class="portlet-body">
						<div class="form">
								<!-- BEGIN FORM-->
								<form action="/manage/app/addAppDeploy.do" method="post"
									class="form-horizontal form-bordered form-row-stripped">
									<div class="form-body">
										<div class="form-group">
											<label class="control-label col-md-3">
												存储类型:<font color='red'>(*)</font>:
											</label>
											<div class="col-md-5">
												<select id="redisType" onchange="showTemplate(this, '#templateId')">
													<option value="6">standalone</option>
													<option value="2">cluster</option>
													<option value="5">sentinel</option>
												</select>
											</div>
										</div>
										<div class="form-group">
											<label class="control-label col-md-3">
												配置模板:<font color='red'>(*)</font>:
											</label>
											<div class="col-md-5">
												<select id="templateId" onchange="changeExtraDesc(this, '#extra_desc')">
													<c:forEach var="template" items="${templateList}">
														<option value="${template.id}" <c:if test="${template.id == templateId}">selected</c:if>>
																${template.name}
														</option>
													</c:forEach>
												</select>
												<font color="green">额外描述：<span id="extra_desc">standalone默认配置模板</span></font>
											</div>
										</div>
										<div class="form-group">
											<label class="control-label col-md-3">
												部署详情:<font color='red'>(*)</font>:
											</label>
											<div class="col-md-5">
												<textarea rows="10" name="appDeployText" id="appDeployText" placeholder="部署详情" class="form-control"></textarea>
												<span class="help-block">
													具体规则如下:<br/>
													1. standalone类型：<br/> 
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp:memSize(M)(例如：10.10.xx.xx:2048)<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp:memSize(M):slaveIp(例如：192.168.1.1:128:192.168.1.2)<br/>
													2. sentinel类型：<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp:memSize(M):slaveIp<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;sentinelIp1<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;sentinelIp2<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;sentinelIp3<br/>
													3. cluster类型：<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp1:memSize(M):slaveIp1<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp2:memSize(M):slaveIp2<br/>
													&nbsp;&nbsp;&nbsp;&nbsp;masterIp3:memSize(M):slaveIp3<br/>
												</span>
											</div>
										</div>
										<input type="hidden" id="appId" name="appId" value="${appId}">
										<input type="hidden" id="appAuditId" name="appAuditId" value="${appAuditId}">
										
										<div class="form-actions fluid">
											<div class="row">
												<div class="col-md-12">
													<div class="col-md-offset-3 col-md-9">
														<button id="appDeployBtn" type="button" class="btn green" disabled="disabled" onclick="addAppDeployText()">
															<i class="fa fa-check"></i>
															开始部署
														</button>
														<button id="appCheckBtn" type="button" class="btn green" onclick="checkAppDeployText()">
															<i class="fa fa-check"></i>
															格式检查
														</button>
														<label id="startDeployLabel">
														</label>
													</div>
												</div>
											</div>
										</div>
									</div>
								</form>
								<!-- END FORM-->
							</div>
					</div>
				</div>
				<!-- END EXAMPLE TABLE PORTLET-->
			</div>
		</div>
	</div>
</div>

