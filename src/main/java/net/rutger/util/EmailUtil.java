package net.rutger.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by rutger on 09-06-16.
 */
public class EmailUtil {

    private static final Logger logger = LoggerFactory.getLogger(EmailUtil.class);

    public static void email(final Object caller, final String content, final String subject, final boolean contentIsImageLocation,
                             final boolean debug){
        logger.debug("Sending email Email");

        final Properties systemProps = PropertiesUtil.getWateringsSystemProperties(caller);

        final String username = systemProps.getProperty("emailUser");
        final String password = systemProps.getProperty("emailPassword");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(systemProps.getProperty("emailFrom"), "Watering system"));

            InternetAddress[] addresses = InternetAddress.parse(systemProps.getProperty("emailTo"));
            InternetAddress[] recipients;
            if (debug && addresses.length>1) {
                recipients = new InternetAddress[] {addresses[0]};
            } else {
                recipients = addresses;
            }
            message.setRecipients(Message.RecipientType.TO, recipients);

            message.setSubject(subject);


            Multipart mp=new MimeMultipart();

            if (contentIsImageLocation) {
                MimeBodyPart attachment= new MimeBodyPart();
                attachment.attachFile(content);
                mp.addBodyPart(attachment);
            } else {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(content, "utf-8");
                mp.addBodyPart(textPart);
            }

            message.setContent(mp);

            Transport.send(message);

            System.out.println("Email done");

        } catch (MessagingException e) {
            logger.error("MessagingException while sending email", e);
        } catch (IOException e) {
            logger.error("IOException while creating image for sending email", e);
        }
    }

}
