package com.soulmate.ai.mcp;

import com.soulmate.common.config.QweatherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 天气工具服务 - 基于和风天气 API
 * 通过 Spring AI Tool 注解暴露天气查询能力给 LLM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherToolService {

    private final QweatherProperties qweatherProperties;
    private final RestClient restClient = RestClient.create();

    @Tool(description = "获取指定城市的当前天气和温度。仅在用户明确询问天气、气温、是否下雨等天气相关信息时调用，不要在普通闲聊中主动调用。城市名支持中文或英文，如 Beijing、上海、Tokyo")
    public String getWeather(@ToolParam(description = "城市名称，如 Beijing、上海、Tokyo") String city) {
        try {
            // 1. 查询城市 ID
            String cityId = lookupCityId(city);
            if (cityId == null) {
                return String.format("未找到城市「%s」，请检查城市名称是否正确。", city);
            }

            // 2. 查询实时天气
            return fetchCurrentWeather(cityId, city);
        } catch (Exception e) {
            log.error("查询天气失败: city={}", city, e);
            return String.format("查询「%s」天气时发生错误: %s", city, e.getMessage());
        }
    }

    @Tool(description = "获取指定城市未来3天的天气预报。当用户询问明天、后天、未来几天、周末天气、是否需要带伞等关于未来天气的问题时调用。城市名支持中文或英文，如 Beijing、上海、Tokyo")
    public String getWeatherForecast(@ToolParam(description = "城市名称，如 Beijing、上海、Tokyo") String city) {
        try {
            // 1. 查询城市 ID
            String cityId = lookupCityId(city);
            if (cityId == null) {
                return String.format("未找到城市「%s」，请检查城市名称是否正确。", city);
            }

            // 2. 查询3天天气预报
            return fetchWeatherForecast(cityId, city);
        } catch (Exception e) {
            log.error("查询天气预报失败: city={}", city, e);
            return String.format("查询「%s」天气预报时发生错误: %s", city, e.getMessage());
        }
    }

    /**
     * 通过 GeoAPI 查询城市 ID
     */
    @SuppressWarnings("unchecked")
    private String lookupCityId(String city) {
        String baseUrl = qweatherProperties.getApiHostUrl();

        Map<String, Object> response = restClient.get()
                .uri(baseUrl + "/geo/v2/city/lookup?location={location}&key={key}&number=1",
                        city, qweatherProperties.getApiKey())
                .retrieve()
                .body(Map.class);

        if (response == null || !"200".equals(String.valueOf(response.get("code")))) {
            log.warn("城市查询失败: city={}, response={}", city, response);
            return null;
        }

        java.util.List<Map<String, Object>> location =
                (java.util.List<Map<String, Object>>) response.get("location");
        if (location == null || location.isEmpty()) {
            return null;
        }

        String id = String.valueOf(location.get(0).get("id"));
        String name = String.valueOf(location.get(0).get("name"));
        String adm1 = String.valueOf(location.get(0).get("adm1"));
        log.info("城市匹配: {} -> {} ({} {})", city, id, adm1, name);
        return id;
    }

    /**
     * 查询实时天气
     */
    @SuppressWarnings("unchecked")
    private String fetchCurrentWeather(String cityId, String cityName) {
        String baseUrl = qweatherProperties.getApiHostUrl();

        Map<String, Object> response = restClient.get()
                .uri(baseUrl + "/v7/weather/now?location={location}&key={key}",
                        cityId, qweatherProperties.getApiKey())
                .retrieve()
                .body(Map.class);

        if (response == null || !"200".equals(String.valueOf(response.get("code")))) {
            log.warn("天气查询失败: cityId={}, response={}", cityId, response);
            return String.format("查询「%s」天气失败，天气服务暂时不可用。", cityName);
        }

        Map<String, Object> now = (Map<String, Object>) response.get("now");
        if (now == null) {
            return String.format("查询「%s」天气失败，未获取到天气数据。", cityName);
        }

        String text = String.valueOf(now.get("text"));           // 天气现象，如"晴"
        String temp = String.valueOf(now.get("temp"));           // 温度
        String feelsLike = String.valueOf(now.get("feelsLike")); // 体感温度
        String humidity = String.valueOf(now.get("humidity"));   // 相对湿度
        String windDir = String.valueOf(now.get("windDir"));     // 风向
        String windScale = String.valueOf(now.get("windScale")); // 风力等级

        return String.format("当前%s天气：%s，气温%s°C（体感%s°C），湿度%s%%，%s%s级。",
                cityName, text, temp, feelsLike, humidity, windDir, windScale);
    }

    /**
     * 查询3天天气预报
     */
    @SuppressWarnings("unchecked")
    private String fetchWeatherForecast(String cityId, String cityName) {
        String baseUrl = qweatherProperties.getApiHostUrl();

        Map<String, Object> response = restClient.get()
                .uri(baseUrl + "/v7/weather/3d?location={location}&key={key}",
                        cityId, qweatherProperties.getApiKey())
                .retrieve()
                .body(Map.class);

        if (response == null || !"200".equals(String.valueOf(response.get("code")))) {
            log.warn("天气预报查询失败: cityId={}, response={}", cityId, response);
            return String.format("查询「%s」天气预报失败，天气服务暂时不可用。", cityName);
        }

        java.util.List<Map<String, Object>> daily =
                (java.util.List<Map<String, Object>>) response.get("daily");
        if (daily == null || daily.isEmpty()) {
            return String.format("查询「%s」天气预报失败，未获取到预报数据。", cityName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s未来3天天气预报：\n", cityName));

        for (int i = 0; i < daily.size(); i++) {
            Map<String, Object> day = daily.get(i);
            String fxDate = String.valueOf(day.get("fxDate"));         // 日期
            String textDay = String.valueOf(day.get("textDay"));       // 白天天气
            String textNight = String.valueOf(day.get("textNight"));   // 夜间天气
            String tempMax = String.valueOf(day.get("tempMax"));       // 最高温度
            String tempMin = String.valueOf(day.get("tempMin"));       // 最低温度
            String humidity = String.valueOf(day.get("humidity"));     // 湿度
            String windDirDay = String.valueOf(day.get("windDirDay")); // 白天风向
            String windScaleDay = String.valueOf(day.get("windScaleDay")); // 风力等级

            String dayLabel = i == 0 ? "今天" : (i == 1 ? "明天" : "后天");
            sb.append(String.format("• %s（%s）：%s转%s，气温%s~%s°C，湿度%s%%，%s%s级\n",
                    dayLabel, fxDate, textDay, textNight, tempMin, tempMax,
                    humidity, windDirDay, windScaleDay));
        }

        return sb.toString().trim();
    }
}
