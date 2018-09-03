package com.sohu.cache.protocol;

import com.sohu.cache.util.ConstUtils;

/**
 * 机器相关的一些常量
 *
 * @author: lingguo
 * @time: 2014/8/26 16:18
 */
public class MachineProtocol {
    
    /**
     * 统一的目录结构
     */
	public static final String BIN_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/bin/";
    public static final String CONF_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/conf/";
    public static final String DATA_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/data";
    public static final String LOG_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/logs/";
    public static final String MONITOR_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/monitor/";
    
    public static final String MONITOR_BIN_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/monitor/bin/";
    public static final String MONITOR_CONF_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/monitor/conf/";
    public static final String MONITOR_LOG_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/monitor/log/";
    public static final String MONITOR_TMP_DIR = ConstUtils.CACHECLOUD_BASE_DIR + "/monitor/tmp/";
    /**
     * 用于监控发现的当前机器上redis实例的文件。内容格式是，应用名，端口，配置文件，启动命令,ip
     */
    public static final String DISCOVERY_FILE = "redis_discovery.txt";

    /**
     * 配置文件的临时目录；
     */
    public static final String TMP_DIR = "/tmp/cachecloud/";

    /**
     * 编码
     */
    public static final String ENCODING_UTF8 = "UTF-8";

}
