package com.openai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class TimeTools {

    private static final Logger logger = LoggerFactory.getLogger(TimeTools.class);

    @Tool(name = "getCurrentLocalTime", description = "Get the current time in the user's timezone")
    String getCurrentLocalTime() {
        logger.info("Returning the current time in user's timezone - {}", LocalTime.now());
        return LocalTime.now().toString();
    }

    @Tool(name = "getCurrentTime", description = "Get the current time in the specified timezone")
    String getCurrentTime(@ToolParam(description = "value representing the time zone") String timezone) {
        logger.info("Returning the current time in the timezone - {}", timezone);
        return LocalTime.now(ZoneId.of(timezone)).toString();
    }

}
