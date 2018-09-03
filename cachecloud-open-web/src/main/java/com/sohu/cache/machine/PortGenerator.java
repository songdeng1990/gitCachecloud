package com.sohu.cache.machine;

import com.sohu.cache.constant.EmptyObjectConstant;
import com.sohu.cache.constant.SymbolConstant;
import com.sohu.cache.exception.SSHException;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.ConstUtils;
import com.google.common.util.concurrent.AtomicLongMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 生成一个redis可用端口
 *
 * @author: lingguo
 * @time: 2014/8/25 20:57
 */
public class PortGenerator {
    private static Logger logger = LoggerFactory.getLogger(PortGenerator.class);
    /**
     * redis port常量
     */
    private static final Integer REDIS_START_PORT = 16380;
    private static AtomicLongMap<String> redisPortHolder = AtomicLongMap.create();

    /**
     * 返回一个redis的可用端口：
     *  - 1. 通过shell查询redis当前已用的最大port；
     *  - 2. 为什么同步：防止多线程访问时获取到同样的端口；
     *  - 3. 为什么还用原子计数：连续两次调用时，如果进程还没启动，则拿到的仍然是相同的端口；
     *
     * @param ip
     * @return
     */
    public static synchronized Integer getRedisPort(final String ip) {
        if (redisPortHolder.get(ip) == 0L) {
            redisPortHolder.put(ip, REDIS_START_PORT);
        }
        String maxPortStr = "";
        try {
            int sshPort = SSHUtil.getSshPort(ip);
            maxPortStr = getMaxPortStr(ip, sshPort);
        } catch (SSHException e) {
            logger.error("cannot get max port of redis by ssh, ip: {}", ip, e);
        }

        if (StringUtils.isBlank(maxPortStr) || !StringUtils.isNumeric(maxPortStr)) {
            logger.warn("the max port of redis is invalid, maxPortStr: {}", maxPortStr);
            return new Long(redisPortHolder.getAndIncrement(ip)).intValue();
        }

        int availablePort = Integer.valueOf(maxPortStr) + 1;
        // 兼容连续调用的情况
        if (availablePort < redisPortHolder.get(ip)) {
            availablePort = new Long(redisPortHolder.getAndIncrement(ip)).intValue();
        } else {    // 正常情况，以及兼容系统重启和当前端口不可用的情形
            redisPortHolder.put(ip, availablePort + 1);
        }

        try {
            while (SSHUtil.isPortUsed(ip, availablePort)) {
                availablePort++;
            }
        } catch (SSHException e) {
            logger.error("check port error, ip: {}, port: {}", ip, availablePort, e);
        }
        redisPortHolder.put(ip, availablePort+1);
        return availablePort;
    }

    @Deprecated
    public static String getMaxPortStrOld(String ip, int sshPort) throws SSHException {
        String redisPidCmd = "ps -ef | grep redis | grep -v 'grep' |  awk -F '*:' '{print $2}' " +
                " | awk -F ' ' '{print $1}' | sort -r | head -1";
        return SSHUtil.execute(ip, sshPort, ConstUtils.USERNAME, ConstUtils.PASSWORD, redisPidCmd);
    }
    
    /**
     * 直接解析ps -ef | grep redis | grep -v 'grep'
     * @param ip
     * @param sshPort
     * @return
     * @throws SSHException
     */
     public static String getMaxPortStr(String ip, int sshPort) throws SSHException {
    	String confDir = ConstUtils.CACHECLOUD_BASE_DIR + "/conf/";
        String redisMaxPortCmd = "ls " + confDir + "*.conf | awk -F'-' '{print $3}' | awk -F'.' '{print $1}' | sort | tail -1";
        String redisMaxPortStr = "";
        try{
        	redisMaxPortStr = SSHUtil.execute(ip, sshPort, ConstUtils.USERNAME, ConstUtils.PASSWORD, redisMaxPortCmd);
        }catch (Exception e){
        	logger.error("",e);
        }
        
        
        return redisMaxPortStr;
    }
     
     public static void setRedisMaxPort(String ip,int port){
    	 redisPortHolder.put(ip, port);
     }

}
