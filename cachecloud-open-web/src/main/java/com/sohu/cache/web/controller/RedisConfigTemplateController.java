package com.sohu.cache.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.entity.ConfigTemplate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.ErrorMessageEnum;
import com.sohu.cache.constant.RedisConfigTemplateChangeEnum;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceConfig;
import com.sohu.cache.redis.RedisConfigTemplateService;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.AppEmailUtil;

/**
 * Redis配置模板管理
 * 
 * @author leifu
 * @Date 2016-6-25
 * @Time 下午2:48:25
 */
@Controller
@RequestMapping("manage/redisConfig")
public class RedisConfigTemplateController extends BaseController {

    @Resource(name = "redisConfigTemplateService")
    private RedisConfigTemplateService redisConfigTemplateService;

    @Resource(name = "appEmailUtil")
    private AppEmailUtil appEmailUtil;

    private static final int DEFAULT_TEMPLATE_ID = 3;
    private static final int DEFAULT_ARCHITECTURE = 6;

    /**
     * 初始化配置
     */
    @RequestMapping(value = "/init")
    public ModelAndView init(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 默认是Redis普通节点配置
        int architecture = NumberUtils.toInt(request.getParameter("type"), DEFAULT_ARCHITECTURE);
        int templateId = NumberUtils.toInt(request.getParameter("templateId"), DEFAULT_TEMPLATE_ID);
        model.addAttribute("redisConfigList", redisConfigTemplateService.getByTemplateId(templateId));
        model.addAttribute("success", request.getParameter("success"));
        model.addAttribute("redisConfigActive", SuccessEnum.SUCCESS.value());
        model.addAttribute("type", architecture);
        model.addAttribute("templateId", templateId);
        model.addAttribute("templateInfo", redisConfigTemplateService.getTemplateById(templateId));
        model.addAttribute("templateList", redisConfigTemplateService.getTemplateByArchitecture(architecture));
        model.addAttribute("defaultList", redisConfigTemplateService.getTemplateByArchitecture(DEFAULT_ARCHITECTURE));
        return new ModelAndView("manage/redisConfig/init");
    }

    /**
     * 修改配置
     */
    @RequestMapping(value = "/update")
    public ModelAndView update(HttpServletRequest request, HttpServletResponse response, Model model) {
        AppUser appUser = getUserInfo(request);
        String id = request.getParameter("id");
        String configKey = request.getParameter("configKey");
        String configValue = request.getParameter("configValue");
        String info = request.getParameter("info");
        int status = NumberUtils.toInt(request.getParameter("status"), -1);
        if (StringUtils.isBlank(id) || !NumberUtils.isDigits(id) || StringUtils.isBlank(configKey) || status > 1
                || status < 0) {
            model.addAttribute("status", SuccessEnum.FAIL.value());
            model.addAttribute("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "id=" + id + ",configKey="
                    + configKey + ",configValue=" + configValue + ",status=" + status);
            return new ModelAndView("");
        }
        //开始修改
        logger.warn("user {} want to change id={}'s configKey={}, configValue={}, info={}, status={}", appUser.getName(),
                id, configKey, configValue, info, status);
        SuccessEnum successEnum;
        InstanceConfig instanceConfig = redisConfigTemplateService.getById(NumberUtils.toLong(id));
        try {
            instanceConfig.setConfigValue(configValue);
            instanceConfig.setInfo(info);
            instanceConfig.setStatus(status);
            redisConfigTemplateService.saveOrUpdate(instanceConfig);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            model.addAttribute("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to change id={}'s configKey={}, configValue={}, info={}, status={}, result is {}", appUser.getName(),
                id, configKey, configValue, info, status, successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.UPDATE);
        model.addAttribute("status", successEnum.value());
        return new ModelAndView("");
    }

    /**
     * 删除配置
     */
    @RequestMapping(value = "/remove")
    public ModelAndView remove(HttpServletRequest request, HttpServletResponse response, Model model) {
        AppUser appUser = getUserInfo(request);
        String idParam = request.getParameter("id");
        long id = NumberUtils.toLong(idParam);
        if (id <= 0) {
            model.addAttribute("status", SuccessEnum.FAIL.value());
            model.addAttribute("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "id=" + idParam);
            return new ModelAndView("");
        }
        logger.warn("user {} want to delete id={}'s config", appUser.getName(), id);
        SuccessEnum successEnum;
        InstanceConfig instanceConfig = redisConfigTemplateService.getById(id);
        try {
            redisConfigTemplateService.remove(id);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            model.addAttribute("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to delete id={}'s config, result is {}", appUser.getName(), id, successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.DELETE);
        model.addAttribute("status", successEnum.value());
        return new ModelAndView("");

    }

    /**
     * 添加配置
     */
    @RequestMapping(value = "/add")
    public ModelAndView add(HttpServletRequest request, HttpServletResponse response, Model model) {
        AppUser appUser = getUserInfo(request);
        InstanceConfig instanceConfig = getInstanceConfig(request);
        if (StringUtils.isBlank(instanceConfig.getConfigKey())) {
            model.addAttribute("status", SuccessEnum.FAIL.value());
            model.addAttribute("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "configKey=" + instanceConfig.getConfigKey());
            return new ModelAndView("");
        }
        logger.warn("user {} want to add config, configKey is {}, configValue is {}, type is {}", appUser.getName(), instanceConfig.getConfigKey(), instanceConfig.getType());
        SuccessEnum successEnum;
        try {
            redisConfigTemplateService.saveOrUpdate(instanceConfig);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            model.addAttribute("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to add config, configKey is {}, configValue is {}, type is {}, result is {}", appUser.getName(),
                instanceConfig.getConfigKey(), instanceConfig.getConfigValue(), instanceConfig.getType(), successEnum.value());
        model.addAttribute("status", successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.ADD);
        return new ModelAndView("");

    }

    /**
     * 预览配置
     */
    @RequestMapping(value = "/preview")
    public ModelAndView preview(HttpServletRequest request, HttpServletResponse response, Model model) {
        //默认配置
        int type = NumberUtils.toInt(request.getParameter("type"), -1);
        String host = StringUtils.isBlank(request.getParameter("host")) ? "127.0.0.1" : request.getParameter("host");
        int port = NumberUtils.toInt(request.getParameter("port"), 6379);
        int maxMemory = NumberUtils.toInt(request.getParameter("maxMemory"), 2048);
        int sentinelPort = NumberUtils.toInt(request.getParameter("sentinelPort"), 26379);
        int templateId = NumberUtils.toInt(request.getParameter("templateId"), 0);
        String masterName = StringUtils.isBlank(request.getParameter("masterName")) ? "myMaster" : request
                .getParameter("masterName");
        int quorum = NumberUtils.toInt(request.getParameter("quorum"), 2);

        // 根据类型生成配置模板
        List<String> configList =  redisConfigTemplateService.getByTemplateIdAndFilterUneffectiveConfig(templateId);

        //if (ConstUtils.CACHE_REDIS_STANDALONE == type) {
        //    configList = redisConfigTemplateService.handleConfig(port, maxMemory, templateId);
        //} else if (ConstUtils.CACHE_REDIS_SENTINEL == type) {
        //    configList = redisConfigTemplateService.handleSentinelConfig(masterName, host, port, sentinelPort, templateId);
        //} else if (ConstUtils.CACHE_TYPE_REDIS_CLUSTER == type) {
        //    configList = redisConfigTemplateService.handleClusterConfig(port, templateId);
        //}

        model.addAttribute("type", type);
        model.addAttribute("host", host);
        model.addAttribute("port", port);
        model.addAttribute("maxMemory", maxMemory);
        model.addAttribute("sentinelPort", sentinelPort);
        model.addAttribute("masterName", masterName);
        model.addAttribute("configList", configList);
        return new ModelAndView("manage/redisConfig/preview");
    }

    /**
     * 使用最简单的request生成InstanceConfig对象
     * 
     * @return
     */
    private InstanceConfig getInstanceConfig(HttpServletRequest request) {
        String configKey = request.getParameter("configKey");
        String configValue = request.getParameter("configValue");
        String info = request.getParameter("info");
        String type = request.getParameter("type");
        String templateId = request.getParameter("templateId");
        InstanceConfig instanceConfig = new InstanceConfig();
        instanceConfig.setConfigKey(configKey);
        instanceConfig.setConfigValue(configValue);
        instanceConfig.setInfo(info);
        instanceConfig.setType(NumberUtils.toInt(type));
        instanceConfig.setTemplateId(NumberUtils.toInt(templateId));
        instanceConfig.setStatus(1);
        return instanceConfig;
    }

    /**
     * 根据架构类型获取模板
     */
    @RequestMapping(value = "/getTemplateByArchitecture")
    public ModelAndView getTemplateByArchitecture(HttpServletRequest request, HttpServletResponse response, Model model) {
        int architecture = NumberUtils.toInt(request.getParameter("type"));
        model.addAttribute("templateList", redisConfigTemplateService.getTemplateByArchitecture(architecture));
        return new ModelAndView("");
    }

    /**
     * 根据id获取模板
     */
    @RequestMapping(value = "/getTemplateById")
    public ModelAndView getTemplateById(HttpServletRequest request, HttpServletResponse response, Model model) {
        int id = NumberUtils.toInt(request.getParameter("id"));
        model.addAttribute("desc", (redisConfigTemplateService.getTemplateById(id)).getExtraDesc());
        return new ModelAndView("");
    }

    /**
     * 添加配置模板
     */
    @RequestMapping(value = "/addTemplate")
    public ModelAndView addTemplate(HttpServletRequest request, HttpServletResponse response, Model model) {
        String templateName = request.getParameter("templateName");
        int architecture = NumberUtils.toInt(request.getParameter("redisType"));
        int copyTemplate = NumberUtils.toInt(request.getParameter("copyTemplate"));
        String extraDesc = request.getParameter("extraDesc");
        SuccessEnum successEnum;
        try {
            ConfigTemplate configTemplate = new ConfigTemplate();
            configTemplate.setName(templateName);
            configTemplate.setArchitecture(architecture);
            configTemplate.setExtraDesc(extraDesc);
            redisConfigTemplateService.addTemplate(configTemplate);
            int templateId = configTemplate.getId();
            if(copyTemplate != 0) {
                List<InstanceConfig> configs =  redisConfigTemplateService.getByTemplateId(copyTemplate);
                for(InstanceConfig config : configs) {
                    config.setTemplateId(templateId);
                    redisConfigTemplateService.saveOrUpdate(config);
                }
            }
            model.addAttribute("templateId", templateId);
            model.addAttribute("type", architecture);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            logger.error(e.getMessage(), e);
        }
        model.addAttribute("status", successEnum.value());
        return new ModelAndView("");
    }

    @RequestMapping(value = "/modifyTemplateInfo")
    public ModelAndView modifyTemplateInfo(HttpServletRequest request, HttpServletResponse response, Model model) {
        String templateName = request.getParameter("templateName");
        int architecture = NumberUtils.toInt(request.getParameter("type"), ConstUtils.CACHE_REDIS_STANDALONE);
        int templateId = NumberUtils.toInt(request.getParameter("id"));
        String extraDesc = request.getParameter("extraDesc");
        redisConfigTemplateService.updateTemplateInfo(templateId, templateName, extraDesc);
        model.addAttribute("redisConfigList", redisConfigTemplateService.getByTemplateId(templateId));
        model.addAttribute("success", request.getParameter("success"));
        model.addAttribute("redisConfigActive", SuccessEnum.SUCCESS.value());
        model.addAttribute("type", architecture);
        model.addAttribute("templateId", templateId);
        model.addAttribute("templateInfo", redisConfigTemplateService.getTemplateById(templateId));
        model.addAttribute("templateList", redisConfigTemplateService.getTemplateByArchitecture(architecture));
        return new ModelAndView("manage/redisConfig/init");
    }

    @RequestMapping(value = "/removeTemplate")
    public ModelAndView removeTemplate(HttpServletRequest request, HttpServletResponse response, Model model) {
        int templateId = NumberUtils.toInt(request.getParameter("id"));
        SuccessEnum successEnum;
        try {
            redisConfigTemplateService.removeByTemplateId(templateId);
            successEnum = SuccessEnum.SUCCESS;
        } catch(Exception e) {
            logger.error("remove template fail, " + e.getMessage(), e);
            successEnum = SuccessEnum.FAIL;
        }
        model.addAttribute("status", successEnum.value());
        return new ModelAndView("");
    }
}
