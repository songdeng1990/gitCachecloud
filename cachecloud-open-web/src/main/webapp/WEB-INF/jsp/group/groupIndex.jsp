<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>CacheCloud分组统计信息</title>
    <jsp:include page="/WEB-INF/include/head.jsp"/>
    <script type="text/javascript" src="/resources/js/jquery-console.js"></script>
    <script type="text/javascript" src="/resources/js/chart.js"></script>

</head>
<body role="document">
<div class="container">
    <jsp:include page="/WEB-INF/include/headMenu.jsp"/>
    <div id="systemAlert">
    </div>
    <div class="tabbable-custom">
        <ul class="nav nav-tabs" id="group_tabs">
            <li class="active"><a href="#group_stat"
                                  data-url="/admin/group/stat.do?groupId=${groupId}&startDate=${startDate}&endDate=${endDate}"
                                  data-toggle="tab">分组统计信息</a></li>
        </ul>
        <div class="tab-content">
            <div class="tab-pane active" id="group_stat">
            </div>
        </div>
    </div>
</div>
<jsp:include page="/WEB-INF/include/foot.jsp"/>
<script type="text/javascript">
    $('#group_tabs a').click(function (e) {
        e.preventDefault();

        var url = $(this).attr("data-url");
        var href = this.hash;
        var pane = $(this);
        var id = $(href).attr("id");
        // ajax load from data-url
        $(href).load(url, function (result) {
            pane.tab('show');
            initChart(id);
        });
    });

    var tabTag = "${tabTag}";
    if (tabTag.length > 0 && $('#' + tabTag).length > 0) {
        var tabId = '#' + tabTag;
        $("a[href=" + tabId + "]").click();
    } else {
        $("a[href=#group_stat]").click();
    }

</script>
<script type="text/javascript" src="/resources/js/mem-cloud.js"></script>
<script type="text/javascript" src="/resources/js/docs.min.js"></script>
</body>
</html>