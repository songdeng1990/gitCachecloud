<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>

<script type="text/javascript">
	var template_name = '${templateInfo.name}';
	var architecture = '${type}';
	var template_id = '${templateId}';
	var extra_desc = '${templateInfo.extraDesc}';

	function removeConfig(id, configKey) {
		if (confirm("确认要删除key="+configKey+"配置?")) {
			$.get(
					'/manage/redisConfig/remove.json',
					{
						id: id
					},
					function(data){
						var status = data.status;
						if (status == 1) {
							alert("删除成功!");
						} else {
							alert("删除失败, msg: " + result.message);
						}
						window.location.reload();
					}
			);

		}
	}

	function changeConfig(id, configKey) {
		var configValue = document.getElementById("configValue" + id);
		var info = document.getElementById("info" + id);
		var status = document.getElementById("status" + id);
		$.get(
				'/manage/redisConfig/update.json',
				{
					id: id,
					configKey: configKey,
					configValue: configValue.value,
					info: info.value,
					status: status.value
				},
				function(data){
					var status = data.status;
					if (status == 1) {
						alert("修改成功！");
						window.location.reload();
					} else {
						alert("修改失败！" + data.message);
					}

				}
		);
	}

	function saveRedisConfig() {
		var configKey = document.getElementById("configKey");
		if (configKey.value == ""){
			alert("请填写配置名");
			configKey.focus();
			return false;
		}
		var configValue = document.getElementById("configValue");
		var info = document.getElementById("info");
		if (info.value == "") {
			alert("请填写配置说明");
			info.focus();
			return false;
		}
		var type = document.getElementById("type");
		var templateId = document.getElementById("templateId");
		$.get(
				'/manage/redisConfig/add.json',
				{
					configKey: configKey.value,
					configValue: configValue.value,
					info: info.value,
					type: type.value,
					templateId: templateId.value
				},
				function(data){
					var status = data.status;
					if (status == 1) {
						alert("添加成功！");
					} else {
						alert("添加失败！" + data.message);
					}
					window.location.reload();
				}
		);
	}

	function saveTemplate() {
		var templateName = document.getElementById("templateName");
		if (templateName.value == ""){
			alert("请填写模板名称");
			templateName.focus();
			return false;
		}
		var redisType = document.getElementById("redisType");
		var copyTemplate = document.getElementById("copyTemplate");
		var extraDesc = document.getElementById("extraDesc");
		document.getElementById("addTemplate").disabled = true;
		$.get(
				'/manage/redisConfig/addTemplate.json',
				{
					templateName: templateName.value,
					redisType: redisType.value,
					copyTemplate: copyTemplate.value,
					extraDesc: extraDesc.value
				},
				function(data){
					if(data.status == 1) {
						alert("创建成功！");
						window.location.href = "/manage/redisConfig/init.do?templateId=" + data.templateId + "&type=" + data.type;
						//window.location = "/manage/redisConfig/init.do?templateId="+data.templateId;
					} else {
						alert("创建失败！");
					}
				}
		);
	}

	function showTemplate(select, node) {
		$.get(
				'/manage/redisConfig/getTemplateByArchitecture.json',
				{
					type: select.value
				},
				function(data){
					var src = "";
					if(node == '#copyTemplate') src += "<option value='0'>空模板</option>";
					for(var i in data.templateList) {
						src += "<option value='" + data.templateList[i].id + "'>" + data.templateList[i].name + "</option>";
					}
					$(node).html(src);
				}
		);
	}


	function redirect(select) {
		var type = $("#saveType").val();
		var templateId = select.value;
		window.location.href = "/manage/redisConfig/init.do?templateId=" + templateId + "&type=" + type;
	}

	function redirect2(select) {
		var type = select.value;
		var templateId;
		if(type == 2) templateId = 1;
		else if(type == 5) templateId = 2;
		else templateId = 3;
		window.location.href = "/manage/redisConfig/init.do?templateId=" + templateId + "&type=" + type;
	}

	function removeTemplate() {
		if (confirm("确认要删除该配置模板吗?")) {
			$.get(
					'/manage/redisConfig/removeTemplate.json',
					{
						id: template_id,
						type: architecture
					},
					function(data){
						var status = data.status;
						if (status == 1) {
							alert("删除成功!");
						} else {
							alert("删除失败!");
						}
						//删除模板后跳转至standalone默认模板
						window.location.href = "/manage/redisConfig/init.do?templateId=3&type=6";
					}
			);

		}
	}
</script>

<div class="page-container">
	<div class="page-content">
		<div class="table-toolbar">
			<div class="btn-group">
				<button id="sample_editable_1_new" class="btn green" data-target="#addRedisConfigModal" data-toggle="modal">
					添加新配置 <i class="fa fa-plus"></i>
				</button>
			</div>
			&nbsp;&nbsp;&nbsp;
			<div class="btn-group">
				<button id="create_new_template" class="btn btn-info" data-target="#createConfigTemplateModal" data-toggle="modal">
					创建新模板 <i class="fa fa-plus"></i>
				</button>
			</div>
			&nbsp;&nbsp;&nbsp;
			<div class="btn-group">
				<button id="modify_template" class="btn btn-info" data-target="#modifyConfigTemplateModal" data-toggle="modal">
					修改模板信息</i>
				</button>
			</div>
			&nbsp;&nbsp;&nbsp;
			<div class="btn-group">
				<button type="button" id="remove_template" class="btn btn-danger" onclick="removeTemplate()">
					删除模板</i>
				</button>
			</div>
			<div class="btn-group" style="float:right">
				<form action="/manage/redisConfig/init.do" method="post" class="form-horizontal form-bordered form-row-stripped">
					<label class="control-label">
						存储类型:
					</label>
					<select name="type" onchange="redirect2(this)" id="saveType">
						<option value="2" <c:if test="${type == 2}">selected</c:if>>
							Redis-cluster
						</option>
						<option value="5" <c:if test="${type == 5}">selected</c:if>>
							Redis-sentinel
						</option>
						<option value="6" <c:if test="${type == 6}">selected</c:if>>
							Redis-standalone
						</option>
					</select>
					<label class="control-label">
						模板名称:
					</label>
					<select name="templateId" id="templateList" onchange="redirect(this)">
						<c:forEach var="template" items="${templateList}">
							<option value="${template.id}" <c:if test="${template.id == templateInfo.id}">selected</c:if>>
									${template.name}
							</option>
						</c:forEach>
					</select>
					<!-- &nbsp;<button type="submit" class="btn green btn-sm">查询</button> -->
				</form>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">
					${templateInfo.name}
					<a target="_blank" href="/manage/redisConfig/preview?templateId=${templateInfo.id}" class="btn btn-info" role="button">配置模板 预览</a>

				</h3>
			</div>
		</div>
		<div class="alert alert-warning" role="alert">
			模板额外描述：${templateInfo.extraDesc}
		</div>


		<c:if test="${type != 6}">
			<div class="row">
				<div class="col-md-12">
					<div class="portlet box light-grey">
						<div class="portlet-title">
							<div class="caption">
								<i class="fa fa-globe"></i>
								<c:if test="${type == 2}">
									填写cluster配置:
									&nbsp;
								</c:if>
								<c:if test="${type == 5}">
									填写sentinel配置:
									&nbsp;
								</c:if>
							</div>
							<div class="tools">
								<a href="javascript:;" class="collapse"></a>
							</div>
						</div>


						<c:forEach items="${redisConfigList}" var="config" varStatus="stats">
							<c:if test="${config.type == 2 || config.type == 5}">
								<div class="form">
									<form class="form-horizontal form-bordered form-row-stripped">
										<div class="form-body">
											<div class="form-group">
												<label class="control-label col-md-3">
													<c:choose>
														<c:when test="${config.status == 0}">
															<font color='red'>（无效配置）</font>
														</c:when>
													</c:choose>
														${config.configKey}:
												</label>
												<div class="col-md-2">
													<input id="configValue${config.id}" type="text" name="configValue" class="form-control" value="${config.configValue}" />
												</div>
												<div class="col-md-3">
													<input id="info${config.id}" type="text" name="info" class="form-control" value="${config.info}" />
												</div>
												<div class="col-md-2">
													<select id="status${config.id}" name="status" class="form-control">
														<option value="1" <c:if test="${config.status == 1}">selected</c:if>>
															有效
														</option>
														<option value="0" <c:if test="${config.status == 0}">selected</c:if>>
															无效
														</option>
													</select>
												</div>
												<div class="col-md-2">
													<button type="button" class="btn btn-small" onclick="changeConfig('${config.id}','${config.configKey}')">
														修改
													</button>
													<button type="button" class="btn btn-small" onclick="removeConfig('${config.id}','${config.configKey}')">
														删除
													</button>
												</div>
											</div>
										</div>
										<input type="hidden" name="configKey" value="${config.configKey}">
										<input type="hidden" name="id" value="${config.id}">
									</form>
									<!-- END FORM-->
								</div>
							</c:if>
						</c:forEach>
					</div>
					<!-- END TABLE PORTLET-->
				</div>
			</div>
		</c:if>

		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption">
							<i class="fa fa-globe"></i>
							填写普通配置:
							&nbsp;
						</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
						</div>
					</div>


					<c:forEach items="${redisConfigList}" var="config" varStatus="stats">
						<c:if test="${config.type == 6}">
							<div class="form">
								<form class="form-horizontal form-bordered form-row-stripped">
									<div class="form-body">
										<div class="form-group">
											<label class="control-label col-md-3">
												<c:choose>
													<c:when test="${config.status == 0}">
														<font color='red'>（无效配置）</font>
													</c:when>
												</c:choose>
													${config.configKey}:
											</label>
											<div class="col-md-2">
												<input id="configValue${config.id}" type="text" name="configValue" class="form-control" value="${config.configValue}" />
											</div>
											<div class="col-md-3">
												<input id="info${config.id}" type="text" name="info" class="form-control" value="${config.info}" />
											</div>
											<div class="col-md-2">
												<select id="status${config.id}" name="status" class="form-control">
													<option value="1" <c:if test="${config.status == 1}">selected</c:if>>
														有效
													</option>
													<option value="0" <c:if test="${config.status == 0}">selected</c:if>>
														无效
													</option>
												</select>
											</div>
											<div class="col-md-2">
												<button type="button" class="btn btn-small" onclick="changeConfig('${config.id}','${config.configKey}')">
													修改
												</button>
												<button type="button" class="btn btn-small" onclick="removeConfig('${config.id}','${config.configKey}')">
													删除
												</button>
											</div>
										</div>
									</div>
									<input type="hidden" name="configKey" value="${config.configKey}">
									<input type="hidden" name="id" value="${config.id}">
								</form>
								<!-- END FORM-->
							</div>
						</c:if>
					</c:forEach>
				</div>
				<!-- END TABLE PORTLET-->
			</div>
		</div>
	</div>
</div>

<div id="addRedisConfigModal" class="modal fade" tabindex="-1" data-width="400">
	<div class="modal-dialog">
		<div class="modal-content">

			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true"></button>
				<h4 class="modal-title">添加Redis配置</h4>
			</div>

			<form class="form-horizontal form-bordered form-row-stripped">
				<div class="modal-body">
					<div class="row">
						<!-- 控件开始 -->
						<div class="col-md-12">
							<!-- form-body开始 -->
							<div class="form-body">
								<div class="form-group">
									<label class="control-label col-md-3">
										配置名:
									</label>
									<div class="col-md-5">
										<input type="text" name="configKey" id="configKey"
											   class="form-control" />
									</div>
								</div>

								<div class="form-group">
									<label class="control-label col-md-3">
										配置值:
									</label>
									<div class="col-md-5">
										<input type="text" name="configValue" id="configValue"
											   class="form-control" />
									</div>
								</div>

								<div class="form-group">
									<label class="control-label col-md-3">
										配置说明:
									</label>
									<div class="col-md-5">
										<input type="text" name="info" id="info" class="form-control" />
									</div>
								</div>


								<div class="form-group">
									<label class="control-label col-md-3">
										类型:
									</label>
									<div class="col-md-5">
										<select name="type" id="type" class="form-control select2_category">
											<option value="6">
												Redis普通配置
											</option>
											<c:if test="${type == 2}">
												<option value="2">
													Redis Cluster配置
												</option>
											</c:if>
											<c:if test="${type == 5}">
												<option value="5" >
													Redis Sentinel配置
												</option>
											</c:if>
										</select>
									</div>
								</div>
								<input type="hidden" id="templateId" value="${templateInfo.id}">
							</div>
							<!-- form-body 结束 -->
						</div>
						<!-- 控件结束 -->
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" data-dismiss="modal" class="btn" >Close</button>
					<button type="button" id="configBtn" class="btn red" onclick="saveRedisConfig()">Ok</button>
				</div>

			</form>
		</div>
	</div>
</div>

<div id="createConfigTemplateModal" class="modal fade" tabindex="-1" data-width="400">
	<div class="modal-dialog">
		<div class="modal-content">

			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true"></button>
				<h4 class="modal-title">创建新模板</h4>
			</div>

			<form class="form-horizontal form-bordered form-row-stripped">
				<div class="modal-body">
					<div class="row">
						<!-- 控件开始 -->
						<div class="col-md-12">
							<!-- form-body开始 -->
							<div class="form-body">
								<div class="form-group">
									<label class="control-label col-md-3">
										模板名称:
									</label>
									<div class="col-md-5">
										<input type="text" name="templateName" id="templateName" class="form-control" />
									</div>
								</div>

								<div class="form-group">
									<label class="control-label col-md-3">
										存储类型:
									</label>
									<div class="col-md-5">
										<select name="redisType" id="redisType" onchange="showTemplate(this,'#copyTemplate')">
											<option value="6">
												Redis-Standalone
											</option>
											<option value="2">
												Redis-Cluster
											</option>
											<option value="5" >
												Redis-Sentinel
											</option>
										</select>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label col-md-3">
										拷贝模板:
									</label>
									<div class="col-md-5">
										<select name="templateId" id="copyTemplate">
											<option value="0">空模板</option>
											<c:forEach var="template" items="${defaultList}">
												<option value="${template.id}">
														${template.name}
												</option>
											</c:forEach>
										</select>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label col-md-3">
										额外说明:
									</label>
									<div class="col-md-5">
										<input type="text" name="extraDesc" id="extraDesc" class="form-control" />
									</div>
								</div>
							</div>
							<!-- form-body 结束 -->
						</div>
						<!-- 控件结束 -->
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" data-dismiss="modal" class="btn" >Close</button>
					<button type="button" id="addTemplate" class="btn red" onclick="saveTemplate()">Ok</button>
				</div>

			</form>
		</div>
	</div>
</div>


<div id="modifyConfigTemplateModal" class="modal fade" tabindex="-1" data-width="400">
	<div class="modal-dialog">
		<div class="modal-content">

			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true"></button>
				<h4 class="modal-title">修改模板信息</h4>
			</div>

			<form action="/manage/redisConfig/modifyTemplateInfo.do" method="post" class="form-horizontal form-bordered form-row-stripped">
				<div class="modal-body">
					<div class="row">
						<!-- 控件开始 -->
						<div class="col-md-12">
							<!-- form-body开始 -->
							<div class="form-body">
								<div class="form-group">
									<label class="control-label col-md-3">
										模板名称:
									</label>
									<div class="col-md-5">
										<input type="text" name="templateName" class="form-control" value="${templateInfo.name}"/>
									</div>
								</div>
								<input type="hidden" name="id" value="${templateId}">
								<input type="hidden" name="type" value="${type}">
								<div class="form-group">
									<label class="control-label col-md-3">
										额外说明:
									</label>
									<div class="col-md-5">
										<input type="text" name="extraDesc" class="form-control" value="${templateInfo.extraDesc}"/>
									</div>
								</div>
							</div>
							<!-- form-body 结束 -->
						</div>
						<!-- 控件结束 -->
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" data-dismiss="modal" class="btn" >取消</button>
					<button type="submit" class="btn red">保存</button>
				</div>

			</form>
		</div>
	</div>
</div>

