<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>
<div class="page-container">
	<div class="page-content">
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">全局统计</h3>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption">
							<i class="fa fa-globe"></i>
							全局统计
						</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
						</div>
					</div>
					<div class="portlet-body">
						<div class="table-toolbar">
							<table class="table table-striped table-bordered table-hover">
								<tr>
									<td>机器总内存</td>
									<td><fmt:formatNumber value="${totalMachineMem/1024/1024/1024}" pattern="0.00" />G</td>
									<td>机器空闲内存</td>
									<td><fmt:formatNumber value="${totalFreeMachineMem/1024/1024/1024}" pattern="0.00" />G</td>
									<td>实例总内存</td>
									<td><fmt:formatNumber value="${totalInstanceMem/1024/1024/1024}" pattern="0.00" />G</td>
									<td>实例总使用内存</td>
									<td><fmt:formatNumber value="${totalUseInstanceMem/1024/1024/1024}" pattern="0.00" />G</td>
								</tr>
								<tr>
									<td>应用总数</td>
									<td>${totalApps}</td>
									<td>运行中应用数</td>
									<td>${totalRunningApps}</td>
									<td>应用总申请内存</td>
									<td><fmt:formatNumber value="${totalApplyMem/1024}" pattern="0.00" />G</td>
									<td>应用已使用内存</td>
									<td><fmt:formatNumber value="${totalUsedMem/1024}" pattern="0.00" />G</td>
								</tr>
							</table>
							<br />
							<h3>
								集群当前可对外提供空间：
								<fmt:formatNumber value="${(totalFreeMachineMem-(totalInstanceMem-totalUseInstanceMem))/1024/1024/1024}" pattern="0.00" />
								G
							</h3>
						</div>
						<title>应用列表</title>
						<div style="float: left; padding-bottom: 10px;">
							<form class="form-inline" method="post" action="/manage/total/list.do" id="appList" name="ec">
								<div class="form-group">
									<select name="businessGroupId" class="form-control">
										<option value="0">所有组</option>
										<c:forEach var="group" items="${appGroupList}">
											<option value="${group.businessGroupId}" <c:if test="${appSearch.businessGroupId == group.businessGroupId}">selected</c:if>>${group.businessGroupName}</option>
										</c:forEach>
									</select>
								</div>
								<div class="form-group">
									<input type="text" class="form-control" id="appId" name="appId" value="${appSearch.appId}" placeholder="应用ID" onchange="testisNum(this.id)">
								</div>
								<div class="form-group">
									<input type="text" class="form-control" id="appName" name="appName" value="${appSearch.appName}" placeholder="应用名">
								</div>
								<div class="form-group">
									<select name="appType" class="form-control">
										<option value="">全部类型</option>
										<option value="2" <c:if test="${appSearch.appType == 2}">selected</c:if>>Redis-cluster</option>
										<option value="5" <c:if test="${appSearch.appType == 5}">selected</c:if>>redis-sentinel</option>
										<option value="6" <c:if test="${appSearch.appType == 6}">selected</c:if>>redis-standalone</option>
									</select>
								</div>
								<div class="form-group">
									<select name="appStatus" class="form-control">
										<option value="">全部状态</option>
										<option value="0" <c:if test="${appSearch.appStatus == 0}">selected</c:if>>未分配</option>
										<option value="1" <c:if test="${appSearch.appStatus == 1}">selected</c:if>>申请中</option>
										<option value="2" <c:if test="${appSearch.appStatus == 2}">selected</c:if>>运行中</option>
										<option value="3" <c:if test="${appSearch.appStatus == 3}">selected</c:if>>已下线</option>
										<option value="4" <c:if test="${appSearch.appStatus == 4}">selected</c:if>>驳回</option>
									</select>
								</div>
								<div class="form-group">
									<select name="pageSize" class="form-control">
										<option value="10" <c:if test="${page.pageSize == 10}">selected</c:if>>10行</option>
										<option value="20" <c:if test="${page.pageSize == 20}">selected</c:if>>20行</option>
										<option value="50" <c:if test="${page.pageSize == 50}">selected</c:if>>50行</option>
										<option value="100" <c:if test="${page.pageSize == 100}">selected</c:if>>100行</option>
									</select>
								</div>
								<input type="hidden" name="pageNo" id="pageNo">
								<button type="submit" class="btn btn-default">查询</button>
							</form>
						</div>
						<table class="table table-striped table-bordered table-hover">
							<thead>
								<tr>
									<td>应用ID</td>
									<td>应用名</td>
									<td>应用类型</td>
									<td>配置模板</td>
									<td>内存详情</td>
									<td>命中率</td>
									<td>已运行时间(天)</td>
									<td>申请状态</td>
									<td>操作</td>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${appDetailList}" var="appDetail">
									<tr class="odd gradeX">
										<td><c:choose>
												<c:when test="${appDetail.appDesc.status == 0 or appDetail.appDesc.status == 1}">
                                                ${appDetail.appDesc.appId}
                                            </c:when>
												<c:when test="${appDetail.appDesc.status == 2 or appDetail.appDesc.status == 3 or appDetail.appDesc.status == 4}">
													<a target="_blank" href="/manage/app/index.do?appId=${appDetail.appDesc.appId}">${appDetail.appDesc.appId}</a>
												</c:when>
											</c:choose></td>
										<td><c:choose>
												<c:when test="${appDetail.appDesc.status == 0 or appDetail.appDesc.status == 1}">
                                                ${appDetail.appDesc.name}
                                            </c:when>
												<c:when test="${appDetail.appDesc.status == 2 or appDetail.appDesc.status == 3 or appDetail.appDesc.status == 4}">
													<a target="_blank" href="/admin/app/index.do?appId=${appDetail.appDesc.appId}">${appDetail.appDesc.name}</a>
												</c:when>
											</c:choose></td>
										<td><c:if test="${appDetail.appDesc.status == 2 or appDetail.appDesc.status == 3 or appDetail.appDesc.status == 4}">
                                            ${appDetail.appDesc.typeDesc}
                                        </c:if></td>
										<td>${appDetail.templateName}</td>
										<td><span style="display: none"><fmt:formatNumber value="${appDetail.memUsePercent / 100}" pattern="0.00" /></span>
											<div class="progress margin-custom-bottom0">
												<c:choose>
													<c:when test="${appDetail.memUsePercent >= 80}">
														<c:set var="progressBarStatus" value="progress-bar-danger" />
													</c:when>
													<c:otherwise>
														<c:set var="progressBarStatus" value="progress-bar-success" />
													</c:otherwise>
												</c:choose>
												<div class="progress-bar ${progressBarStatus}" role="progressbar" aria-valuenow="${appDetail.memUsePercent}" aria-valuemax="100" aria-valuemin="0" style="width: ${appDetail.memUsePercent}%">
													<label style="color: #000000"> <fmt:formatNumber value="${appDetail.mem  * appDetail.memUsePercent / 100 / 1024}" pattern="0.00" />G&nbsp;&nbsp;Used/<fmt:formatNumber
															value="${appDetail.mem / 1024 * 1.0}" pattern="0.00" />G&nbsp;&nbsp;Total
													</label>
												</div>
											</div></td>
										<td><span style="display: none"><fmt:formatNumber value="${appDetail.hitPercent / 100}" pattern="0.00" /></span> <c:choose>
												<c:when test="${appDetail.hitPercent <= 0}">
                                                	无
                                            </c:when>
												<c:when test="${appDetail.hitPercent <= 30}">
													<label class="label label-danger">${appDetail.hitPercent}%</label>
												</c:when>
												<c:when test="${appDetail.hitPercent >= 30 && appDetail.hitPercent < 50}">
													<label class="label label-warning">${appDetail.hitPercent}%</label>
												</c:when>
												<c:when test="${appDetail.hitPercent >= 50 && appDetail.hitPercent < 90}">
													<label class="label label-info">${appDetail.hitPercent}%</label>
												</c:when>
												<c:otherwise>
													<label class="label label-success">${appDetail.hitPercent}%</label>
												</c:otherwise>
											</c:choose></td>
										<td>${appDetail.appDesc.appRunDays}</td>
										<td><c:choose>
												<c:when test="${appDetail.appDesc.status == 0}">
													<font color="red">未申请</font>
												</c:when>
												<c:when test="${appDetail.appDesc.status == 1}">
													<font color="red">申请中</font>
												</c:when>
												<c:when test="${appDetail.appDesc.status == 2}">
                                                                                                                        运行中
                                            </c:when>
												<c:when test="${appDetail.appDesc.status == 3}">
													<font color="red">已下线</font>
												</c:when>
												<c:when test="${appDetail.appDesc.status == 4}">
													<font color="red">驳回</font>
												</c:when>
											</c:choose></td>
										<td><c:choose>
												<c:when test="${appDetail.appDesc.status == 2}">
													<button type="button" class="btn btn-small btn-primary" id="offline${appDetail.appDesc.appId}" onclick="offLine(${appDetail.appDesc.appId})">应用下线</button>
													<a target="_blank" type="button" class="btn btn-small btn-primary" href="/manage/app/index.do?appId=${appDetail.appDesc.appId}">应用运维</a>
													<button type="button" class="btn btn-small btn-primary" id="syncCache${appDetail.appDesc.appId}" onclick="syncCache(${appDetail.appDesc.appId})">同步缓存</button>
												</c:when>
											</c:choose></td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
						<div style="margin-bottom: 10px; float: right; margin-right: 15px">
							<span>
								<ul id='ccPagenitor' style="margin-bottom: 0px; margin-top: 0px"></ul>
								<div id="pageDetail" style="float: right; padding-top: 7px; padding-left: 8px; color: #4A64A4; display: none">共${page.totalPages}页,${page.totalCount}条</div>
							</span>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<script type="text/javascript">
    function offLine(appId) {
    	if(confirm("确认要下线该应用？应用id="+appId)){
            $.ajax({
                type: "get",
                url: "/manage/app/offLine.json",
                data: {appId: appId},
                success: function (result) {
                    alert(result.msg);
                    window.location.reload();
                }
            });
        }
    }
    
    function syncCache(appId) {
    	if(confirm("确认要同步信息到统一缓存平台？应用id="+appId)){
            $.ajax({
                type: "get",
                url: "/manage/app/syncToUnionCache.json",
                data: {appId: appId},
                success: function (result) {
                    alert(result.msg);
                    window.location.reload();
                }
            });
        }
    }
</script>
<script type="text/javascript" src="/resources/js/mem-cloud.js"></script>
<script type="text/javascript" src="/resources/bootstrap/paginator/bootstrap-paginator.js"></script>
<script type="text/javascript" src="/resources/bootstrap/paginator/custom-pagenitor.js"></script>
<script type="text/javascript">
    $(function(){
    
    		//分页点击函数
        	var pageClickedFunc = function (e, originalEvent, type, page){
        		//form传参用pageSize
        		document.getElementById("pageNo").value=page;
        		document.getElementById("appList").submit();
        	};
        	//分页组件
            var element = $("#ccPagenitor");
            //当前page号码
            var pageNo = '${page.pageNo}';
            //总页数
            var totalPages = '${page.totalPages}';
            //显示总页数
            var numberOfPages = '${page.numberOfPages}';
    		var options = generatePagenitorOption(pageNo, numberOfPages, totalPages, pageClickedFunc);
    		if(totalPages > 0){
    			$("#ccPagenitor").bootstrapPaginator(options);
    			document.getElementById("pageDetail").style.display = "";
    		}else{
    			element.html("未查询到相关记录！");
    		}
    	
    	
    });
</script>