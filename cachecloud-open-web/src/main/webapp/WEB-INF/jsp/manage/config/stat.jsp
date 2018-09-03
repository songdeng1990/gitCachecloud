<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>

<script type="text/javascript">
    var startDate = '${startDate}';
    var endDate = '${endDate}';
    var betweenParams = "startDate="+startDate+"&endDate="+endDate;
</script>

<div class="page-container">
    <div class="page-content">
        <div class="row">
            <div class="col-md-12">
                <h3 class="page-title">
                    <c:choose>
                        <c:when test="${success == 1}">
                            <font color="red">更新成功</font>
                        </c:when>
                        <c:when test="${success == 0}">
                            <font color="red">更新失败</font>
                        </c:when>
                    </c:choose>
                </h3>
            </div>
        </div>

        <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>
                            采集任务堆积查看:
                            &nbsp;
                        </div>
                        <div class="tools">
                            <a href="javascript:;" class="collapse"></a>
                        </div>
                    </div>

                    <div class="form">
                        <div class="row">
                            <div style="min-width: 310px; width: 800px; margin: 0 auto">
                                <form method="post" action="/admin/app/redictQueneJsp.do" id="ec" name="ec">
                                    <label style="font-weight:bold;text-align:left;">
                                        开始日期:&nbsp;&nbsp;
                                    </label>
                                    <input type="text" size="21" name="startDate" id="startDate" value="${startDate}" onFocus="WdatePicker({startDate:'%y-%M-01',dateFmt:'yyyy-MM-dd',alwaysUseStartDate:true})"/>
                                    <label style="font-weight:bold;text-align:left;">
                                        结束日期:
                                    </label>
                                    <input type="text" size="20" name="endDate" id="endDate" value="${endDate}" onFocus="WdatePicker({startDate:'%y-%M-01',dateFmt:'yyyy-MM-dd',alwaysUseStartDate:true})"/>
                                    <input type="hidden" name="appId" value="${appDetail.appDesc.appId}">
                                    <label>&nbsp;<input type="submit" class="btn-4" value="查询"/></label>
                                </form>
                            </div>
                        </div>
                        <!-- BEGIN FORM-->
                        <div id="containerHits"
                             style="min-width: 310px; width: 800px;height: 350px; margin: 0 auto"></div>

                        <!-- END FORM-->
                    </div>
                </div>
                <div class="form">
                    <!-- BEGIN FORM-->
                    <form action="/manage/config/updateQueueRadio.d" method="post"
                          class="form-horizontal form-bordered form-row-stripped">
                        <div class="form-body">

                            <c:forEach items="${configList}" var="config" varStatus="stats">
                                <c:choose>
                                    <c:when test="${config.configKey == 'collect.queue.alert.ratio'}">
                                        <div class="form-group">
                                           <label class="control-label col-md-3">
                                                ${config.info}<font color='red'>(*)</font>:
                                           </label>
                                        <div class="col-md-5">

                                        <input type="text" name="${config.configKey}" class="form-control" style="width: 30%"
                                               value="${config.configValue}"/>
                                        </div>
                                        </div>
                                    </c:when>

                                </c:choose>
                            </c:forEach>

                            <div class="form-actions fluid">
                                <div class="row">
                                    <div class="col-md-12">
                                        <div class="col-md-offset-3 col-md-9">
                                            <button type="submit" class="btn green">
                                                <i class="fa fa-check"></i>
                                                确认修改
                                            </button>
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
            <!-- END TABLE PORTLET-->
        </div>
    </div>

</div>
</div>



<script type="text/javascript">

    //查询一天出每分钟数据
        $(document).ready(
            function() {
                var startDate = '${startDate}';
                var yesterDate = '${yesterDate}';
                Highcharts.setOptions({
                    global : {
                        useUTC : false
                    }
                });
                Highcharts.setOptions({
                    colors : [ '#2f7ed8', '#E3170D', '#0d233a', '#8bbc21', '#1aadce',
                        '#492970', '#804000', '#f28f43', '#77a1e5',
                        '#c42525', '#a6c96a' ]
                });
                var options = getOption("containerHits", "<b>堆积统计</b>", "个数");
                var commandsUrl = "/admin/app/getMutiQueneSizeStats.json?" + betweenParams;

                $.ajax({
                    type : "get",
                    url : commandsUrl,
                    async : true,
                    success : function(data) {
                        var dates = new Array();
                        dates.push(startDate);
                        //dates.push(yesterDate);
                        pushOptionSeries(options, data, dates, "队列堆积趋势图","个");
                        new Highcharts.Chart(options);
                    }
                });
            });
</script>
