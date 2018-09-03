<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>
<div class="page-container">
	<div class="page-content">
		<div class="row">
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption">
							<i class="fa fa-globe"></i>全局统计
						</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
						</div>
					</div>
					<div class="portlet-body">

						<div class="table-toolbar">
							<table class="table table-striped table-bordered table-hover">
								<tr>
									<td>机器总数</td>
									<td>${totalStat.machineNum}台
									</td>
									<td>机器总内存</td>
									<td><fmt:formatNumber
											value="${totalStat.totalMem/1024}" pattern="0.00" />G</td>
									<td>已使用比例</td>
									<td><fmt:formatNumber
											value="${totalStat.memUseRatio}" pattern="0.00" />%
									</td>
									<td>应用数目</td>
									<td>${totalStat.appNum}个</td>
									<td>实例数目</td>
									<td>${totalStat.instanceNum}个</td>
									<td>最近1小时平均ops</td>
									<td>${totalStat.ops}</td>
								</tr>

							</table>
							<br />
							<h3>
								分组统计详情：
							</h3>
						</div>
						<title>分组列表</title>
						<div style="float: left;padding-bottom: 10px;">
						</div>
						<table class="table table-striped table-bordered table-hover">
							<thead>
								<tr>
									<td>组ID</td>
									<td>组名称</td>
									<td>机器数(个)</td>
									<td>总内存(G)</td>
									<td>已使用比例</td>
									<td>应用数目(个)</td>
									<td>实例数目(个)</td>
									<td>最近1小时平均ops</td>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${machineGroupStat}" var="machineGroupStat">
									<tr class="odd gradeX">
										<td>${machineGroupStat.groupId}</td>
										<td>${machineGroupStat.groupName}</td>
										<td>${machineGroupStat.machineNum}</td>
										<td><fmt:formatNumber
												value="${machineGroupStat.totalMem/1024}" pattern="0.00" /></td>
										<td><fmt:formatNumber
												value="${machineGroupStat.memUseRatio}" pattern="0.00" />%</td>
										<td>${machineGroupStat.appNum}</td>
										<td>${machineGroupStat.instanceNum}</td>
										<td>${machineGroupStat.ops}</td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
						<div style="margin-bottom: 10px; float: right; margin-right: 15px">
						<span>
							<ul id='ccPagenitor' style="margin-bottom: 0px; margin-top: 0px"></ul>
							<div id="pageDetail"
								style="float: right; padding-top: 7px; padding-left: 8px; color: #4A64A4; display: none">共${page.totalPages}页,${page.totalCount}条</div>
						</span>
					</div>
					</div>

				</div>
			</div>
		</div>
	</div>
</div>
