<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/manage/commons/taglibs.jsp" %>


<div class="page-container">
    <div class="page-content">
    
        <%@include file="appIntanceReferList.jsp" %>
    	<%@include file="horizontalScaleProcessList.jsp" %>

        <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>
                            	填写扩容配置
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
                            <form action="/manage/app/addHorizontalScaleApply.do" method="post"
                                  class="form-horizontal form-bordered form-row-stripped"
                                  onsubmit="return checkOnlineScaleParam('onlineText','onlineAddresses');">
                                <div class="form-body">
                                    <label class="control-label col-md-3">扩容地址:（多个地址用换行分隔）</label>
									<div class="col-md-5">
											<textarea rows="10" name="onlineText" id="onlineText" placeholder="materIp:port" class="form-control"></textarea>									
									</div>
                                    
                                    <input type="hidden" name="appId" value="${appAudit.appId}">
                                    <input type="hidden" name="appAuditId" value="${appAudit.id}">
                                    <input type="hidden" name="onlineAddresses" id="onlineAddresses" value="">

                                    <div class="form-actions fluid">
                                        <div class="row">
                                            <div class="col-md-12">
                                                <div class="col-md-offset-3 col-md-3">
                                                    <button type="submit" class="btn green">
                                                        <i class="fa fa-check"></i>
                                                        	提交	
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
                <!-- END EXAMPLE TABLE PORTLET-->
            </div>
        </div>
        
        <!-- 下线 -->
        
        <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>
                            	填写下线配置
                            &nbsp;
                        </div>
                        <div class="tools">
                            <a href="javascript:;" class="collapse"></a>
                            <a href="javascript:;" class="remove"></a>
                        </div>
                    </div>
                    <div class="portlet-body">
                        <div class="form">
                            <form action="/manage/app/offLineHorizontalShard.do" method="post"
                                  class="form-horizontal form-bordered form-row-stripped"
                                  onsubmit="return checkOffLineInstanceParam();">
                                <div class="form-body">
                                    <div class="form-group">
                                        <label class="control-label col-md-3">
                                            	ip:<font color='red'>(*)</font>:
                                        </label>
                                        <div class="col-md-5">
                                            <input type="text" name="ip" id="dropIp" value="${dropIp}" class="form-control"/>
                                        </div>
                                    </div>
                                    
                                    <div class="form-group">
                                        <label class="control-label col-md-3">
                                            	port:<font color='red'>(*)</font>:
                                        </label>
                                        <div class="col-md-5">
                                            <input type="text" name="port" id="dropPort" value="${dropPort}" class="form-control"/>
                                        </div>
                                    </div>
                                    
                                    <input type="hidden" name="appId" value="${appAudit.appId}">
                                    <input type="hidden" name="appAuditId" value="${appAudit.id}">

                                    <div class="form-actions fluid">
                                        <div class="row">
                                            <div class="col-md-12">
                                                <div class="col-md-offset-3 col-md-3">
                                                    <button type="submit" class="btn green">
                                                        <i class="fa fa-check"></i>
                                                        	提交
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
       
       <!-- 节点迁移 -->
       <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>
                            	填写迁移配置
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
                            <form action="/manage/app/addNodeMigrateApply.do" method="post"
                                  class="form-horizontal form-bordered form-row-stripped"
                                  onsubmit="return checkNodeMigrateParam('migrateText','migrationInfo');">
                                <div class="form-body">
                                    <label class="control-label col-md-3">迁移信息:（多个迁移信息用换行分隔）</label>
									<div class="col-md-5">
											<textarea rows="10" name="migrateText" id="migrateText" placeholder="srcIp:port,dstIp:port,threadNum" class="form-control"></textarea>									
									</div>
                                    
                                    <input type="hidden" name="appId" value="${appAudit.appId}">
                                    <input type="hidden" name="appAuditId" value="${appAudit.id}">
                                    <input type="hidden" name="migrationInfo" id="migrationInfo" value="">

                                    <div class="form-actions fluid">
                                        <div class="row">
                                            <div class="col-md-12">
                                                <div class="col-md-offset-3 col-md-3">
                                                    <button type="submit" class="btn green">
                                                        <i class="fa fa-check"></i>
                                                        	提交	
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
                <!-- END EXAMPLE TABLE PORTLET-->
            </div>
        </div>
        
        
       <!-- 完成残留slot迁移 -->
       <div class="row">
            <div class="col-md-12">
                <div class="portlet box light-grey">
                    <div class="portlet-title">
                        <div class="caption">
                            <i class="fa fa-globe"></i>
                            	完成因异常中断的处于migrate状态的slots
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
                            <form action="/manage/app/finishSlotMigrate.do" method="post"
                                  class="form-horizontal form-bordered form-row-stripped"
                                  >
                                 
                                <div class="form-body">
                                
                                <div class="form-group">
                                        <label class="control-label col-md-3">
                                            	并发工作线程数目
                                        </label>
                                        <div class="col-md-5">
                                            <input type="text" name="threadNum" value="50" class="form-control"/>
                                        </div>
                                </div>
                                
                                	
                                    <input type="hidden" name="appId" value="${appAudit.appId}">
                                    <input type="hidden" name="appAuditId" value="${appAudit.id}">

                                    <div class="form-actions fluid">
                                        <div class="row">
                                            <div class="col-md-12">
                                                <div class="col-md-offset-3 col-md-3">
                                                    <button type="submit" class="btn green">
                                                        <i class="fa fa-check"></i>
                                                        	开始剩余迁移
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
                <!-- END EXAMPLE TABLE PORTLET-->
            </div>
        </div>
        
        
    </div>
</div>

