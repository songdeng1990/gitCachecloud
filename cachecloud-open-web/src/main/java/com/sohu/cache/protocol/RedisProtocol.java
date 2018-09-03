package com.sohu.cache.protocol;

import java.util.Date;

import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.util.DateUtil;

/**
 * Created by yijunzhang on 14-11-26.
 */
public class RedisProtocol {

    private static final String RUN_SHELL = MachineProtocol.BIN_DIR + "redis-server %s > " + MachineProtocol.LOG_DIR + "redis-%d-%s.log 2>&1 &";

    private static final String SENTINEL_SHELL = MachineProtocol.BIN_DIR + "redis-server %s --sentinel > " + MachineProtocol.LOG_DIR + "redis-sentinel-%d-%s.log 2>&1 &";

    private static final String CLUSTER_CONFIG = "redis-cluster-%d.conf";

    private static final String COMMON_CONFIG = "redis-sentinel-%d.conf";

    private static final String EXECUTE_COMMAND = MachineProtocol.BIN_DIR + "redis-cli -h %s -p %s --raw %s";

    public static String getRunShell(int port, boolean isCluster) {
        return String.format(RUN_SHELL, MachineProtocol.CONF_DIR + getConfig(port, isCluster), port, DateUtil.formatYYYYMMddHHMM(new Date()));
    }
    
    /**
     * 供每个机器上的重启脚本，重启redis时执行的命令
     * @param port
     * @param isCluster
     * @return
     */
    public static String getRestartRunShell(int port, boolean isCluster) {
        return String.format(RUN_SHELL, MachineProtocol.CONF_DIR + getConfig(port, isCluster), port, "`date +%Y%m%d%H%M`");
    } 

    public static String getSentinelShell(int port) {
        return String.format(SENTINEL_SHELL, MachineProtocol.CONF_DIR + getConfig(port, false), port, DateUtil.formatYYYYMMddHHMM(new Date()));
    }

    public static String getExecuteCommandShell(String host, int port, String command) {
        return String.format(EXECUTE_COMMAND, host, port, command);
    }

    public static String getConfig(int port, boolean isCluster) {
        if (isCluster) {
            return String.format(CLUSTER_CONFIG, port);
        } else {
            return String.format(COMMON_CONFIG, port);
        }
    }

}
