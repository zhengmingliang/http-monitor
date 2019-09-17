package com.alianga.httpmonitor.utils;
/**
 * Created by 郑明亮 on 2019/9/17 11:35.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * <ol>
 *  2019/9/17 11:35 <br>
 *
 * </ol>
 *
 * @author 郑明亮
 * @version 1.0
 */
@Component
public class SendEmailUtils {
    @Autowired
    JavaMailSender sender;

    @Value("${spring.mail.username}")
    String from;

    public void sendSimpleText(String title, String content, String to){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(title);
        message.setText(content);
        sender.send(message);

    }


    public void sendMineMessage(String title, String content, String... to){
        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content,true);
            sender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
}
