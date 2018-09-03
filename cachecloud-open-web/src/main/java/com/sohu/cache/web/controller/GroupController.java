package com.sohu.cache.web.controller;

import com.sohu.cache.constant.TimeDimensionalityEnum;
import com.sohu.cache.entity.*;
import com.sohu.cache.stats.app.GroupDetailCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.chart.model.HighchartPoint;
import com.sohu.cache.web.chart.model.SimpleChartData;
import com.sohu.cache.web.util.DateUtil;
import com.sohu.cache.web.vo.AppDetailVO;
import com.sohu.cache.web.vo.GroupDetailVO;
import net.sf.json.JSONArray;
import org.apache.catalina.Group;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by caijt on 2016/9/20.
 * 分组统计相关
 */
@Controller
@RequestMapping("/admin/group")
public class GroupController extends BaseController {
    private Logger logger = LoggerFactory.getLogger(GroupController.class);

    @Resource(name = "groupDetailCenter")
    private GroupDetailCenter groupDetailCenter;

    @RequestMapping("/index")
    public ModelAndView index(HttpServletRequest request, HttpServletResponse response, Model model, Long groupId, String firstCommand)
            throws ParseException {
        // 日期转换
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date startDate = new Date();
            startDateParam = DateUtil.formatDate(startDate, "yyyy-MM-dd");
            endDateParam = DateUtil.formatDate(DateUtils.addDays(startDate, 1), "yyyy-MM-dd");
        }
        String slowLogStartDateParam = request.getParameter("slowLogStartDate");
        String slowLogEndDateParam = request.getParameter("slowLogEndDate");
        if (StringUtils.isBlank(slowLogStartDateParam) || StringUtils.isBlank(slowLogEndDateParam)) {
            Date startDate = new Date();
            slowLogEndDateParam = DateUtil.formatDate(startDate, "yyyy-MM-dd");
            slowLogStartDateParam = DateUtil.formatDate(DateUtils.addDays(startDate, -2), "yyyy-MM-dd");
        }

        model.addAttribute("startDate", startDateParam);
        model.addAttribute("endDate", endDateParam);
        model.addAttribute("slowLogStartDate", slowLogStartDateParam);
        model.addAttribute("slowLogEndDate", slowLogEndDateParam);
        model.addAttribute("groupId", groupId);
        model.addAttribute("firstCommand", firstCommand);

        return new ModelAndView("group/groupIndex");
    }

    /**
     * 获取某个命令时间分布图
     *
     * @param groupId 业务组id
     * @throws ParseException
     */
    @RequestMapping("/getMutiDatesCommandStats")
    public ModelAndView getMutiDatesCommandStats(HttpServletRequest request,
                                                 HttpServletResponse response, Model model, Long groupId) throws ParseException {
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));

            // 命令参数
            String commandName = request.getParameter("commandName");
            List<GroupCommandStats> groupCommandStatsList;
            if (StringUtils.isNotBlank(commandName)) {
                groupCommandStatsList = groupDetailCenter.getCommandStatsListV2(groupId, beginTime, endTime, TimeDimensionalityEnum.MINUTE, commandName);
            } else {
                groupCommandStatsList = groupDetailCenter.getCommandStatsListV2(groupId, beginTime, endTime, TimeDimensionalityEnum.MINUTE);
            }
            result = assembleMutilDateGroupCommandJsonMinute(groupCommandStatsList, startDate, endDate);
        }
        model.addAttribute("data", result);
        return new ModelAndView("");
    }


    /**
     * 获取某个命令时间分布图
     *
     * @param groupId 业务组id
     * @throws ParseException
     */
    @RequestMapping("/getCommandStats")
    public ModelAndView getCommandStats(HttpServletRequest request,
                                        HttpServletResponse response, Model model, Long groupId) throws ParseException {
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));

            // 命令参数
            String commandName = request.getParameter("commandName");
            List<GroupCommandStats> groupCommandStatsList;
            if (StringUtils.isNotBlank(commandName)) {
                groupCommandStatsList = groupDetailCenter.getCommandStatsList(groupId, beginTime, endTime, commandName);
            } else {
                groupCommandStatsList = groupDetailCenter.getCommandStatsList(groupId, beginTime, endTime);
            }
            result = assembleJson(groupCommandStatsList);
        }
        write(response, result);
        return null;
    }

    /**
     * 分组统计相关
     *
     * @param groupId 组别id
     * @return
     */
    @RequestMapping("/stat")
    public ModelAndView groupStat(HttpServletRequest request,
                                HttpServletResponse response, Model model, Long groupId) throws ParseException {
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        /** 1.获取group统计信息的VO */
        GroupDetailVO groupDetailVO = groupDetailCenter.getGroupDetail(groupId);
        model.addAttribute("groupDetailVO", groupDetailVO);

        /** 2. 时间. */
        Date startDate;
        Date endDate;
        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            startDate = new Date();
            endDate = DateUtils.addDays(startDate, 1);
        } else {
            endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
            startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        }
        Date yesterDay = DateUtils.addDays(startDate, -1);

        long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
        long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));
        model.addAttribute("startDate", startDateParam);
        model.addAttribute("endDate", endDateParam);
        model.addAttribute("yesterDay", DateUtil.formatDate(yesterDay, "yyyy-MM-dd"));

        // 3.是否超过1天
        if (endDate.getTime() - startDate.getTime() > TimeUnit.DAYS.toMillis(1)) {
            model.addAttribute("betweenOneDay", 0);
        } else {
            model.addAttribute("betweenOneDay", 1);
        }

        // 4. top5命令
        List<GroupCommandStats> top5Commands = groupDetailCenter.getTopLimitGroupCommandStatsList(groupId, beginTime, endTime, 5);
        model.addAttribute("top5Commands", top5Commands);

        // 5.峰值
        List<GroupCommandStats> top5ClimaxList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(top5Commands)) {
            for (GroupCommandStats groupCommandStats : top5Commands) {
                GroupCommandStats temp = groupDetailCenter.getCommandClimax(groupId, beginTime, endTime, groupCommandStats.getCommandName());
                if (temp != null) {
                    top5ClimaxList.add(temp);
                }
            }
        }

        model.addAttribute("top5ClimaxList", top5ClimaxList);

        model.addAttribute("groupId", groupId);
        return new ModelAndView("group/groupStat");
    }

    /**
     *
     * @param groupId
     * @throws ParseException
     */
    @RequestMapping("/getTop5Commands")
    public ModelAndView getAppTop5Commands(HttpServletRequest request,
                                           HttpServletResponse response, Model model, Long groupId) throws ParseException {
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));
            List<GroupCommandStats> groupCommandStats = groupDetailCenter.getTop5GroupCommandStatsList(groupId, beginTime, endTime);
            result = assembleJson(groupCommandStats);
        }
        write(response, result);
        return null;
    }



    /**
     * 获取命中率、丢失率等分布
     *
     * @param groupId    业务组id
     * @param statName 统计项(hit,miss等)
     * @throws ParseException
     */
    @RequestMapping("/getMutiDatesGroupStats")
    public ModelAndView getMutiDatesGroupStats(HttpServletRequest request,
                                             HttpServletResponse response, Model model, Long groupId,
                                             String statName, Integer addDay) throws ParseException {

        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));
            List<GroupStats> groupStats = groupDetailCenter.getGroupStatsList(groupId, beginTime, endTime, TimeDimensionalityEnum.MINUTE);
            result = assembleMutilDateGroupStatsJsonMinute(groupStats, statName, startDate, endDate);
        }
        model.addAttribute("data", result);
        return new ModelAndView("");
    }

    /**
     * 获取命中率、丢失率等分布
     *
     * @param groupId    应用id
     * @param statName 统计项(hit,miss等)
     * @throws ParseException
     */
    @RequestMapping("/getGroupStats")
    public ModelAndView getGroupStats(HttpServletRequest request,
                                    HttpServletResponse response, Model model, Long groupId,
                                    String statName) throws ParseException {
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));
            List<GroupStats> groupStats = groupDetailCenter.getGroupStatsListByMinuteTime(groupId, beginTime, endTime);
            result = assembleGroupStatsJson(groupStats, statName);
        }
        write(response, result);
        return null;
    }

    /**
     * 多命令
     * @param groupId
     * @return
     * @throws ParseException
     */
    @RequestMapping("/getMutiStatGroupStats")
    public ModelAndView getMutiStatGroupStats(HttpServletRequest request,
                                            HttpServletResponse response, Model model, Long groupId) throws ParseException {
        String statNames = request.getParameter("statName");
        List<String> statNameList = Arrays.asList(statNames.split(ConstUtils.COMMA));

        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");
        Date startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
        Date endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
        String result = "[]";
        if (groupId != null) {
            long beginTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(startDate));
            long endTime = NumberUtils.toLong(DateUtil.formatYYYYMMddHHMM(endDate));
            List<GroupStats> groupStats = groupDetailCenter.getGroupStatsList(groupId, beginTime, endTime, TimeDimensionalityEnum.MINUTE);
            result = assembleMutiStatGroupStatsJsonMinute(groupStats, statNameList, startDate);
        }
        model.addAttribute("data", result);
        return new ModelAndView("");
    }

    /**
     * 多时间组装
     * @param groupStats
     * @param statName
     * @param startDate
     * @param endDate
     * @return
     */
    private String assembleMutilDateGroupStatsJsonMinute(List<GroupStats> groupStats, String statName, Date startDate, Date endDate) {
        if (groupStats == null || groupStats.isEmpty()) {
            return "[]";
        }
        Map<String, List<HighchartPoint>> map = new HashMap<String, List<HighchartPoint>>();
        Date currentDate = DateUtils.addDays(endDate, -1);
        int diffDays = 0;
        while (currentDate.getTime() >= startDate.getTime()) {
            List<HighchartPoint> list = new ArrayList<HighchartPoint>();
            for (GroupStats stat : groupStats) {
                try {
                    HighchartPoint highchartPoint = HighchartPoint.getFromGroupStats(stat, statName, currentDate, diffDays);
                    if (highchartPoint == null) {
                        continue;
                    }
                    list.add(highchartPoint);
                } catch (ParseException e) {
                    logger.info(e.getMessage(), e);
                }
            }
            String formatDate = DateUtil.formatDate(currentDate, "yyyy-MM-dd");
            map.put(formatDate, list);
            currentDate = DateUtils.addDays(currentDate, -1);
            diffDays++;
        }
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(map);
        return jsonObject.toString();
    }

    /**
     * GroupStats列表组装成json串
     */
    private String assembleGroupStatsJson(List<GroupStats> groupStats, String statName) {
        if (groupStats == null || groupStats.isEmpty()) {
            return "[]";
        }
        List<SimpleChartData> list = new ArrayList<SimpleChartData>();
        for (GroupStats stat : groupStats) {
            try {
                SimpleChartData chartData = SimpleChartData.getFromGroupStats(stat, statName);
                list.add(chartData);
            } catch (ParseException e) {
                logger.info(e.getMessage(), e);
            }
        }
        JSONArray jsonArray = JSONArray.fromObject(list);
        return jsonArray.toString();
    }

    /**
     * 多命令组装
     * @param groupStats
     * @param statNameList
     * @param startDate
     * @return
     */
    private String assembleMutiStatGroupStatsJsonMinute(List<GroupStats> groupStats, List<String> statNameList, Date startDate) {
        if (groupStats == null || groupStats.isEmpty()) {
            return "[]";
        }
        Map<String, List<HighchartPoint>> map = new HashMap<String, List<HighchartPoint>>();
        for(String statName : statNameList) {
            List<HighchartPoint> list = new ArrayList<HighchartPoint>();
            for (GroupStats stat : groupStats) {
                try {
                    HighchartPoint highchartPoint = HighchartPoint.getFromGroupStats(stat, statName, startDate, 0);
                    if (highchartPoint == null) {
                        continue;
                    }
                    list.add(highchartPoint);
                } catch (ParseException e) {
                    logger.info(e.getMessage(), e);
                }
            }
            map.put(statName, list);
        }
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(map);
        return jsonObject.toString();
    }

    private String assembleMutilDateGroupCommandJsonMinute(List<GroupCommandStats> groupCommandStats, Date startDate, Date endDate) {
        if (groupCommandStats == null || groupCommandStats.isEmpty()) {
            return "[]";
        }
        Map<String, List<HighchartPoint>> map = new HashMap<String, List<HighchartPoint>>();
        Date currentDate = DateUtils.addDays(endDate, -1);
        int diffDays = 0;
        while (currentDate.getTime() >= startDate.getTime()) {
            List<HighchartPoint> list = new ArrayList<HighchartPoint>();
            for (GroupCommandStats stat : groupCommandStats) {
                try {
                    HighchartPoint highchartPoint = HighchartPoint.getFromGroupCommandStats(stat, currentDate, diffDays);
                    if (highchartPoint == null) {
                        continue;
                    }
                    list.add(highchartPoint);
                } catch (ParseException e) {
                    logger.info(e.getMessage(), e);
                }
            }
            String formatDate = DateUtil.formatDate(currentDate, "yyyy-MM-dd");
            map.put(formatDate, list);
            currentDate = DateUtils.addDays(currentDate, -1);
            diffDays++;
        }
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(map);
        return jsonObject.toString();
    }

    private String assembleJson(List<GroupCommandStats> groupCommandStatsList) {
        return assembleJson(groupCommandStatsList, null);
    }

    private String assembleJson(List<GroupCommandStats> groupCommandStatsList, Integer addDay) {
        if (groupCommandStatsList == null || groupCommandStatsList.isEmpty()) {
            return "[]";
        }
        List<SimpleChartData> list = new ArrayList<SimpleChartData>();
        for (GroupCommandStats stat : groupCommandStatsList) {
            try {
                SimpleChartData chartData = SimpleChartData
                        .getFromGroupCommandStats(stat, addDay);
                list.add(chartData);
            } catch (ParseException e) {
                logger.info(e.getMessage(), e);
            }
        }
        JSONArray jsonArray = JSONArray.fromObject(list);
        return jsonArray.toString();
    }
}
