package com.soulmate.ai.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具服务
 * 通过 Spring AI Tool 注解暴露时间查询能力给 LLM
 */
@Slf4j
@Service
public class TimeToolService {

    @Tool(description = "获取当前的日期和时间。当用户询问现在几点、今天几号、今天星期几、当前时间等时间相关问题时调用")
    public String getCurrentTime() {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
            String formatted = now.format(formatter);

            String[] weekDays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            String weekDay = weekDays[now.getDayOfWeek().getValue() - 1];

            return String.format("当前时间：%s %s（北京时间）", formatted, weekDay);
        } catch (Exception e) {
            log.error("获取当前时间失败", e);
            return "获取当前时间失败: " + e.getMessage();
        }
    }
}
