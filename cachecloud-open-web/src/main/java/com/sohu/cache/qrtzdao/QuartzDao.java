package com.sohu.cache.qrtzdao;

import com.sohu.cache.entity.TriggerInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * quartz相关的dao操作
 *
 * @author: lingguo
 * @time: 2014/10/13 14:44
 */
public interface QuartzDao {

    public List<TriggerInfo> getTriggersByJobGroup(String jobGroup);

    public List<TriggerInfo> getAllTriggers();

    public List<TriggerInfo> searchTriggerByNameOrGroup(String queryString);

    List<TriggerInfo> searchTriggers(@Param("query") String query, @Param("start") int start, @Param("size") int size);

    int count(String query);

    void save(TriggerInfo triggerInfo);
}
