<%@ page language="java" contentType="text/html; charset=UTF-8"
        pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp" %>
<link rel="SHORTCUT ICON" href="/resources/img/db.logo"/>


<div id="123456" class="page-container">
    <div class="page-content">
        <div class="row">
            <div class="col-md-12">
                <h3 class="page-title">机器管理</h3>
            </div>
        </div>
        <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>机器列表
                        </div>
                        <div class="tools">
                            <a href="javascript:;" class="collapse"></a>
                        </div>
                    </div>
                    <div class="portlet-body">
                        <div class="table-toolbar">
                            <div class="btn-group">
                                <button id="sample_editable_1_new" class="btn green"
                                        data-target="#addMachineModal" data-toggle="modal">
                                    添加新机器 <i class="fa fa-plus"></i>
                                </button>
                            </div>
                            <div class="btn-group">
                                <button id="sample_editable_batch_new" class="btn btn-info"
                                        data-target="#addBatchMachineModal" data-toggle="modal">
                                    批量添加 <i class="fa fa-plus"></i>
                                </button>
                            </div>
                            <div class="btn-group">

                                <button id="update_monitor" type="button" class="btn btn-info"
                                        onclick="update_monitor()">更新监控脚本
                                </button>
                            </div>
                            
                             <div class="btn-group">

                                <button id="update_machine_info" type="button" class="btn btn-info"
                                        onclick="update_machine_info()">更新机器信息
                                </button>
                            </div>
                            
                            <div class="btn-group">
                                <a href="/manage/machine/machineGroupStat.do" class="btn btn-info"
                                   id="getMachineGroupStat">机器分组详情
                                </a>
                            </div>
                            <div class="btn-group" style="float: right">
                                <form action="/manage/machine/list.do" method="post" id="machineList"
                                      class="form-horizontal form-bordered form-row-stripped">
                                    <label class="control-label">
                                        所属分组: </label> &nbsp;
                                    <div class="machineGroup" style="height:400px; overflow:auto;display:none;">
                                        <table class="machineGroup">
                                            <c:forEach items="${machineGroup}" var="group">
                                                <tr>
                                                    <td>${group}</td>
                                                </tr>
                                            </c:forEach>
                                        </table>
                                    </div>
                                    &nbsp;<input type="text" name="groupName" id="groupName"
                                                 value="${groupName}" placeholder="分组名"/>
                                    <label class="control-label"> 机器ip: </label> &nbsp;<input
                                        type="text" name="ipLike" id="ipLike" value="${ipLike}"
                                        placeholder="iplike"/> <label class="control-label">
                                    额外说明: </label> &nbsp;
                                    <div class="groupSelect" style="height:400px; overflow:auto;display:none;">
                                        <table class="groupSelect">
                                            <c:forEach items="${groupSet}" var="group">
                                                <tr>
                                                    <td>${group}</td>
                                                </tr>
                                            </c:forEach>
                                        </table>
                                    </div>
                                    &nbsp;<input type="text" name="extraDesc" id="extraDesc"
                                                 value="${extraDesc}" placeholder="额外描述like匹配"/>

                                    &nbsp;
                                    <input type="hidden" name="pageNo" id="pageNo">
                                    <input type="hidden" name="pageNo" id="pageSize">
                                    <button type="submit" class="btn blue btn-sm">查询</button>

                            </div>

                        </div>
                        <table class="table table-striped table-bordered table-hover"
                               id="tableDataList">
                            <thead>
                            <tr>
                                <th>ip</th>
                                <th>额外说明</th>
                                <th>所属分组</th>
                                <th>实例数目</th>
                                <th>内存使用率</th>
                                <th>已分配内存</th>
                                <th>CPU使用率</th>
                                <th>网络流量</th>
                                <th>机器负载</th>
                                <th>最后统计时间</th>
                                <th>是否虚机</th>
                                <th>机房</th>
                                <th>状态收集</th>
                                <th>已安装redis</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${list}" var="machine">
                                <tr class="odd gradeX">
                                    <td><a target="_blank"
                                           href="/manage/machine/machineInstances.do?ip=${machine.info.ip}">${machine.info.ip}</a>
                                    </td>
                                    <th>${machine.info.extraDesc}
                                        <c:if
                                                test="${machine.info.type == 2}">
                                            <font color='red'>(迁移工具机器)</font>
                                        </c:if>
                                    </th>
                                    <th>${machine.info.groupName}
                                    </th>
                                    <th>${machine.instanceNum}</th>
                                    <td><c:choose>
                                        <c:when
                                                test="${machine.memoryUsageRatio == null || machine.memoryUsageRatio == ''}">
                                            收集中..${collectAlert}
                                        </c:when>
                                        <c:otherwise>
													<span style="display: none"><fmt:formatNumber
                                                            value="${machine.memoryUsageRatio / 100}"
                                                            pattern="0.00"/></span>
                                            <div class="progress margin-custom-bottom0">
                                                <c:choose>
                                                    <c:when test="${fmtMemoryUsageRatio >= 80.00}">
                                                        <c:set var="memUsedProgressBarStatus"
                                                               value="progress-bar-danger"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:set var="memUsedProgressBarStatus"
                                                               value="progress-bar-success"/>
                                                    </c:otherwise>
                                                </c:choose>
                                                <fmt:formatNumber var="fmtMemoryUsageRatio"
                                                                  value="${machine.memoryUsageRatio}" pattern="0.00"/>
                                                <div class="progress-bar ${memUsedProgressBarStatus}"
                                                     role="progressbar"
                                                     aria-valuenow="${machine.memoryUsageRatio}"
                                                     aria-valuemax="100" aria-valuemin="0"
                                                     style="width: ${machine.memoryUsageRatio}%">
                                                    <label style="color: #000000"> <fmt:formatNumber
                                                            value="${((machine.memoryTotal-machine.memoryFree)/1024/1024/1024)}"
                                                            pattern="0.00"/>G&nbsp;&nbsp;Used/<fmt:formatNumber
                                                            value="${ machine.memoryTotal/1024/1024/1024}"
                                                            pattern="0.00"/>G&nbsp;&nbsp;Total
                                                    </label>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose></td>
                                    <td><c:choose>
                                        <c:when
                                                test="${machine.memoryUsageRatio == null || machine.memoryUsageRatio == ''}">
                                            收集中..${collectAlert}
                                        </c:when>
                                        <c:otherwise>
                                            <fmt:formatNumber var="fmtMemoryAllocatedRatio"
                                                              value="${((machine.memoryAllocated)/1024)*100.0/(machine.memoryTotal/1024/1024/1024)}"
                                                              pattern="0.00"/>
                                            <span style="display: none"><fmt:formatNumber
                                                    value="${fmtMemoryAllocatedRatio / 100}" pattern="0.00"/></span>
                                            <div class="progress margin-custom-bottom0">
                                                <c:choose>
                                                    <c:when test="${fmtMemoryAllocatedRatio >= 80.00}">
                                                        <c:set var="memAllocateProgressBarStatus"
                                                               value="progress-bar-danger"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:set var="memAllocateProgressBarStatus"
                                                               value="progress-bar-success"/>
                                                    </c:otherwise>
                                                </c:choose>
                                                <div class="progress-bar ${memAllocateProgressBarStatus}"
                                                     role="progressbar"
                                                     aria-valuenow="${fmtMemoryAllocatedRatio}"
                                                     aria-valuemax="100" aria-valuemin="0"
                                                     style="width: ${fmtMemoryAllocatedRatio}%">
                                                    <label style="color: #000000"> <fmt:formatNumber
                                                            value="${((machine.memoryAllocated)/1024)}"
                                                            pattern="0.00"/>G&nbsp;&nbsp;Used/<fmt:formatNumber
                                                            value="${ machine.memoryTotal/1024/1024/1024}"
                                                            pattern="0.00"/>G&nbsp;&nbsp;Total
                                                    </label>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose></td>
                                    <td><c:choose>
                                        <c:when
                                                test="${machine.cpuUsage == null || machine.cpuUsage == ''}">
                                            收集中..${collectAlert}
                                        </c:when>
                                        <c:otherwise>
                                            ${machine.cpuUsage}
                                        </c:otherwise>
                                    </c:choose></td>
                                    <td><fmt:formatNumber
                                            value="${machine.traffic / 1024 / 1024}" pattern="0.00"/>M
                                    </td>
                                    <td><c:choose>
                                        <c:when test="${machine.load == null || machine.load == ''}">
                                            收集中..${collectAlert}
                                        </c:when>
                                        <c:otherwise>
                                            ${machine.load}
                                        </c:otherwise>
                                    </c:choose></td>
                                    <td><fmt:formatDate value="${machine.modifyTime}"
                                                        type="time" timeStyle="full" pattern="yyyy-MM-dd HH:mm"/></td>
                                    <th><c:choose>
                                        <c:when test="${machine.info.virtual == 1}">
                                            是
                                            <br/>
                                            物理机:${machine.info.realIp}
                                        </c:when>
                                        <c:otherwise>
                                            否
                                        </c:otherwise>
                                    </c:choose></th>
                                    <th>${machine.info.room}</th>

                                    <c:choose>
                                        <c:when test="${machine.info.collect == 1}">
                                            <td>开启</td>
                                        </c:when>
                                        <c:otherwise>
                                            <th>关闭</th>
                                        </c:otherwise>
                                    </c:choose>
                                    <td id="installedFlag${machine.info.id}"><c:choose>
                                        <c:when test="${machine.info.installed == 1}">已安装</c:when>
                                        <c:otherwise>未安装</c:otherwise>
                                    </c:choose></td>
                                    <td>
                                        <button id="initMachineBtn${machine.info.id}"
                                                onclick="initMachine('${machine.info.id}','${machine.info.ip}','${machine.info.installed}')"
                                                type="button" class="btn btn-info">安装Redis
                                        </button>
                                        <a
                                                href="javascript;"
                                                data-target="#addMachineModal${machine.info.id}"
                                                class="btn btn-info" data-toggle="modal">修改</a> &nbsp;

                                        <button id="removeMachineBtn${machine.info.id}"
                                                onclick="removeMachine(this.id,'${machine.info.ip}')"
                                                type="button" class="btn btn-info">删除
                                        </button>

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
                            </form>
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
        <c:forEach items="${list}" var="machine">
            <%@include file="addMachine.jsp" %>
        </c:forEach>
        <%@include file="addMachine.jsp" %>
    </div>


    <script type="text/javascript">
        $(function () {

            //分页点击函数
            var pageClickedFunc = function (e, originalEvent, type, page) {
                //form传参用pageSize
                document.getElementById("pageNo").value = page;
                document.getElementById("pageSize").value = document.getElementById("page_size").value;
                document.getElementById("machineList").submit();
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

