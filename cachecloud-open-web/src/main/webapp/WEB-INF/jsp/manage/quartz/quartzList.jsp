<%@ page import="org.springframework.ui.Model" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>

<script type="text/javascript" src="/resources/bootstrap/paginator/bootstrap-paginator.js"></script>
<script type="text/javascript" src="/resources/bootstrap/paginator/custom-pagenitor.js"></script>
<div class="page-container">
	<div class="page-content">
		<div class="row">
			<div class="col-md-12">
				<h3 class="page-title">
					Quartz管理
				</h3>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div class="portlet box light-grey">
					<div class="portlet-title">
						<div class="caption"><i class="fa fa-globe"></i>trigger列表</div>
						<div class="tools">
							<a href="javascript:;" class="collapse"></a>
						</div>
					</div>
					<div class="portlet-body">
                        <div class="table-toolbar">
							<a onclick="if(window.confirm('确认批量恢复吗?!')){return true;}else{return false;}"
							   href="/manage/quartz/batch/resume.do">[批量恢复]&nbsp;&nbsp;
							</a>
							<a onclick="if(window.confirm('确认批量暂停吗?!')){return true;}else{return false;}"
							   href="/manage/quartz/batch/pause.do">[批量暂停]&nbsp;&nbsp;
							</a>
							<a onclick="if(window.confirm('确认批量删除吗?!')){return true;}else{return false;}"
							   href="/manage/quartz/batch/remove.do">[批量删除]&nbsp;&nbsp;
							</a>
                            <div class="btn-group" style="float:right">
                                <form action="/manage/quartz/list.do" method="post" class="form-horizontal form-bordered form-row-stripped"
								id="quarztList">
                                    <label class="control-label">
                                        查询:
                                    </label>
                                    &nbsp;<input type="text" name="query" id="ipLike" value="${query}" placeholder=""/>
                                    &nbsp;<button type="submit" class="btn blue btn-sm">查询</button>
									<input type="hidden" name="pageNo" id="pageNo">
									<input type="hidden" name="pageNo" id="pageSize">

                        </div>
						<table class="table table-striped table-bordered table-hover" id="tableDataList">
							<thead>
								<tr>
									<th>triggerName</th>
									<th>triggerGroup</th>
                                    <th>cron</th>
                                    <th>nextFireDate</th>
									<th>prevFireDate</th>
                                    <th>startDate</th>
									<th>triggerState</th>
									<th>操作</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${triggerList}" var="t">
									<tr class="odd gradeX">
										<td>${t.triggerName}</td>
										<td>${t.triggerGroup}</td>
                                        <td>${t.cron}</td>
                                        <td>${t.nextFireDate}</td>
										<td>${t.prevFireDate}</td>
                                        <td>${t.startDate}</td>
										<td>${t.triggerState}</td>
										<td>
                                        <c:if test="${t.triggerState == 'PAUSED'}">
                                            <a onclick="if(window.confirm('确认恢复吗?!')){return true;}else{return false;}"
                                               href="/manage/quartz/resume.do?name=${t.triggerName}&group=${t.triggerGroup}">[恢复]
                                            </a>
                                        </c:if>
                                        <c:if test="${t.triggerState != 'PAUSED'}">
                                            <a onclick="if(window.confirm('确认暂停吗?!')){return true;}else{return false;}"
                                               href="/manage/quartz/pause.do?name=${t.triggerName}&group=${t.triggerGroup}">[暂停]
                                            </a>
                                        </c:if>
                                        <a onclick="if(window.confirm('确认删除吗?!')){return true;}else{return false;}"
                                               href="/manage/quartz/remove.do?name=${t.triggerName}&group=${t.triggerGroup}">[删除]
                                        </a>
										</td>
									</tr>
								</c:forEach>
							</tbody>

						</table>
						<div style="margin-bottom: 10px; float: right; margin-right: 15px">
							<div style="float: left;padding-bottom: 10px;">
								<div class="form-group">
									<select name="pageSize" class="form-control" id="page_size">
										<option value="50"
												<c:if test="${page.pageSize == 50}">selected</c:if>>
											50行
										</option>
										<option value="100"
												<c:if test="${page.pageSize == 100}">selected</c:if>>
											100行
										</option>
										<option value="200"
												<c:if test="${page.pageSize == 200}">selected</c:if>>
											200行
										</option>
									</select>
								</div>
							</div>
							<span>
							  <ul id='ccPagenitor' style="margin-bottom: 0px; margin-top: 0px"></ul>
							  <div id="pageDetail"
								   style="float: right; padding-top: 7px; padding-left: 8px; color: #4A64A4; display: none">共${page.totalPages}页,${page.totalCount}条</div>
						    </span>
						</div>
							</form>
						</div>
					</div>
				</div>

			</div>
		</div>
	</div>
</div>


<script type="text/javascript">
    $(function () {

        //分页点击函数
        var pageClickedFunc = function (e, originalEvent, type, page) {
            //form传参用pageSize
            document.getElementById("pageNo").value = page;
            document.getElementById("pageSize").value = document.getElementById("page_size").value;
            document.getElementById("quarztList").submit();
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
        if (totalPages > 0) {
            $("#ccPagenitor").bootstrapPaginator(options);
            document.getElementById("pageDetail").style.display = "";
        } else {
            element.html("未查询到相关记录！");
        }


    });
</script>

