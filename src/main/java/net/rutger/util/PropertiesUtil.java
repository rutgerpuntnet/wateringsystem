package net.rutger.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rutger on 24-06-17.
 */
public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    public static Properties getWateringsSystemProperties(final Object caller){
        final Properties result = new Properties();


        try (InputStream appinput = caller.getClass().getClassLoader().getResourceAsStream("app.properties")) {
            final Properties appProperties = new Properties();
            appProperties.load(appinput);
            final String wateringSystemPropertiesLocation = appProperties.getProperty("wateringsystem.properties.location");

            try (InputStream inputWatering = new FileInputStream(wateringSystemPropertiesLocation)) {
                result.load(inputWatering);
            } catch (IOException ex) {
                logger.error("Cannot read wateringSystemProperties, returning empty properties", ex);
            }
        } catch (IOException ex) {
            logger.error("Cannot read app.properties. Returning empty properties", ex);
        }
        return result;

    }

}
