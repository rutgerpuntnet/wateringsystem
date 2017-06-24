package net.rutger.wateringsystem;

import net.rutger.util.EmailUtil;
import net.rutger.util.KnmiUtil;
import net.rutger.util.PropertiesUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

/**
 * Created by rutger on 18-05-16.
 */
public class WateringTimerTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(WateringTimerTask.class);
    private static final String GARDEN_ARDUINO_BASE_URL = "http://192.168.1.12";
    private static final String GARDEN_ARDUINO_TIMER_PARAMETER = "?setTimer=";
    private static final int DEFAULT_WATER_MINUTES = 10;
    private static final int AVERAGE_MAKKINK = 27;

    private String wateringMinutesExplanation = "";

    @Override
    public void run() {
        final Properties props = PropertiesUtil.getWateringsSystemProperties(this);
        try {
            if (isWateringsystemActive(props)) {
                runGardenWaterTask(props);
            } else {
                logger.debug("Wateringsystem deactivated");
            }
        } catch (RuntimeException e) {
            logger.error("RuntimException on running watering task");
        }
    }


    public void runGardenWaterTask(final Properties props) {
        logger.debug("Running garden water task at " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        int minutes = determineWateringMinutes();

        // Get request on arduino wateringsystem
        String emailContent = null;
        try {
            if (minutes > 0) {
                Map<String,Integer> wateringSystemData = callWateringSystem(props, minutes);
                emailContent = "Successfully watered the plants for " + minutes + " minutes.\n" + wateringMinutesExplanation;

            }
        } catch (IOException e) {
            logger.error("Exception on calling arduino URL", e);
            emailContent = "An exeption was caught while trying to call the watering system Arduino. Message: " + e.getMessage();
        }
        EmailUtil.email(this, emailContent,"Wateringsystem",false,true);

    }


    public Map<String,Integer> callWateringSystem(final Properties props, final Integer minutes) throws IOException {
        String url = props.getProperty("garden.arduino.base.url");
        if (url == null) {
            url = GARDEN_ARDUINO_BASE_URL;
        }

        if (minutes != null) {
            url += GARDEN_ARDUINO_TIMER_PARAMETER + String.valueOf(minutes);
        }
        JSONObject response = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));

        Map<String, Integer> result = new HashMap<>();
        for (String key : response.keySet()) {
            logger.debug("Json key/value: " + key + " / " + response.getInt(key));
            result.put(key, response.getInt(key));
        }

        return result;

    }

    /*
     * Determine the number of minutes to water, based on weather data and default values
     */
    public int determineWateringMinutes() {
        // Get data from KNMI (for now, start with default 10 minutes
        int minutes = DEFAULT_WATER_MINUTES;
        try {
            Map<KnmiUtil.WeatherDataTypes, Integer> weatherData = KnmiUtil.getYesterdaysWeatherData();
            Integer transpiration = weatherData.get(KnmiUtil.WeatherDataTypes.TRANSPIRATION);
            wateringMinutesExplanation = "Transpiration index (Makkink) = " + transpiration;
            // TODO log all weather data
            logger.info("transpiration " + transpiration);
            // transpiration index is between about 4 (on a very wet day) and about 57 (on a very hot sunny dry day)
            // We'll use 27 as average, any 2 points, we'll add 1 minute, below 25 is the opposite
            if (transpiration != null) {
                int restTranspiration = (transpiration - AVERAGE_MAKKINK) / 2;
                minutes += restTranspiration;
                wateringMinutesExplanation +=  ". Will add/deduct " + restTranspiration + " from the default " + DEFAULT_WATER_MINUTES
                        + " number of watering minutes. Resulting in watering the plants for " +  minutes + "minutes";
            }
        } catch (RuntimeException e) {
            logger.warn("Exception on receiving weatherdata. Returning default minutes: " + e.getMessage());
            wateringMinutesExplanation = "No KNMI data found (" + e.getMessage() + "): Watering for the default of " + DEFAULT_WATER_MINUTES + "minutes.";
        }
        logger.info("Number of minutes to water: " + minutes);

        return minutes;
    }

    private boolean isWateringsystemActive(final Properties props) {
        return "true".equalsIgnoreCase(props.getProperty("wateringsystem.enabled"));
    }
}
