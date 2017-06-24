package net.rutger;

import net.rutger.util.PropertiesUtil;
import net.rutger.wateringsystem.WateringTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TimerServlet.class);
    private Timer morningtimer = null;
    private Timer hourlyTimer = null;

    public void init() throws ServletException {
        logger.debug("Starting TimerServlet");

        TimerTask hourlyTask = new TimerTask(){

            @Override
            public void run() {
                logger.debug("Running hourly task at {}", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));

            }
        };

        ZonedDateTime nextMorningTimerTime = getNextTime(8,50);
        morningtimer = new Timer();
        morningtimer.schedule(new WateringTimerTask(), Date.from(nextMorningTimerTime.toInstant()), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
        logger.debug("WateringTimerTask (daily) set for {}", nextMorningTimerTime.format(DateTimeFormatter.RFC_1123_DATE_TIME));

        hourlyTimer = new Timer();
        hourlyTimer.schedule(hourlyTask, new Date(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
        logger.debug("TimerServlet has started");
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {

        // run the watertask on a get request (testing purposes)
        logger.debug("Run the watertask");
        new WateringTimerTask().runGardenWaterTask(PropertiesUtil.getWateringsSystemProperties(this));
    }


    @Override
    public void destroy() {
        // do nothing.
        logger.debug("Destroy TimerServlet");
        if (morningtimer != null) {
            morningtimer.cancel();
        }
        if (hourlyTimer != null) {
            hourlyTimer.cancel();
        }
    }

    public ZonedDateTime getNextTime(int hourOfDay, int minutes) {
        ZonedDateTime dt = ZonedDateTime.now().withHour(hourOfDay).withMinute(minutes).withSecond(0);
        if (dt.isBefore(ZonedDateTime.now())) {
            dt = dt.plusDays(1);
        }
        return dt;
    }
}