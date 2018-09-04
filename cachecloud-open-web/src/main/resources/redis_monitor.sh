#!/bin/sh

###################################################
#
#	功能描述：
#    1、监控redis，收集redis数据上报到zabbix中心。
#    2、监测redis运行状态，如果没有运行则自动重启。
#
###################################################

#设置命令执行路径。该脚本，要在cron运行，设置path是绝对必要的，否则cron运行时会出现找不到部分命令的情况
export PATH=$PATH:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin

redis_home=/opt/redis
redis_logs=${redis_home}/logs
redis_monitor=${redis_home}/monitor
redis_client=${redis_home}/bin/redis-cli

#检查redis日志时会产生一些临时文件，都放此临时目录
redis_monitor_tmp=${redis_monitor}/tmp
redis_monitor_log=${redis_monitor}/log
redis_monitor_bin=${redis_monitor}/bin
redis_monitor_conf=${redis_monitor}/conf

#本文件所在路径
redis_monitor_script=${redis_monitor_bin}/redis_monitor.sh

#该文件由cachecloud中心远程写入，记录正在运行的redis信息，格式为：应用名，端口，配置文件，启动命令，ip
redis_discovery_file=${redis_monitor_conf}/redis_discovery.txt

#告警短信只有故障发生时发一次，故障解除时发一次，为了避免大量重复发送告警，所以用一个文件来保存告警类型和告警状态的信息。
alarm_status_file=${redis_monitor_log}/alarm_status.log
alarm_log_file=${redis_monitor_log}/alarm.log

#检测redis实例产生的tmp文件
test_tmp_file=${redis_monitor_tmp}/redis_function_test.tmp

#变量赋值
#zabbix_center=`awk -F= '/^Server=/ {print $2}' /usr/local/zabbix/etc/zabbix_agentd.conf`
zabbix_center='10.9.100.123'
zabbix_sender_exe=/usr/local/zabbix/bin/zabbix_sender
zabbix_report_log_file=${redis_monitor_log}/zabbix_report.log


log_file_size_limit=10455040


#根据同步文件，获取本机ip
f_get_ip_inner()
{
	while read instance_info
	do
		ip_inner=`echo $instance_info | awk -F, '{print $5}' | sed 's/\s//g'`
		break
	done < $redis_discovery_file
}

f_rotate_log_file()
{
	log_file=$1
	if [ -f ${log_file} ]; then
      log_file_size_current=`ls -l ${log_file}|awk '{print $5}'`
      if [ ${log_file_size_current} -gt ${log_file_size_limit} ]; then
        >${log_file}
      fi
    fi   
}

#发送短信
f_send_mobile_msg()
{
 	f_rotate_log_file $alarm_log_file
    echo "`date` $1" >> $alarm_log_file	
#	return 1
    if [ $# -ne 1 ]; then
       echo "输入参数不对，请输入要发送的信息内容."   
    else
       msgtime="`date +%Y%m%d' '%H:%M:%S`"
       content="${msgtime} $1"   
       if [ -z "$content" ];then
          echo "要发的短信内容为空"
          exit 1;
       fi
      
       telmsg="redis监控: ${content}"       
       errinfostr="${telmsg}"
       SENDSTR="curl -X POST -H \"Content-Type: application/json\" -d '{\"code\":26, \"desc\": \"${errinfostr}\"}' http://alert.xxxx.com/v1/alert/"
       eval "${SENDSTR}"
       rstcode=$?
       echo "##`date` 上报告警中心返回值 ${rstcode} ##"
    fi
}


f_alarm_record()
{
    #1,redis 端口没有启动。2，redis端口已启动但是业务不正常。
    alarm_string=$1
    
    #0，正常状态；1，problem状态
    alarm_status=$2
    
    redis_ip=$3
    redis_port=$4
    alarm_detail=$5    
    alarm_prefix=$redis_ip-$redis_port-$alarm_string
    
    if [ ! -f $alarm_status_file ];then
    	touch $alarm_status_file
    fi
    
    alarm_line=`grep "$alarm_prefix" $alarm_status_file`
    if [ -z "$alarm_line" -a "$alarm_status" -ne 0 ];then
        echo "$alarm_prefix $alarm_status" >> $alarm_status_file
        f_send_mobile_msg "$alarm_detail 状态:故障正在进行"
    elif [ -n "$alarm_line" -a "$alarm_status" -eq 0 ];then
        sed -i "/$alarm_prefix/d" $alarm_status_file
        f_send_mobile_msg "$alarm_detail 状态:故障已解决"     
    fi
    
}



#使用zabbix_sender发送给zabbix中心，同时记录发送日志，每10mb，日志清空一次
f_send_to_zabbix()
{
    key=$1
    value=$2
   
    f_rotate_log_file $zabbix_report_log_file
    
    echo "############`date +%Y%m%d" "%X`############################"  >> $zabbix_report_log_file
    [ "z${value}" = "z" ] && value="OK"
    echo "${zabbix_sender_exe} -vv -z ${zabbix_center} -s $ip_inner -k $key -o $value" >> $zabbix_report_log_file
    ${zabbix_sender_exe} -vv -z ${zabbix_center} -s "$ip_inner" -k "$key" -o "$value"  2>>$zabbix_report_log_file 1>>$zabbix_report_log_file   
    
    echo "######################`date +%Y%m%d" "%X`##END################" >> $zabbix_report_log_file
}

f_string_match()
{
	string=$1
	pattern=$2
	echo $string | grep -E "$pattern" > /dev/null 2>&1
	return $?
}

##检测判断redis是否正常,0正常，1异常
f_check_redis_function()
{
    redis_ip="$1"
    redis_port="$2"
    redis_pwd="$3"
    rtimeout=5
    result=""    
    
    if [ -z "${redis_pwd}" ];then
      result=`${redis_client} -h ${redis_ip} -p ${redis_port} del impossiple_exist_212_@_yunwei_check`
      role=`${redis_client} -h ${redis_ip} -p ${redis_port} info replication | grep "role:" | awk -F: '{print $2}' | sed 's/\s//g'`
    else
      result=`${redis_client} -h ${redis_ip} -p ${redis_port} -a "${redis_pwd}" del impossiple_exist_212_@_yunwei_check `     
      role=`${redis_client} -h ${redis_ip} -p ${redis_port} -a "${redis_pwd}" info replication | grep "role:" | awk -F: '{print $2}' | sed 's/\s//g'`
    fi     
    
    #-是特殊字符，必须过滤掉
    result=`echo $result|sed 's/\s//g'|sed 's/+OK//g'|sed 's/-//g'`
    
    if [ `f_string_match "$result" "MOVED*";echo $?` -eq 0 ];then    
     		echo 0
    elif [ `f_string_match "$result" "ASK*";echo $?` -eq 0 ];then
    		echo 0
    elif [ "$role" = "slave" -a `f_string_match "$result" "READONLY*";echo $?` -eq 0 ];then
    		echo 0
    elif [ "$result" = "0" ];then
     		echo 0
    else
    		echo 1
    fi
}

#监测异常日志
#监测两种异常日志关键字
#1、内存不能分配 Cannot allocate memory
#2、aof的fsync缓慢 Asynchronous AOF fsync is taking too long
f_check_redislog()
{
    mkdir -p ${redis_monitor_tmp}
    OIFS=$IFS
    IFS=$'\n'
    for instance_info in `cat ${redis_discovery_file}`
    do
        app_name=`echo $instance_info | awk -F, '{print $1}'`
        redis_port=`echo $instance_info | awk -F, '{print $2}'`
        
        redis_log_file=`ls -t ${redis_logs}/redis-${redis_port}-* | head -1`
        
        if [ ! -e "$redis_log_file" ];then
        	continue
        fi
                
        file_name=`basename ${redis_log_file}`
        redis_log_file_old=${redis_monitor_tmp}/${file_name}.old
        redis_log_file_diff=${redis_monitor_tmp}/${file_name}.diff        
        
        
        #生成diff文件，diff文件保存日志文件最近的更新内容
        if [ ! -f "${redis_log_file_old}" ];then
            cp ${redis_log_file} ${redis_log_file_old}
            cp ${redis_log_file} ${redis_log_file_diff}
        else
            current_size=`du -b ${redis_log_file} | awk '{print $1}'`
            old_size=`du -b ${redis_log_file_old} | awk '{print $1}'`
            if [ "$current_size" -eq "$old_size" ];then
                rm -f ${redis_log_file_diff}
                echo "${redis_log_file},nothing changed."
                continue
            else
                comm -13 --nocheck-order ${redis_log_file_old} ${redis_log_file} > ${redis_log_file_diff}
                cp -f ${redis_log_file} ${redis_log_file_old}                
            fi             
        fi
        
        #在diff文件中，搜索日志关键字
        
        #搜索内存不足关键字
        nomemory_num=`egrep "Cannot allocate memory" ${redis_log_file_diff} | wc -l`    
        if [ ${nomemory_num} -gt 0 ]; then
            errorinfo="${errorinfo}|app $app_name redis ${ip_inner}:${redis_port} 有异常日志：Cannot allocate memory共${nomemory_num}条"
        fi           
        
        #搜索error缓慢关键字
        fsync_slow_num=`egrep -i "error" ${redis_log_file_diff} | wc -l`
        if [ ${fsync_slow_num} -gt 0 ]; then
            errorinfo="${errorinfo}|app $app_name redis ${ip_inner}:${redis_port} 有异常日志：error 共${fsync_slow_num}条"
        fi

        #fsync_slow_num=`egrep "Asynchronous AOF fsync is taking too long" ${redis_log_file_diff} | wc -l`
        #if [ ${fsync_slow_num} -gt 0 ]; then
        #    errorinfo="${errorinfo}|app $app_name redis ${ip_inner}:${redis_port} 有异常日志：Asynchronous AOF fsync is taking too long共${fsync_slow_num}条"
        #fi 
        rm -f ${redis_log_file_diff}
    done 
    IFS=$OIFS    
    
    if [ -n "$errorinfo" ];then
        #发送错误日志给zabbix    
        f_send_to_zabbix "redis.error.info" "$errorinfo"
    else
	f_send_to_zabbix "redis.error.info" "ok"
        echo "no redis log error on this machine."
    fi
 
}

#检查端口是否正常监听
f_check_port()
{
	port=$1
    
    netstat -ln | grep ":$port" > /dev/null 2>&1
    #if [ -z "$pwd" ];then
    #	$redis_client -p $port -h $ip ping > /dev/null 2>&1
    #else
    #	#实际上这里不用password，结果也是0，因为redis-cli只有在端口不存在时，才返回0
    # 	$redis_client -p $port -h $ip -a "$pwd" ping > /dev/null 2>&1
    #fi
   
    echo $?
}

#监测redis 端口是否异常
#如果端口正常监听，再检查redis业务是否正常。业务不正常则发送告警
#如果端口没有监听，再重启redis。同时发送重启告警。
f_check_and_restart()
{
	OIFS=$IFS
    IFS=$'\n'
    for instance_info in `cat ${redis_discovery_file}`    
    do
        app_name=`echo $instance_info | awk -F, '{print $1}'`
        redis_port=`echo $instance_info | awk -F, '{print $2}'`
        redis_conf=`echo $instance_info | awk -F, '{print $3}'`
        start_redis_command=`echo $instance_info | awk -F, '{print $4}'`
        redis_pwd=`egrep "^requirepass" ${redis_conf}|awk '{print $2}'|sed 's/\"//g'`
        
        is_port_ok=`f_check_port ${redis_port}`       
      
        if [ ${is_port_ok} -eq 0 ];then            
            
            f_alarm_record "port_error" "${is_port_ok}" "$ip_inner" "$redis_port" "应用名:${app_name} redis ${ip_inner}:${redis_port} 端口未监听。"
            
            is_function_ok=`f_check_redis_function ${ip_inner} ${redis_port} ${redis_pwd}`
                        
            f_alarm_record "function_error" "$is_function_ok" "$ip_inner" "$redis_port" "应用名:${app_name} redis ${ip_inner}:${redis_port} 端口正常监听，但是业务不正常。"
        else
            if [ -f $redis_conf ];then
                eval "$start_redis_command"
                f_alarm_record "port_error" "${is_port_ok}" "$ip_inner" "$redis_port" "应用名:${app_name} redis ${ip_inner}:${redis_port} 端口未监听。尝试自动重启。"
            else
                f_alarm_record "port_error" "${is_port_ok}" "$ip_inner" "$redis_port" "应用名:${app_name} redis ${ip_inner}:${redis_port} 端口未监听。非标准安装的redis,没有自动重启。"
            fi            
        fi        
    done 
    IFS=$OIFS    
}

install()
{
	mkdir -p ${redis_monitor_log}
	mkdir -p ${redis_monitor_tmp}
    mycron_tmp=/tmp/mycron.tmp
    crontab -l | grep "${redis_monitor_script} execute"
    if [ $? -eq 1 ];then
        crontab -l > ${mycron_tmp}
        echo "*/1 * * * * ${redis_monitor_script} execute" >> ${mycron_tmp}
        crontab ${mycron_tmp}
        rm -f ${mycron_tmp}
    fi
}

main()
{
    case $1 in
        install)
            install
        ;;
        execute)
        	f_get_ip_inner
            f_check_redislog
            f_check_and_restart    
        ;;
        *)
            echo "Usage: ./redis_monitor {install|execute}"
    esac
}

main $@