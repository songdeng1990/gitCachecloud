<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div id="addMachineModal${machine.info.id}" class="modal fade" tabindex="-1" data-width="400">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true"></button>
				<h4 class="modal-title">管理机器</h4>
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
										机器ip:
									</label>
									<div class="col-md-5">
										<input type="text" name="ip" id="ip${machine.info.id}"
											value="${machine.info.ip}" placeholder="机器ip"
											class="form-control" />
									</div>
                                    <div>
                                        <button type="button" id="getMachineBtn${machine.info.id}" class="btn red" onclick="getMachineInfo('${machine.info.id}')">获取机器信息</button>
                                    </div>
								</div>

                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        机房:
                                    </label>
                                    <div class="col-md-5">
                                        <select name="room" id="room${machine.info.id}" class="form-control select2_category">
                                            <c:forEach var="room" items="${roomList}">
                                                <c:choose>
                                                    <c:when test="${machine.info.room == room.id}">
                                                        <option value="${room.id}" selected="selected">${room.name}</option>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <option value="${room.id}">${room.name}</option>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        内存（单位M）:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="mem" id="mem${machine.info.id}"
                                               value="${machine.info.mem}" placeholder="机器内存"
                                               class="form-control" />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        cpu:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="cpu" id="cpu${machine.info.id}"
                                               value="${machine.info.cpu}" placeholder="机器CPU核数"
                                               class="form-control" />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        是否虚机:
                                    </label>
                                    <div class="col-md-5">
                                        <select name="virtual" id="virtual${machine.info.id}" class="form-control select2_category">
                                            <option value="0" <c:if test="${machine.info.virtual == 0}">selected="selected"</c:if>>
                                                否
                                            </option>
                                            <option value="1" <c:if test="${machine.info.virtual == 1}">selected="selected"</c:if>>
                                                是
                                            </option>
                                        </select>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        宿主机ip（虚机需要填写）:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="realIp" id="realIp${machine.info.id}"
                                               value="${machine.info.realIp}" placeholder="宿主机ip（虚机需要填写）"
                                               class="form-control" />
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                        	机器类型:
                                    </label>
                                    <div class="col-md-5">
                                        <select name="machineType" id="machineType${machine.info.id}" class="form-control select2_category">
                                            <option value="0" <c:if test="${machine.info.type == 0}">selected="selected"</c:if>>
                                                	Redis机器(默认)
                                            </option>
                                            <option value="2" <c:if test="${machine.info.type == 2}">selected="selected"</c:if>>
                                                	Redis迁移工具机器
                                            </option>
                                        </select>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                      	  额外说明:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="extraDesc" id="extraDesc${machine.info.id}"
                                               value="${machine.info.extraDesc}" placeholder="额外说明(可以不填)"
                                               class="form-control" />
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                      	  ssh用户:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="sshUser" id="sshUser${machine.info.id}"
                                               value="${machine.info.sshUser}" placeholder="ssh登陆用户"
                                               class="form-control" />
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="control-label col-md-3">
                                      	  ssh密码:
                                    </label>
                                    <div class="col-md-5">
                                        <input type="text" name="sshPasswd" id="sshPasswd${machine.info.id}"
                                               value="${machine.info.sshPasswd}" placeholder="ssh登陆密码"
                                               class="form-control" />
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                        <label class="control-label col-md-3">
                                            选择分组:
                                        </label>
                                        <div class="col-md-5">
                                            <select name="machinegroupIdSingle" id="machinegroupIdSingle${machine.info.id}" class="form-control select2_category">
                                                <c:forEach var="group" items="${machineGroup}">
                                                 <c:choose>
                                                    <c:when test="${machine.info.groupName == group}">
                                                        <option value="${group}" selected="selected">${group}</option>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <option value="${group}">${group}</option>
                                                    </c:otherwise>
                                                </c:choose>
                                                </c:forEach>
                                            </select>
                                        </div>
                                    </div>
                                

								<input type="hidden" id="machineId${machine.info.id}" name="machineId" value="${machine.info.id}"/>
							</div>
							<!-- form-body 结束 -->
						</div>
						<div id="machineInfo${machine.info.id}"></div>
						<!-- 控件结束 -->
					</div>
				</div>
				
				<div class="modal-footer">
					<button type="button" data-dismiss="modal" class="btn" >Close</button>
					<button type="button" id="addMachineBtn${machine.info.id}" class="btn red" onclick="saveOrUpdateMachine('${machine.info.id}')">Ok</button>
				</div>
			
			</form>
		</div>
	</div>
</div>

<div id="addBatchMachineModal" class="modal fade" tabindex="-1" data-width="400">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true"></button>
                <h4 class="modal-title">批量添加</h4>
            </div>

            <form class="form-horizontal form-bordered form-row-stripped">
                <div class="modal-body">
                    <div class="row">
                        <!-- 控件开始 -->
                        <div class="col-md-12">
                            <!-- form-body开始 -->
                            <div class="form-body">
                                <div class="form-group">
                                    <div class="col-md-12">
                                        <textarea rows="15" name="machineInfoText" id="machineInfoText" placeholder="机器信息" class="form-control"></textarea>
										<span class="help-block">
											具体规则:ip:额外描述<br/>
											例如：<br/>
                                            192.168.5.43:测试机<br/>
                                            192.168.5.44:<br/>
                                            192.168.5.45:cachecloud机器
										</span>
                                    </div>

                                    <div class="form-group">
                                        <label class="control-label col-md-3">
                                            选择分组:
                                        </label>
                                        <div class="col-md-5">
                                            <select name="machinegroupId" id="machinegroupId" class="form-control select2_category">
                                                <c:forEach var="group" items="${machineGroup}">
                                                    <option value="${group}">${group}</option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <!-- form-body 结束 -->
                        </div>
                        <div id="addBatchResult"></div>
                        <!-- 控件结束 -->
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" data-dismiss="modal" class="btn" >Close</button>
                    <button type="button" id="addBatchMachineBtn" class="btn red" onclick="addBatchMachine()">Ok</button>
                </div>

            </form>
        </div>
    </div>
</div>
