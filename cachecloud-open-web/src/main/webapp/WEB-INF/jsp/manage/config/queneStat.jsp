<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CacheCloud管理后台</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">

    <script src="/resources/manage/plugins/jquery-1.10.2.min.js" type="text/javascript"></script>
    <script type="text/javascript" src="/resources/js/jquery-console.js"></script>
    <script type="text/javascript" src="/resources/highchart3/js/highcharts.js"></script>
    <script type="text/javascript" src="/resources/js/myhighchart.js"></script>
    <script type="text/javascript" src="/resources/My97DatePicker/WdatePicker.js"></script>

    <%@include file="/WEB-INF/jsp/manage/include/cache_cloud_main_css.jsp" %>

</head>

<body class="page-header-fixed">
<%@include file="/WEB-INF/jsp/manage/include/head.jsp" %>

<%@include file="/WEB-INF/jsp/manage/include/left.jsp" %>

<%@include file="stat.jsp" %>

<%@include file="/WEB-INF/jsp/manage/include/foot.jsp" %>

<%@include file="/WEB-INF/jsp/manage/include/cache_cloud_main_js.jsp" %>

<script type="text/javascript" src="/resources/js/mem-cloud.js"></script>
<script type="text/javascript" src="/resources/js/docs.min.js"></script>
</body>
</html>