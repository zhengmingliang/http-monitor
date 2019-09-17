package com.alianga.httpmonitor.utils;
/**
 * Created by 郑明亮 on 2019/9/17 13:37.
 */

import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import okhttp3.Response;
import top.wys.utils.DateUtils;
import top.wys.utils.HttpUtils;

/**
 * <ol>
 *  2019/9/17 13:37 <br>
 *
 * </ol>
 *
 * @author 郑明亮
 * @version 1.0
 */
@Component
public class MonitorUtils {

    @Autowired
    SendEmailUtils sendEmail;
    private final int HTTP_OK = 200;
    /**
     * 发送报警的时间间隔，单位分钟
     */
    private final int SEND_ERROR_MSG_GAPS = 5;
    /**
     * 用户防止频繁报警的map集合
     */
    Map<String, Date> lastSendDateMap = Maps.newHashMap();

    private static final Logger log = LoggerFactory.getLogger(MonitorUtils.class);

    /**
     * @param url         要监控的url
     * @param description 对该url的描述信息
     */
    public void monitor(String url, String description, String... to) {
        try {
            Response response = HttpUtils.getResponse(url);
            int code = response.code();
            if (HTTP_OK == code) {
                log.info("url:{}------response code:{}", url, code);
            } else {
                // 再确认一次，是不是误报
                response = HttpUtils.getResponse(url);
                if (HTTP_OK == response.code()) {
                    log.info("此次属于误报，不报警");
                    return;
                }
                Date lastSendDate = lastSendDateMap.get(url);
                if (lastSendDate == null) {
                    lastSendDateMap.put(url, new Date());
                } else {
                    Date curDate = new Date();
                    long gaps = DateUtils.getTimeGapsInMilliseconds(lastSendDate, curDate) / 1000 / 60;
                    if (gaps < SEND_ERROR_MSG_GAPS) {
                        log.info("小于频繁报警时间");
                        return;
                    }
                    lastSendDateMap.put(url, curDate);
                }
                log.error("url:{}------response code:{}", url, code);
                String titleAll = "【报警】" + description + " 异常";
                String html = response.body().string();
                String content = String.format("当前监控URL：%s,<br>%s访问状态码非200，当前状态码为：%s，错误信息为：<br>%s",url, description, code, html);
                if (to == null || to.length == 0) {
                    to = new String[]{"mpro@vip.qq.com"};
                }
                sendEmail.sendMineMessage(titleAll, content, to);
            }
        } catch (IOException e) {
            log.error("就业网站监控异常", e);
        }
    }
}
