-- MySQL dump 10.13  Distrib 5.5.16, for Linux (x86_64)
--
-- Host: 10.10.19.167    Database: cache-cloud
-- ------------------------------------------------------
-- Server version	5.5.34-32.0-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `QRTZ_BLOB_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Trigger 作为 Blob 类型存储(用于 Quartz 用户用 JDBC 创建他们自己定制的 Trigger 类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CALENDARS`
--

DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) NOT NULL COMMENT 'scheduler名称',
  `CALENDAR_NAME` varchar(200) NOT NULL COMMENT 'calendar名称',
  `CALENDAR` blob NOT NULL COMMENT 'calendar信息',
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='以 Blob 类型存储 Quartz 的 Calendar 信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CRON_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL COMMENT 'scheduler名称',
  `TRIGGER_NAME` varchar(200) NOT NULL COMMENT 'trigger名',
  `TRIGGER_GROUP` varchar(200) NOT NULL COMMENT 'trigger组',
  `CRON_EXPRESSION` varchar(120) NOT NULL COMMENT 'cron表达式',
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储 Cron Trigger，包括 Cron 表达式和时区信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_FIRED_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL COMMENT '是否非并行执行',
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL COMMENT '是否持久化',
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储已触发的 Trigger相关的状态信息，以及关联 Job 的执行信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_JOB_DETAILS`
--

DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL COMMENT '是否持久化，0不持久化，1持久化',
  `IS_NONCONCURRENT` varchar(1) NOT NULL COMMENT '是否非并发，0非并发，1并发',
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL COMMENT '是否可恢复，0不恢复，1恢复',
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储每一个已配置的 Job 的详细信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_LOCKS`
--

DROP TABLE IF EXISTS `QRTZ_LOCKS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储程序的悲观锁的信息(假如使用了悲观锁)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_PAUSED_TRIGGER_GRPS`
--

DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储已暂停的 Trigger 组的信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SCHEDULER_STATE`
--

DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL COMMENT '执行quartz实例的主机名',
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL COMMENT '实例将状态报告给集群中的其它实例的上一次时间',
  `CHECKIN_INTERVAL` bigint(13) NOT NULL COMMENT '实例间状态报告的时间频率',
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储少量的有关 Scheduler 的状态信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPLE_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL COMMENT '重复次数',
  `REPEAT_INTERVAL` bigint(12) NOT NULL COMMENT '重复间隔',
  `TIMES_TRIGGERED` bigint(10) NOT NULL COMMENT '已出发次数',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储简单的 Trigger，包括重复次数，间隔，以及已触的次数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPROP_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int(11) DEFAULT NULL,
  `INT_PROP_2` int(11) DEFAULT NULL,
  `LONG_PROP_1` bigint(20) DEFAULT NULL,
  `LONG_PROP_2` bigint(20) DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='存储已配置的 Trigger 的信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_audit`
--

DROP TABLE IF EXISTS `app_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `user_id` bigint(20) NOT NULL COMMENT '申请人的id',
  `user_name` varchar(64) NOT NULL COMMENT '用户名',
  `type` tinyint(4) NOT NULL COMMENT '申请类型:0:申请应用,1:应用扩容,2:修改配置',
  `param1` varchar(600) DEFAULT NULL COMMENT '预留参数1',
  `param2` varchar(600) DEFAULT NULL COMMENT '预留参数2',
  `param3` varchar(600) DEFAULT NULL COMMENT '预留参数3',
  `info` varchar(360) NOT NULL COMMENT '申请描述',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0:等待审批; 1:审批通过; -1:驳回',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `refuse_reason` varchar(360) DEFAULT NULL COMMENT '驳回理由',
  PRIMARY KEY (`id`),
  KEY `idx_appid` (`app_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status_create_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用审核表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_audit_log`
--

DROP TABLE IF EXISTS `app_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `user_id` bigint(20) NOT NULL COMMENT '审批操作人id',
  `info` longtext NOT NULL COMMENT 'app审批的详细信息',
  `type` tinyint(4) NOT NULL,
  `create_time` datetime NOT NULL,
  `app_audit_id` bigint(20) NOT NULL COMMENT '审批id',
  PRIMARY KEY (`id`),
  KEY `idx_audit_appid` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='app审核日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_costtime_minute_stat`
--

DROP TABLE IF EXISTS `app_client_costtime_minute_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_costtime_minute_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip',
  `report_time` datetime NOT NULL COMMENT '客户端上报时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `command` varchar(20) NOT NULL COMMENT '命令',
  `mean` double NOT NULL COMMENT '耗时平均值',
  `median` int(11) NOT NULL COMMENT '耗时中值',
  `ninety_percent_max` int(11) NOT NULL COMMENT '耗时90%最大值',
  `ninety_nine_percent_max` int(11) NOT NULL COMMENT '耗时99%最大值',
  `hundred_max` int(11) NOT NULL COMMENT '耗时最大值',
  `count` int(11) NOT NULL COMMENT '调用次数',
  `instance_host` varchar(20) DEFAULT NULL COMMENT '客户端上报实例ip',
  `instance_port` int(11) DEFAULT NULL COMMENT '客户端上报实例port',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '实例id',
  PRIMARY KEY (`id`),
  KEY `idx_appid_collecttime` (`app_id`,`collect_time`),
  KEY `idx_collect_time` (`collect_time`),
  KEY `idx_group` (`app_id`,`instance_id`,`client_ip`,`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端每分钟耗时上报数据统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_costtime_minute_stat_total`
--

DROP TABLE IF EXISTS `app_client_costtime_minute_stat_total`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_costtime_minute_stat_total` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `command` varchar(20) NOT NULL COMMENT '命令',
  `mean` double NOT NULL COMMENT '耗时平均值',
  `median` int(11) NOT NULL COMMENT '耗时中值',
  `ninety_percent_max` int(11) NOT NULL COMMENT '耗时90%最大值',
  `ninety_nine_percent_max` int(11) NOT NULL COMMENT '耗时99%最大值',
  `hundred_max` int(11) NOT NULL COMMENT '耗时最大值',
  `total_cost` double NOT NULL COMMENT '总耗时',
  `total_count` int(11) NOT NULL COMMENT '调用次数',
  `max_instance_host` varchar(20) DEFAULT NULL COMMENT '客户端上报最大耗时对应的实例ip',
  `max_instance_port` int(11) DEFAULT NULL COMMENT '客户端上报最大耗时对应的实例port',
  `max_instance_id` bigint(20) DEFAULT NULL COMMENT '最大耗时对应的实例id',
  `max_client_ip` varchar(20) NOT NULL COMMENT '最大耗时对应的客户端ip',
  `accumulation` int(10) NOT NULL DEFAULT '0' COMMENT '参与累加实例数和客户端数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`,`command`,`collect_time`),
  KEY `idx_appid_collecttime` (`app_id`,`collect_time`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用全局耗时统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_datasize_minute_stat`
--

DROP TABLE IF EXISTS `app_client_datasize_minute_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_datasize_minute_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip',
  `report_time` datetime NOT NULL COMMENT '客户端上报时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `cost_map_size` varchar(20) NOT NULL COMMENT '耗时map尺寸',
  `value_map_size` double NOT NULL COMMENT '值map尺寸',
  `exception_map_size` int(11) NOT NULL COMMENT '异常map尺寸',
  `collect_map_size` int(11) NOT NULL COMMENT '耗时map尺寸',
  PRIMARY KEY (`id`),
  KEY `idx_client_ip` (`client_ip`),
  KEY `idx_collect_time_client_ip` (`collect_time`,`client_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端每分钟耗时上报收集数据的map尺寸';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_exception_minute_stat`
--

DROP TABLE IF EXISTS `app_client_exception_minute_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_exception_minute_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip',
  `report_time` datetime NOT NULL COMMENT '客户端上报时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `exception_class` varchar(255) NOT NULL COMMENT '异常类',
  `exception_count` varchar(255) NOT NULL COMMENT '异常个数',
  `instance_host` varchar(20) DEFAULT NULL COMMENT '实例ip',
  `instance_port` int(11) DEFAULT NULL COMMENT '实例port',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '实例id',
  `type` tinyint(4) DEFAULT '1' COMMENT '异常类型:1是jedis异常,2是客户端异常',
  PRIMARY KEY (`id`),
  KEY `idx_appid_collecttime` (`app_id`,`collect_time`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端每分钟异常上报数据统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_value_distri_minute_stat`
--

DROP TABLE IF EXISTS `app_client_value_distri_minute_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_value_distri_minute_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip',
  `report_time` datetime NOT NULL COMMENT '客户端上报时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `command` varchar(20) NOT NULL COMMENT '命令',
  `distribute_value` varchar(20) NOT NULL COMMENT '值分布',
  `distribute_type` tinyint(4) NOT NULL COMMENT '值分布类型',
  `count` int(11) NOT NULL COMMENT '调用次数',
  `instance_host` varchar(20) DEFAULT NULL COMMENT '实例ip',
  `instance_port` int(11) DEFAULT NULL COMMENT '实例port',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '实例id',
  PRIMARY KEY (`id`),
  KEY `idx_appid_collecttime` (`app_id`,`collect_time`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端每分钟值分布上报数据统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_client_version_statistic`
--

DROP TABLE IF EXISTS `app_client_version_statistic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_client_version_statistic` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip地址',
  `client_version` varchar(20) NOT NULL COMMENT '客户端版本',
  `report_time` datetime DEFAULT NULL COMMENT '上报时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_client_ip` (`app_id`,`client_ip`),
  KEY `app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端上报版本信息统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_desc`
--

DROP TABLE IF EXISTS `app_desc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_desc` (
  `app_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '应用id',
  `name` varchar(36) NOT NULL COMMENT '应用名',
  `business_group_id` bigint(20) NOT NULL COMMENT '所属业务组的id',
  `user_id` bigint(20) NOT NULL COMMENT '申请人id',
  `status` tinyint(4) NOT NULL COMMENT '应用状态, 0未分配，1申请未审批，2审批并发布 3:应用下线,4:驳回',
  `intro` varchar(255) NOT NULL COMMENT '应用描述',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `passed_time` datetime NOT NULL COMMENT '审批通过时间',
  `type` int(10) NOT NULL DEFAULT '0' COMMENT 'cache类型，2. redis-cluster,5. redis-sentinel ,6.redis-standalone ',
  `officer` varchar(20) NOT NULL COMMENT '负责人，中文',
  `ver_id` int(11) NOT NULL COMMENT '版本',
  `is_test` tinyint(4) DEFAULT '0' COMMENT '是否测试：1是0否',
  `need_persistence` tinyint(4) DEFAULT '1' COMMENT '是否需要持久化: 1是0否',
  `need_hot_back_up` tinyint(4) DEFAULT '1' COMMENT '是否需要热备: 1是0否',
  `has_back_store` tinyint(4) DEFAULT '1' COMMENT '是否有后端数据源: 1是0否',
  `forecase_qps` int(11) DEFAULT NULL COMMENT '预估qps',
  `forecast_obj_num` int(11) DEFAULT NULL COMMENT '预估条目数',
  `mem_alert_value` int(11) DEFAULT NULL COMMENT '内存报警阀值',
  `client_machine_room` varchar(36) DEFAULT NULL COMMENT '客户端机房信息',
  `template_id` int NOT NULL DEFAULT '0' COMMENT '模板id',
  `backup_days` int(11) DEFAULT '3' COMMENT '备份天数，0表示不备份',
  `open_alarm` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否开启告警 0.不开启 1.开启',
  `jcache_url` varchar(200) DEFAULT '' COMMENT '同步jcacheUrl,如果为空则不同步',
  PRIMARY KEY (`app_id`),
  UNIQUE KEY `uidx_app_name` (`name`),
  UNIQUE KEY `idx_app_uid` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8 COMMENT='app应用描述';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_hour_command_statistics`
--

DROP TABLE IF EXISTS `app_hour_command_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_hour_command_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHH',
  `command_name` varchar(60) NOT NULL COMMENT '命令名称',
  `command_count` bigint(20) NOT NULL COMMENT '命令执行次数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`,`command_name`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用的每小时命令统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_hour_statistics`
--

DROP TABLE IF EXISTS `app_hour_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_hour_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间:格式yyyyMMddHH',
  `hits` bigint(20) NOT NULL COMMENT '每小时命中数量和',
  `misses` bigint(20) NOT NULL COMMENT '每小时未命中数量和',
  `command_count` bigint(20) DEFAULT '0' COMMENT '命令总数',
  `used_memory` bigint(20) NOT NULL COMMENT '每小时内存占用最大值',
  `expired_keys` bigint(20) NOT NULL COMMENT '每小时过期key数量和',
  `evicted_keys` bigint(20) NOT NULL COMMENT '每小时驱逐key数量和',
  `net_input_byte` bigint(20) DEFAULT '0' COMMENT '网络输入字节',
  `net_output_byte` bigint(20) DEFAULT '0' COMMENT '网络输出字节',
  `connected_clients` int(10) NOT NULL COMMENT '每小时客户端连接数最大值',
  `object_size` bigint(20) NOT NULL COMMENT '每小时存储对象数最大值',
  `accumulation` int(10) NOT NULL DEFAULT '0' COMMENT '每小时参与累加实例数最小值',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '每小时修改时间最大值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用统计数据每小时统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_minute_command_statistics`
--

DROP TABLE IF EXISTS `app_minute_command_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_minute_command_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm',
  `command_name` varchar(60) NOT NULL COMMENT '命令名称',
  `command_count` bigint(20) NOT NULL COMMENT '命令执行次数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`,`collect_time`,`command_name`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用的每分钟命令统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_minute_statistics`
--

DROP TABLE IF EXISTS `app_minute_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_minute_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间:格式yyyyMMddHHmm',
  `hits` bigint(20) NOT NULL COMMENT '命中数量',
  `misses` bigint(20) NOT NULL COMMENT '未命中数量',
  `command_count` bigint(20) DEFAULT '0' COMMENT '命令总数',
  `used_memory` bigint(20) NOT NULL COMMENT '内存占用',
  `expired_keys` bigint(20) NOT NULL COMMENT '过期key数量',
  `evicted_keys` bigint(20) NOT NULL COMMENT '驱逐key数量',
  `net_input_byte` bigint(20) DEFAULT '0' COMMENT '网络输入字节',
  `net_output_byte` bigint(20) DEFAULT '0' COMMENT '网络输出字节',
  `connected_clients` int(10) NOT NULL COMMENT '客户端连接数',
  `object_size` bigint(20) NOT NULL COMMENT '每分钟存储对象数最大值',
  `accumulation` int(10) NOT NULL DEFAULT '0' COMMENT '参与累加实例数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_to_user`
--

DROP TABLE IF EXISTS `app_to_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_to_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  PRIMARY KEY (`id`),
  KEY `app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_user`
--

DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '用户名',
  `ch_name` varchar(255) NOT NULL COMMENT '中文名',
  `email` varchar(64) NOT NULL COMMENT '邮箱',
  `mobile` varchar(16) NOT NULL COMMENT '手机',
  `type` int(4) NOT NULL DEFAULT '2' COMMENT '0管理员，1预留，2普通用户，-1无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_user_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instance_fault`
--

DROP TABLE IF EXISTS `instance_fault`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_fault` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `inst_id` bigint(20) NOT NULL COMMENT '实例id',
  `ip` varchar(16) NOT NULL COMMENT 'ip地址',
  `port` int(11) NOT NULL COMMENT '端口',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态:0:心跳停止,1:心跳恢复',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `type` mediumint(4) NOT NULL COMMENT '类型：2. redis-cluster, 5. redis-sentinel 6.redis-standalone',
  `reason` mediumtext NOT NULL COMMENT '故障原因描述',
  PRIMARY KEY (`id`),
  KEY `idx_ip_port` (`ip`,`port`),
  KEY `app_id` (`app_id`),
  KEY `inst_id` (`inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='实例故障表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instance_host`
--

DROP TABLE IF EXISTS `instance_host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_host` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(16) NOT NULL COMMENT '机器ip',
  `ssh_user` varchar(32) DEFAULT NULL COMMENT 'ssh用户',
  `ssh_pwd` varchar(32) DEFAULT NULL COMMENT 'ssh密码',
  `warn` int(5) DEFAULT '1' COMMENT '0不报警，1报警',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_host_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='机器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instance_info`
--

DROP TABLE IF EXISTS `instance_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'instance id',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '对等实例的id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id，与app_desc关联',
  `host_id` bigint(20) NOT NULL COMMENT '对应的主机id，与instance_host关联',
  `ip` varchar(16) NOT NULL COMMENT '实例的ip',
  `port` int(11) NOT NULL COMMENT '实例端口',
  `status` tinyint(4) NOT NULL COMMENT '是否启用:0:节点异常,1:正常启用,2:节点下线',
  `mem` int(11) NOT NULL COMMENT '内存大小',
  `conn` int(11) NOT NULL COMMENT '连接数',
  `cmd` varchar(255) NOT NULL COMMENT '启动实例的命令/redis-sentinel的masterName',
  `type` mediumint(11) NOT NULL COMMENT '类型：2. redis-cluster, 5. redis-sentinel 6.redis-standalone',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_inst_ipport` (`ip`,`port`),
  KEY `app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='实例信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `instance_statistics`
--

DROP TABLE IF EXISTS `instance_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `inst_id` bigint(20) NOT NULL COMMENT '实例的id',
  `app_id` bigint(20) NOT NULL COMMENT 'app id',
  `host_id` bigint(20) NOT NULL COMMENT '机器的id',
  `ip` varchar(16) COLLATE utf8_bin NOT NULL COMMENT 'ip',
  `port` int(255) NOT NULL COMMENT 'port',
  `role` tinyint(255) NOT NULL COMMENT '主从，1主2从',
  `max_memory` bigint(255) NOT NULL COMMENT '预分配内存，单位byte',
  `used_memory` bigint(255) NOT NULL COMMENT '已使用内存，单位byte',
  `curr_items` bigint(255) NOT NULL COMMENT '当前item数量',
  `curr_connections` int(255) NOT NULL COMMENT '当前连接数',
  `misses` bigint(255) NOT NULL COMMENT 'miss数',
  `hits` bigint(255) NOT NULL COMMENT '命中数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ip` (`ip`,`port`),
  KEY `app_id` (`app_id`),
  KEY `machine_id` (`host_id`),
  KEY `idx_inst_id` (`inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='实例的最新统计信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `machine_info`
--

DROP TABLE IF EXISTS `machine_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `machine_info` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '机器的id',
  `ssh_user` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'cachecloud' COMMENT 'ssh用户',
  `ssh_passwd` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'cachecloud' COMMENT 'ssh密码',
  `ip` varchar(16) COLLATE utf8_bin NOT NULL COMMENT 'ip',
  `room` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '所属机房',
  `mem` int(11) unsigned NOT NULL COMMENT '内存大小，单位m',
  `cpu` mediumint(24) unsigned NOT NULL COMMENT 'cpu数量',
  `virtual` tinyint(8) unsigned NOT NULL DEFAULT '1' COMMENT '是否虚拟，0表示否，1表示是',
  `real_ip` varchar(16) COLLATE utf8_bin COMMENT '宿主机ip',
  `service_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上线时间',
  `fault_count` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '故障次数',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `warn` tinyint(255) unsigned NOT NULL DEFAULT '1' COMMENT '是否启用报警，0不启用，1启用',
  `available` tinyint(255) NOT NULL COMMENT '表示机器是否可用，1表示可用，0表示不可用；',
  `groupId` int(11) NOT NULL DEFAULT '0' COMMENT '机器分组，默认为0，表示原生资源，非0表示外部提供的资源(可扩展)',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '0原生 1 其他',
  `extra_desc` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '对于机器的额外说明(例如机器安装的其他服务(web,mysql,queue等等))',
  `installed` tinyint NOT NULL DEFAULT '0' COMMENT '0未安装  1已安装',
  `machine_groupId` int(11) NOT NULL DEFAULT '0' COMMENT '机器所属分组',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='保存机器的静态信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `machine_statistics`
--

DROP TABLE IF EXISTS `machine_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `machine_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) NOT NULL COMMENT '机器id',
  `ip` varchar(16) NOT NULL COMMENT '机器ip',
  `cpu_usage` varchar(120) NOT NULL COMMENT 'cpu使用率',
  `load` varchar(120) NOT NULL COMMENT '机器负载',
  `traffic` varchar(120) NOT NULL COMMENT 'io网络流量',
  `memory_usage_ratio` varchar(120) NOT NULL COMMENT '内存使用率',
  `memory_free` varchar(120) NOT NULL COMMENT '内存剩余',
  `memory_total` varchar(120) NOT NULL COMMENT '总内存量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_ip` (`ip`),
  KEY `host_id` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='机器状态统计信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_statistics`
--

DROP TABLE IF EXISTS `standard_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `standard_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间:格式yyyyMMddHHmm',
  `ip` varchar(16) NOT NULL COMMENT 'ip地址',
  `port` int(11) NOT NULL COMMENT '端口/hostId',
  `db_type` varchar(16) NOT NULL COMMENT '收集的数据类型',
  `info_json` text NOT NULL COMMENT '收集的json数据',
  `diff_json` text NOT NULL COMMENT '上一次收集差异的json数据',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_index` (`ip`,`port`,`db_type`,`collect_time`),
  KEY `idx_create_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

CREATE TABLE `instance_slow_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `instance_id` bigint(20) NOT NULL COMMENT '实例的id',
  `app_id` bigint(20) NOT NULL COMMENT 'app id',
  `ip` varchar(32) NOT NULL COMMENT 'ip',
  `port` int(11) NOT NULL COMMENT 'port',
  `slow_log_id` bigint(20) NOT NULL COMMENT '慢查询id',
  `cost_time` int(11) NOT NULL COMMENT '耗时(微妙)',
  `command` varchar(255) NOT NULL COMMENT '执行命令',
  `execute_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '执行时间点',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `slowlogkey` (`instance_id`,`slow_log_id`,`execute_time`),
  KEY `idx_app_create_time` (`app_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='实例慢查询列表';

CREATE TABLE `app_client_value_minute_stats` (
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm00',
  `update_time` datetime NOT NULL COMMENT '创建时间',
  `command` varchar(20) NOT NULL COMMENT '命令',
  `distribute_type` tinyint(4) NOT NULL COMMENT '值分布类型',
  `count` int(11) NOT NULL COMMENT '调用次数',
  PRIMARY KEY (`app_id`,`collect_time`,`command`,`distribute_type`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户端每分钟值分布上报数据统计';

CREATE TABLE `app_client_instance` (
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `client_ip` varchar(20) NOT NULL COMMENT '客户端ip',
  `instance_host` varchar(20) NOT NULL COMMENT 'redis节点ip',
  `instance_port` int(11) NOT NULL COMMENT 'redis节点端口',
  `instance_id` bigint(20) NOT NULL COMMENT 'redis节点id',
  `day` date NOT NULL COMMENT '日期',
  PRIMARY KEY (`app_id`,`day`,`client_ip`,`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用实例与客户端对应关系表';

CREATE TABLE `system_config` (
  `config_key` varchar(255) NOT NULL COMMENT '配置key',
  `config_value` varchar(512) NOT NULL COMMENT '配置value',
  `info` varchar(255) NOT NULL COMMENT '配置说明',
  `status` tinyint NOT NULL COMMENT '1:可用,0:不可用',
  `order_id` int NOT NULL COMMENT '顺序',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='系统配置';

--
-- init cachecloud data
--

insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.machine.ssh.name','xxxxxxx','机器ssh用户名',1,1);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.machine.ssh.password','xxxxxx','机器ssh密码',1,2);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.machine.ssh.port','22','机器ssh端口',1,3);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.superAdmin','xxxxx','超级管理员组',1,4);
insert into system_config(config_key,config_value,info,status,order_id) values('machine.cpu.alert.ratio','80.0','机器cpu报警阀值',1,7);
insert into system_config(config_key,config_value,info,status,order_id) values('machine.mem.alert.ratio','80.0','机器内存报警阀值',1,8);
insert into system_config(config_key,config_value,info,status,order_id) values('machine.load.alert.ratio','8.0','机器负载报警阀值',1,9);

insert into system_config(config_key,config_value,info,status,order_id) values('machine.disk.alert.ratio','80.0','磁盘报警阀值',1,6);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.documentUrl','http://cachecloud.github.io','文档地址',1,10);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.owner.email','xx@sohu.com,yy@qq.com','邮件报警(逗号隔开)',1,11);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.owner.phone','xxxxxxx','手机号报警(逗号隔开)',1,12);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.mavenWareHouse','http://your_maven_house','maven仓库地址(客户端)',1,13);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.contact','user1:(xx@zz.com, user1:135xxxxxxxx)<br/>user2: (user2@zz.com, user2:138xxxxxxxx)','值班联系人信息',1,14);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.good.client','1.0-SNAPSHOT','可用客户端版本(用逗号隔开)',1,15);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.warn.client','0.1','警告客户端版本(用逗号隔开)',1,16);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.error.client','0.0','不可用客户端版本(用逗号隔开)',1,17);

insert into system_config(config_key,config_value,info,status,order_id) values('redis.migrate.tool.home','/opt/redis-migrate-tool/','redis-migrate-tool安装路径',1,18);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.user.login.type','1','用户登录状态保存方式(session或cookie)',1,19);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.cookie.domain','','cookie登录方式所需要的域名',1,20);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.base.dir','/opt/redis','cachecloud根目录，要和cachecloud-init.sh脚本中的目录一致',1,21);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.app.client.conn.threshold','2000','应用连接数报警阀值',1,22);

insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.mobile.alert.interface','xxxxxx','短信报警接口(说明:http://cachecloud.github.io 邮件和短信报警接口规范)',1,24);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.ldap.url','','LDAP接口地址(例如:ldap://ldap.xx.com)',1,25);

insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.whether.schedule.clean.data','false','是否定期清理统计数据',1,26);

insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.app.secret.base.key','cachecloud-2014','appkey秘钥基准key',1,27);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.machine.stats.cron.minute','1','机器性能统计周期(分钟)',1,28);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.defaultIdcId','-1','默认机房id',1,29);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.defaultIdcName','','默认机房名称',1,30);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.aof.time','02:00-02:09','aof时间区间（在这个区间内，每5min，一次aof重写））',1,31);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.aof.disk.threshold','60','磁盘剩余空间比率（0-100）小于这个值，将触发0.6倍增长率的aof重写）',1,32);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.reshard.threadNum','50','cluster节点增加或减少时，并发迁移的线程数目',1,33);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.backup.backupDays','3','redis默认备份天数',1,34);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.backup.backupDir','/opt/redis/backup','redis备份目录',1,35);
INSERT INTO system_config(config_key,config_value,info,STATUS,order_id) values('collect.queue.alert.ratio','80.0','任务堆积告警阈值',1,37);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.alert.disable.time','','禁止短信告警的时间,空则禁用，格式举例02:00-02:09',1,36);
insert into system_config(config_key,config_value,info,status,order_id) values('jcache.node.sync','false','是否同步节点信息到统一缓存接口,true or false',1,38);
insert into system_config(config_key,config_value,info,status,order_id) values('sql.commit.limit','3000','单次提交sql commit记录数，控制监控性能',1,39);
insert into system_config(config_key,config_value,info,status,order_id) values('cachecloud.backup.check.time','14:00-14:39','备份完整性检查区间',1,40);



CREATE TABLE `app_data_migrate_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `migrate_machine_ip` varchar(255) NOT NULL COMMENT '迁移工具所在机器ip',
  `migrate_machine_port` int NOT NULL COMMENT '迁移工具所占port',
  `source_migrate_type` tinyint(4) NOT NULL COMMENT '源迁移类型,0:single,1:redis cluster,2:rdb file,3:twemproxy',
  `source_servers` varchar(2048) NOT NULL COMMENT '源实例列表',
  `target_migrate_type` tinyint(4) NOT NULL COMMENT '目标迁移类型,0:single,1:redis cluster,2:rdb file,3:twemproxy',
  `target_servers` varchar(2048) NOT NULL COMMENT '目标实例列表',
  `source_app_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '源应用id',
  `target_app_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '目标应用id',
  `user_id` bigint(20) NOT NULL COMMENT '操作人',
  `status` tinyint(4) NOT NULL COMMENT '迁移执行状态,0:开始,1:结束,2:异常',
  `start_time` datetime NOT NULL COMMENT '迁移开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '迁移结束执行时间',
  `log_path` varchar(255) NOT NULL COMMENT '日志文件路径',
  `config_path` varchar(255) NOT NULL COMMENT '配置文件路径',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='迁移状态';


CREATE TABLE `instance_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) NOT NULL COMMENT '配置名',
  `config_value` varchar(512) NOT NULL COMMENT '配置值',
  `info` varchar(512) NOT NULL COMMENT '配置说明',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `type` mediumint(9) NOT NULL COMMENT '类型：2.cluster节点特殊配置, 5:sentinel节点配置, 6:redis普通节点',
  status tinyint not null comment '1有效,0无效',
  `template_id` tinyint not null comment '对应模板配置的id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key_type` (`config_key`, `template_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-enabled','yes','是否开启集群模式',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-node-timeout','15000','集群节点超时时间,默认15秒',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-slave-validity-factor','10','从节点延迟有效性判断因子,默认10秒',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-migration-barrier','1','主从迁移至少需要的从节点数,默认1个',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-config-file','nodes-%d.conf','集群配置文件名称,格式:nodes-{port}.conf',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'cluster-require-full-coverage','no','节点部分失败期间,其他节点是否继续工作',now(),2,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'tcp-backlog','511','TCP连接完成队列',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'timeout','0','客户端闲置多少秒后关闭连接,默认为0,永不关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'tcp-keepalive','60','检测客户端是否健康周期,默认关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'loglevel','notice','日志级别',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'databases','16','可用的数据库数，默认值为16个,默认数据库为0',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'dir','%s','redis工作目录',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'stop-writes-on-bgsave-error','no','bgsave出错了不停写',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-timeout','60','master批量数据传输时间或者ping回复时间间隔,默认:60秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-ping-slave-period','10','指定slave定期ping master的周期,默认:10秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-disable-tcp-nodelay','no','是否禁用socket的NO_DELAY,默认关闭，影响主从延迟',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-backlog-size','10M','复制缓存区,默认:1mb,配置为:10Mb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-backlog-ttl','7200','master在没有Slave的情况下释放BACKLOG的时间多久:默认:3600,配置为:7200',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'slave-serve-stale-data','yes','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置“yes”，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'slave-read-only','yes','slave服务器节点是否只读,cluster的slave节点默认读写都不可用,需要调用readonly开启可读模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'slave-priority','100','slave的优先级,影响sentinel/cluster晋升master操作,0永远不晋升',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'lua-time-limit','5000','Lua脚本最长的执行时间，单位为毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'slowlog-log-slower-than','10000','慢查询被记录的阀值,默认10毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'slowlog-max-len','128','最多记录慢查询的条数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'hash-max-ziplist-entries','512','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'hash-max-ziplist-value','64','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'list-max-ziplist-entries','512','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'list-max-ziplist-value','64','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'set-max-intset-entries','512','set数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'zset-max-ziplist-entries','128','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'zset-max-ziplist-value','64','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'activerehashing','yes','是否激活重置哈希,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'client-output-buffer-limit normal','0 0 0','客户端输出缓冲区限制(客户端)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'client-output-buffer-limit slave','512mb 128mb 60','客户端输出缓冲区限制(复制)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'client-output-buffer-limit pubsub','32mb 8mb 60','客户端输出缓冲区限制(发布订阅)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'hz','10','执行后台task数量,默认:10',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'port','%d','端口',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'maxmemory','%dmb','当前实例最大可用内存',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'maxmemory-policy','volatile-lru','内存不够时,淘汰策略,默认:volatile-lru',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'appendonly','yes','开启append only持久化模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'appendfsync','everysec','默认:aof每秒同步一次',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'appendfilename','appendonly-%d.aof','aof文件名称,默认:appendonly-{port}.aof',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'dbfilename','dump-%d.rdb','RDB文件默认名称,默认dump-{port}.rdb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'aof-rewrite-incremental-fsync','yes','aof rewrite过程中,是否采取增量文件同步策略,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'no-appendfsync-on-rewrite','yes','是否在后台aof文件rewrite期间调用fsync,默认调用,修改为yes,防止可能fsync阻塞,但可能丢失rewrite期间的数据',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'auto-aof-rewrite-min-size','64m','触发rewrite的aof文件最小阀值,默认64m',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'auto-aof-rewrite-percentage','0','Redis重写aof文件的比例条件,默认从100开始,统一机器下不同实例按4%递减',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'rdbcompression','yes','rdb是否压缩',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'rdbchecksum','yes','rdb校验和',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-diskless-sync','no','开启无盘复制',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'repl-diskless-sync-delay','5','无盘复制延时',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'save 900','1','900秒有一次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'save 300','10','300秒有10次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'save 60','10000','60秒有10000次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'maxclients','40960','客户端最大连接数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'hll-sparse-max-bytes','3000','HyperLogLog稀疏表示限制设置	',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'min-slaves-to-write','0','当slave数量小于min-slaves-to-write，且延迟小于等于min-slaves-max-lag时， master停止写入操作',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'min-slaves-max-lag','10','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置yes，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'aof-load-truncated','yes','加载aof文件时，是否忽略aof文件不完整的情况，是否Redis正常启动',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'notify-keyspace-events','','keyspace事件通知功能',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'protected-mode','no','保护模式下，redis只能本机访问，默认不开启',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(1,'bind','0.0.0.0','绑定的业务监听ip',now(),6,1);


insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'port','%d','sentinel实例端口',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'dir','%s','工作目录',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'sentinel monitor','%s %s %d 1','master名称定义和最少参与监控的sentinel数,格式:masterName ip port num',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'sentinel down-after-milliseconds','%s 20000','Sentinel判定服务器断线的毫秒数',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'sentinel failover-timeout','%s 180000','故障迁移超时时间,默认:3分钟',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'sentinel parallel-syncs','%s 1','在执行故障转移时,最多有多少个从服务器同时对新的主服务器进行同步,默认:1',now(),5,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'tcp-backlog','511','TCP连接完成队列',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'timeout','0','客户端闲置多少秒后关闭连接,默认为0,永不关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'tcp-keepalive','60','检测客户端是否健康周期,默认关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'loglevel','notice','日志级别',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'databases','16','可用的数据库数，默认值为16个,默认数据库为0',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'dir','%s','redis工作目录',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'stop-writes-on-bgsave-error','no','bgsave出错了不停写',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-timeout','60','master批量数据传输时间或者ping回复时间间隔,默认:60秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-ping-slave-period','10','指定slave定期ping master的周期,默认:10秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-disable-tcp-nodelay','no','是否禁用socket的NO_DELAY,默认关闭，影响主从延迟',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-backlog-size','10M','复制缓存区,默认:1mb,配置为:10Mb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-backlog-ttl','7200','master在没有Slave的情况下释放BACKLOG的时间多久:默认:3600,配置为:7200',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'slave-serve-stale-data','yes','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置“yes”，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'slave-read-only','yes','slave服务器节点是否只读,cluster的slave节点默认读写都不可用,需要调用readonly开启可读模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'slave-priority','100','slave的优先级,影响sentinel/cluster晋升master操作,0永远不晋升',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'lua-time-limit','5000','Lua脚本最长的执行时间，单位为毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'slowlog-log-slower-than','10000','慢查询被记录的阀值,默认10毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'slowlog-max-len','128','最多记录慢查询的条数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'hash-max-ziplist-entries','512','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'hash-max-ziplist-value','64','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'list-max-ziplist-entries','512','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'list-max-ziplist-value','64','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'set-max-intset-entries','512','set数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'zset-max-ziplist-entries','128','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'zset-max-ziplist-value','64','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'activerehashing','yes','是否激活重置哈希,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'client-output-buffer-limit normal','0 0 0','客户端输出缓冲区限制(客户端)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'client-output-buffer-limit slave','512mb 128mb 60','客户端输出缓冲区限制(复制)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'client-output-buffer-limit pubsub','32mb 8mb 60','客户端输出缓冲区限制(发布订阅)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'hz','10','执行后台task数量,默认:10',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'port','%d','端口',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'maxmemory','%dmb','当前实例最大可用内存',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'maxmemory-policy','volatile-lru','内存不够时,淘汰策略,默认:volatile-lru',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'appendonly','yes','开启append only持久化模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'appendfsync','everysec','默认:aof每秒同步一次',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'appendfilename','appendonly-%d.aof','aof文件名称,默认:appendonly-{port}.aof',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'dbfilename','dump-%d.rdb','RDB文件默认名称,默认dump-{port}.rdb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'aof-rewrite-incremental-fsync','yes','aof rewrite过程中,是否采取增量文件同步策略,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'no-appendfsync-on-rewrite','yes','是否在后台aof文件rewrite期间调用fsync,默认调用,修改为yes,防止可能fsync阻塞,但可能丢失rewrite期间的数据',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'auto-aof-rewrite-min-size','64m','触发rewrite的aof文件最小阀值,默认64m',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'auto-aof-rewrite-percentage','0','Redis重写aof文件的比例条件,默认从100开始,统一机器下不同实例按4%递减',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'rdbcompression','yes','rdb是否压缩',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'rdbchecksum','yes','rdb校验和',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-diskless-sync','no','开启无盘复制',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'repl-diskless-sync-delay','5','无盘复制延时',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'save 900','1','900秒有一次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'save 300','10','300秒有10次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'save 60','10000','60秒有10000次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'maxclients','40960','客户端最大连接数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'hll-sparse-max-bytes','3000','HyperLogLog稀疏表示限制设置	',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'min-slaves-to-write','0','当slave数量小于min-slaves-to-write，且延迟小于等于min-slaves-max-lag时， master停止写入操作',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'min-slaves-max-lag','10','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置yes，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'aof-load-truncated','yes','加载aof文件时，是否忽略aof文件不完整的情况，是否Redis正常启动',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'notify-keyspace-events','','keyspace事件通知功能',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'protected-mode','no','保护模式下，redis只能本机访问，默认不开启',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(2,'bind','0.0.0.0','绑定的业务监听ip',now(),6,1);

insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'tcp-backlog','511','TCP连接完成队列',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'timeout','0','客户端闲置多少秒后关闭连接,默认为0,永不关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'tcp-keepalive','60','检测客户端是否健康周期,默认关闭',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'loglevel','notice','日志级别',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'databases','16','可用的数据库数，默认值为16个,默认数据库为0',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'dir','%s','redis工作目录',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'stop-writes-on-bgsave-error','no','bgsave出错了不停写',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-timeout','60','master批量数据传输时间或者ping回复时间间隔,默认:60秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-ping-slave-period','10','指定slave定期ping master的周期,默认:10秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-disable-tcp-nodelay','no','是否禁用socket的NO_DELAY,默认关闭，影响主从延迟',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-backlog-size','10M','复制缓存区,默认:1mb,配置为:10Mb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-backlog-ttl','7200','master在没有Slave的情况下释放BACKLOG的时间多久:默认:3600,配置为:7200',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'slave-serve-stale-data','yes','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置“yes”，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'slave-read-only','yes','slave服务器节点是否只读,cluster的slave节点默认读写都不可用,需要调用readonly开启可读模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'slave-priority','100','slave的优先级,影响sentinel/cluster晋升master操作,0永远不晋升',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'lua-time-limit','5000','Lua脚本最长的执行时间，单位为毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'slowlog-log-slower-than','10000','慢查询被记录的阀值,默认10毫秒',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'slowlog-max-len','128','最多记录慢查询的条数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'hash-max-ziplist-entries','512','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'hash-max-ziplist-value','64','hash数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'list-max-ziplist-entries','512','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'list-max-ziplist-value','64','list数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'set-max-intset-entries','512','set数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'zset-max-ziplist-entries','128','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'zset-max-ziplist-value','64','zset数据结构优化参数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'activerehashing','yes','是否激活重置哈希,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'client-output-buffer-limit normal','0 0 0','客户端输出缓冲区限制(客户端)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'client-output-buffer-limit slave','512mb 128mb 60','客户端输出缓冲区限制(复制)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'client-output-buffer-limit pubsub','32mb 8mb 60','客户端输出缓冲区限制(发布订阅)',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'hz','10','执行后台task数量,默认:10',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'port','%d','端口',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'maxmemory','%dmb','当前实例最大可用内存',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'maxmemory-policy','volatile-lru','内存不够时,淘汰策略,默认:volatile-lru',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'appendonly','yes','开启append only持久化模式',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'appendfsync','everysec','默认:aof每秒同步一次',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'appendfilename','appendonly-%d.aof','aof文件名称,默认:appendonly-{port}.aof',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'dbfilename','dump-%d.rdb','RDB文件默认名称,默认dump-{port}.rdb',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'aof-rewrite-incremental-fsync','yes','aof rewrite过程中,是否采取增量文件同步策略,默认:yes',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'no-appendfsync-on-rewrite','yes','是否在后台aof文件rewrite期间调用fsync,默认调用,修改为yes,防止可能fsync阻塞,但可能丢失rewrite期间的数据',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'auto-aof-rewrite-min-size','64m','触发rewrite的aof文件最小阀值,默认64m',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'auto-aof-rewrite-percentage','0','Redis重写aof文件的比例条件,默认从100开始,统一机器下不同实例按4%递减',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'rdbcompression','yes','rdb是否压缩',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'rdbchecksum','yes','rdb校验和',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-diskless-sync','no','开启无盘复制',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'repl-diskless-sync-delay','5','无盘复制延时',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'save 900','1','900秒有一次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'save 300','10','300秒有10次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'save 60','10000','60秒有10000次修改做bgsave',now(),6,0);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'maxclients','40960','客户端最大连接数',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'hll-sparse-max-bytes','3000','HyperLogLog稀疏表示限制设置	',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'min-slaves-to-write','0','当slave数量小于min-slaves-to-write，且延迟小于等于min-slaves-max-lag时， master停止写入操作',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'min-slaves-max-lag','10','当slave服务器和master服务器失去连接后，或者当数据正在复制传输的时候，如果此参数值设置yes，slave服务器可以继续接受客户端的请求',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'aof-load-truncated','yes','加载aof文件时，是否忽略aof文件不完整的情况，是否Redis正常启动',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'notify-keyspace-events','','keyspace事件通知功能',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'protected-mode','no','保护模式下，redis只能本机访问，默认不开启',now(),6,1);
insert into instance_config(template_id, config_key,config_value,info,update_time, type, status) values(3,'bind','0.0.0.0','绑定的业务监听ip',now(),6,1);

-- change appdesc add 秘钥和客户端连接数报警
alter table app_desc add column client_conn_alert_value int(11) DEFAULT 2000 COMMENT '客户端连接报警阀值';
alter table app_desc add column app_key varchar(255) DEFAULT NULL COMMENT '应用秘钥';
alter table app_desc add password varchar(200) null comment '密码';

alter table instance_statistics add column mem_fragmentation_ratio double default 0 COMMENT '碎片率';
alter table instance_statistics add column aof_delayed_fsync int default 0 COMMENT 'aof阻塞次数';

/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-01-13  9:28:31

-- add column for the switch of server status collection
ALTER TABLE `machine_info` ADD COLUMN `collect` int DEFAULT 1 COMMENT 'switch of collect server status, 1:open, 0:close';

-- add server status table
DROP TABLE IF EXISTS `server`;
CREATE TABLE `server` (
  `ip` varchar(16) NOT NULL COMMENT 'ip',
  `host` varchar(255) DEFAULT NULL COMMENT 'host',
  `nmon` varchar(255) DEFAULT NULL COMMENT 'nmon version',
  `cpus` tinyint(4) DEFAULT NULL COMMENT 'logic cpu num',
  `cpu_model` varchar(255) DEFAULT NULL COMMENT 'cpu 型号',
  `dist` varchar(255) DEFAULT NULL COMMENT '发行版信息',
  `kernel` varchar(255) DEFAULT NULL COMMENT '内核信息',
  `ulimit` varchar(255) DEFAULT NULL COMMENT 'ulimit -n,ulimit -u',
  `updatetime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `server_stat`;
CREATE TABLE `server_stat` (
  `ip` varchar(16) NOT NULL COMMENT 'ip',
  `cdate` date NOT NULL COMMENT '数据收集天',
  `ctime` char(4) NOT NULL COMMENT '数据收集小时分钟',
  `cuser` float DEFAULT NULL COMMENT '用户态占比',
  `csys` float DEFAULT NULL COMMENT '内核态占比',
  `cwio` float DEFAULT NULL COMMENT 'wio占比',
  `c_ext` text COMMENT '子cpu占比',
  `cload1` float DEFAULT NULL COMMENT '1分钟load',
  `cload5` float DEFAULT NULL COMMENT '5分钟load',
  `cload15` float DEFAULT NULL COMMENT '15分钟load',
  `mtotal` float DEFAULT NULL COMMENT '总内存,单位M',
  `mfree` float DEFAULT NULL COMMENT '空闲内存',
  `mcache` float DEFAULT NULL COMMENT 'cache',
  `mbuffer` float DEFAULT NULL COMMENT 'buffer',
  `mswap` float DEFAULT NULL COMMENT 'cache',
  `mswap_free` float DEFAULT NULL COMMENT 'cache',
  `nin` float DEFAULT NULL COMMENT '网络入流量 单位K/s',
  `nout` float DEFAULT NULL COMMENT '网络出流量 单位k/s',
  `nin_ext` text COMMENT '各网卡入流量详情',
  `nout_ext` text COMMENT '各网卡出流量详情',
  `tuse` int(11) DEFAULT NULL COMMENT 'tcp estab连接数',
  `torphan` int(11) DEFAULT NULL COMMENT 'tcp orphan连接数',
  `twait` int(11) DEFAULT NULL COMMENT 'tcp time wait连接数',
  `dread` float DEFAULT NULL COMMENT '磁盘读速率 单位K/s',
  `dwrite` float DEFAULT NULL COMMENT '磁盘写速率 单位K/s',
  `diops` float DEFAULT NULL COMMENT '磁盘io速率 交互次数/s',
  `dbusy` float DEFAULT NULL COMMENT '磁盘io带宽使用百分比',
  `d_ext` text COMMENT '磁盘各分区占比',
  `dspace` text COMMENT '磁盘各分区空间使用率',
  PRIMARY KEY (`ip`,`cdate`,`ctime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `idc`;
CREATE TABLE `idc` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `status` char(1) DEFAULT 'Y',
  `wlanip` varchar(50) DEFAULT NULL,
  `domain` varchar(50) DEFAULT NULL,
  `sshport` varchar(10) DEFAULT NULL,
  `sshcmd` varchar(30) DEFAULT NULL,
  `route` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `business_group`;
CREATE TABLE `business_group` (
  `business_group_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_group_name` varchar(20) NOT NULL,
  PRIMARY KEY (`business_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `config_template`;
CREATE TABLE `config_template` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(40) NOT NULL,
  `architecture` tinyint NOT NULL COMMENT '类型：2.cluster节点特殊配置, 5:sentinel节点配置, 6:redis普通节点',
  `extra_desc` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '对于配置模板的额外说明',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/* 初始化模板 */
insert into config_template(`id`,`name`, `architecture`, `extra_desc`) values(1,'cluster默认模板', 2, 'cluster默认配置模板');
insert into config_template(`id`,`name`, `architecture`, `extra_desc`) values(2,'sentinel默认模板', 5, 'sentinel默认配置模板');
insert into config_template(`id`,`name`, `architecture`, `extra_desc`) values(3,'standalone默认模板', 6, 'standalone默认配置模板');

INSERT INTO `idc` VALUES (25,'广州南基','-','Y','183.232.25','','-','-',NULL),(26,'北京数北','','Y','119.90.34','','','',''),(27,'南基BGP','','Y','58.67.203','','','',''),(28,'北京兆维','','Y','113.31.17','','','',''),(29,'北京雍和宫','','Y','111.13.48','','','',''),(30,'北京苏州桥','','Y','118.145.3','','','',''),(31,'深圳观澜','','Y','114.119.7','','','',''),(32,'青云/亚马逊','','Y','119.254/121.20/207.226','','','',''),(33,'北京富丰','','Y','113.31.136','','','','BGP'),(37,'广州奥飞BGP','','Y','','','','','BGP'),(39,'北京睿江云平台','','Y','','','','','BGP');


/*分组每分钟数据统计*/
DROP TABLE IF EXISTS `group_minute_statistics`;
CREATE TABLE `group_minute_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `business_group_id` bigint(20) NOT NULL COMMENT '业务组id',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间:格式yyyyMMddHHmm',
  `hits` bigint(20) NOT NULL COMMENT '命中数量',
  `misses` bigint(20) NOT NULL COMMENT '未命中数量',
  `command_count` bigint(20) DEFAULT '0' COMMENT '命令总数',
  `used_memory` bigint(20) NOT NULL COMMENT '内存占用',
  `expired_keys` bigint(20) NOT NULL COMMENT '过期key数量',
  `evicted_keys` bigint(20) NOT NULL COMMENT '驱逐key数量',
  `net_input_byte` bigint(20) DEFAULT '0' COMMENT '网络输入字节',
  `net_output_byte` bigint(20) DEFAULT '0' COMMENT '网络输出字节',
  `connected_clients` int(10) NOT NULL COMMENT '客户端连接数',
  `object_size` bigint(20) NOT NULL COMMENT '每分钟存储对象数最大值',
  `accumulation` int(10) NOT NULL DEFAULT '0' COMMENT '参与累加实例数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `business_group_id` (`business_group_id`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

/*分组每小时数据统计*/
DROP TABLE IF EXISTS `group_hour_statistics`;
CREATE TABLE `group_hour_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `business_group_id` bigint(20) NOT NULL COMMENT '业务组id',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间:格式yyyyMMddHH',
  `hits` bigint(20) NOT NULL COMMENT '每小时命中数量和',
  `misses` bigint(20) NOT NULL COMMENT '每小时未命中数量和',
  `command_count` bigint(20) DEFAULT '0' COMMENT '命令总数',
  `used_memory` bigint(20) NOT NULL COMMENT '每小时内存占用最大值',
  `expired_keys` bigint(20) NOT NULL COMMENT '每小时过期key数量和',
  `evicted_keys` bigint(20) NOT NULL COMMENT '每小时驱逐key数量和',
  `net_input_byte` bigint(20) DEFAULT '0' COMMENT '网络输入字节',
  `net_output_byte` bigint(20) DEFAULT '0' COMMENT '网络输出字节',
  `connected_clients` int(10) NOT NULL COMMENT '每小时客户端连接数最大值',
  `object_size` bigint(20) NOT NULL COMMENT '每小时存储对象数最大值',
  `accumulation` int(10) NOT NULL DEFAULT '0' COMMENT '每小时参与累加实例数最小值',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '每小时修改时间最大值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`business_group_id`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组统计数据每小时统计';


DROP TABLE IF EXISTS `group_minute_command_statistics`;
CREATE TABLE `group_minute_command_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `business_group_id` bigint(20) NOT NULL COMMENT '业务组id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHHmm',
  `command_name` varchar(60) NOT NULL COMMENT '命令名称',
  `command_count` bigint(20) NOT NULL COMMENT '命令执行次数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`business_group_id`,`collect_time`,`command_name`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组的每分钟命令统计';


DROP TABLE IF EXISTS `group_hour_command_statistics`;
CREATE TABLE `group_hour_command_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `business_group_id` bigint(20) NOT NULL COMMENT '业务组id',
  `collect_time` bigint(20) NOT NULL COMMENT '统计时间:格式yyyyMMddHH',
  `command_name` varchar(60) NOT NULL COMMENT '命令名称',
  `command_count` bigint(20) NOT NULL COMMENT '命令执行次数',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`business_group_id`,`command_name`,`collect_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_modify_time` (`modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='分组的每小时命令统计';

DROP TABLE IF EXISTS `machine_group`;
CREATE TABLE `machine_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '组ID',
  `name` varchar(225) NOT NULL COMMENT '组名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into machine_group values(null,'未分组'); update machine_group set id=0 where id=1;

DROP TABLE IF EXISTS `quene_size_statistics`;
CREATE TABLE `quene_size_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `quene_size` int(11) NOT NULL DEFAULT '0' COMMENT '当前剩余任务数',
  `collect_time` bigint(20) NOT NULL COMMENT '收集时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5460 DEFAULT CHARSET=utf8;

