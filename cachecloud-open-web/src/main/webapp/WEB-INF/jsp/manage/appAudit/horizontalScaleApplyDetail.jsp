<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>


<div class="page-container">
	<div class="page-content">
		
		<%@include file="machineForHorizontalScaleList.jsp" %>
		
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">
					应用水平扩容(申请详情:<font color="red">${appAudit.info}</font>)
				</h3>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption">
							<i class="fa fa-globe"></i>
							填写扩容配置(用于创建新的redis节点并加入当前cluster):
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
								<form action="/manage/app/addAppClusterSharding.do" method="post"
									class="form-horizontal form-bordered form-row-stripped" onsubmit="return checkAddShardParam()">
									<div class="form-body">
										<div class="form-group">
											<label class="control-label col-md-3">主从分片配置:<br/>跳转时间可能较长请耐心等待</label>
											<div class="col-md-5">
												<textarea rows="10" name="masterSizeSlaves" id="masterSizeSlaves" placeholder="materIp:memSize:slaveIp" class="form-control"></textarea>
												<!-- <input type="text" name="masterSizeSlave" id="masterSizeSlave" placeholder="materIp:memSize:slaveIp" class="form-control"> -->
											</div>											
										</div>
										<input type="hidden" name="appAuditId" id="appAuditId" value="${appAudit.id}">
										<input type="hidden" name="deployConfigId" id="deployConfigId" value="">
										<input type="hidden" name="addNewNodeInputText" id="addNewNodeInputText" value="">
										<div class="form-actions fluid">
											<div class="row">
												<div class="col-md-12">
													<div class="col-md-offset-3 col-md-3">
														<button id="addNewNodeBtn" type="button" class="btn green" disabled="disabled" onclick="addAppShardingText()">
															<i class="fa fa-check"></i>
															提交
														</button>
														<button id="addNewNodeCheckBtn" type="button" class="btn green" onclick="checkHorizontalScaleText('masterSizeSlaves','addNewNodeCheckBtn','addNewNodeBtn','addNewNodeInputText')">
															<i class="fa fa-check"></i>
															格式检查															
														</button>														
																											
														&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
														<a target="_blank" class="btn green" href="/manage/app/handleHorizontalScale?appAuditId=${appAudit.id}">ReShard页面</a>
														<label id="startDeployLabel"></label>
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

