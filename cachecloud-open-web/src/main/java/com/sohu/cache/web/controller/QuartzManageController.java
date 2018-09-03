package com.sohu.cache.web.controller;

import com.sohu.cache.entity.TriggerInfo;
import com.sohu.cache.qrtzdao.QuartzDao;
import com.sohu.cache.schedule.SchedulerCenter;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * quartz管理test
 * 
 * @author leifu
 * @Time 2014年7月4日
 */
@Controller
@RequestMapping("manage/quartz")
public class QuartzManageController extends BaseController {

    @Resource
    private SchedulerCenter schedulerCenter;

    @Resource
    private QuartzDao quartzDao;

    @RequestMapping(value = "/list")
    public ModelAndView doQuartzList(HttpServletRequest request,
                                     HttpServletResponse response, Model model) {
        String query = request.getParameter("query");
        int pageNo = NumberUtils.toInt(request.getParameter("pageNo"), 1);
        int pageSize = NumberUtils.toInt(request.getParameter("pageSize"), 50);
        List<TriggerInfo> triggerList;
        if (StringUtils.isBlank(query)) {
            query = ".";
        }
        triggerList = schedulerCenter.searchTriggers(query,(pageNo - 1)*pageSize,pageSize);
        model.addAttribute("triggerList", triggerList);
        model.addAttribute("quartzActive", SuccessEnum.SUCCESS.value());
        model.addAttribute("query", query.equals(".")? "":query);
        request.getSession().setAttribute("triggerList",triggerList);
        Page page = new Page(pageNo,pageSize,quartzDao.count(query));
        model.addAttribute("page",page);
        return new ModelAndView("manage/quartz/list");
    }

    @RequestMapping(value = "/pause")
    public String pause(HttpServletRequest request,
                                     HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.pauseTrigger(new TriggerKey(name, group));
        }
        return "redirect:/manage/quartz/list";
    }

    @RequestMapping(value = "batch/pause")
    public String batchPause(HttpServletRequest request,
                        HttpServletResponse response, Model model) {
        List<TriggerInfo> triggerList = (List<TriggerInfo>)request.getSession().getAttribute("triggerList");
        for (TriggerInfo trigger : triggerList) {
            if (StringUtils.isNotBlank(trigger.getTriggerName()) || StringUtils.isNotBlank(trigger.getTriggerGroup())) {
                schedulerCenter.pauseTrigger(new TriggerKey(trigger.getTriggerName(), trigger.getTriggerGroup()));
            }
        }
        return "redirect:/manage/quartz/list";
    }

    @RequestMapping(value = "/resume")
    public String resume(HttpServletRequest request,
                        HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.resumeTrigger(new TriggerKey(name, group));
        }
        return "redirect:/manage/quartz/list";
    }


    @RequestMapping(value = "batch/resume")
    public String batchResume(HttpServletRequest request,
                         HttpServletResponse response, Model model) {
        List<TriggerInfo> triggerList = (List<TriggerInfo>)request.getSession().getAttribute("triggerList");
        for (TriggerInfo trigger : triggerList) {
            if (StringUtils.isNotBlank(trigger.getTriggerName()) || StringUtils.isNotBlank(trigger.getTriggerGroup())) {
                schedulerCenter.resumeTrigger(new TriggerKey(trigger.getTriggerName(), trigger.getTriggerGroup()));
            }
        }
        return "redirect:/manage/quartz/list";
    }

    @RequestMapping(value = "/remove")
    public String remove(HttpServletRequest request,
                         HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.unscheduleJob(new TriggerKey(name, group));
        }
        return "redirect:/manage/quartz/list";
    }


    @RequestMapping(value = "batch/remove")
    public String batchRemove(HttpServletRequest request,
                         HttpServletResponse response, Model model) {
        List<TriggerInfo> triggerList = (List<TriggerInfo>)request.getSession().getAttribute("triggerList");
        for (TriggerInfo trigger : triggerList) {
            if (StringUtils.isNotBlank(trigger.getTriggerName()) || StringUtils.isNotBlank(trigger.getTriggerGroup())) {
                schedulerCenter.unscheduleJob(new TriggerKey(trigger.getTriggerName(), trigger.getTriggerGroup()));
            }
        }
        return "redirect:/manage/quartz/list";
    }

}
