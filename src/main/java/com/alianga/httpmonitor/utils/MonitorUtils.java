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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;

import okhttp3.Response;
import top.wys.utils.DateUtils;
import top.wys.utils.EncryptUtils;
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
    Map<String, Date> lastSendDateMap = Maps.newConcurrentMap();

    private static final Logger log = LoggerFactory.getLogger(MonitorUtils.class);

    /**
     * @param url         要监控的url
     * @param description 对该url的描述信息
     */
    public void monitor(String url, String description, String... to) {
        // 收件人参数校验
        if (to == null || to.length == 0) {
            to = new String[]{"mpro@vip.qq.com"};
        }
        try {
            String urlMd5 = EncryptUtils.md5(url);
            Response response = HttpUtils.getResponse(url);
            int code = response.code();
            if (HTTP_OK == code) {
                if (lastSendDateMap.get(urlMd5) != null) {
                    String titleAll = "【通知】" + description + " 已恢复正常";
                    String html = response.body().string();
                    String content = String.format("当前监控URL：%s  ,<br>%s访问状态码非200，当前状态码为：%s，错误信息为：<br>%s",url, description, code, html);
                    sendEmail.sendMineMessage(titleAll,content,to);
                    //移除当前值，避免重复通知
                    lastSendDateMap.remove(urlMd5);
                }
                log.info("url:{}------response code:{}", url, code);
            } else {

                sendErrInfo(url, description, urlMd5, code, to);
            }
        }catch (UnknownHostException e){
            sendExceptionInfo(url, description, to, "域名无法解析到指定服务器", " 域名无法解析到指定服务器", e.getMessage(), "当前监控URL：%s  ,<br>%s 域名无法解析到指定服务器，错误信息为：<br>%s");
        }catch (SocketTimeoutException e){
            sendExceptionInfo(url, description, to, "网络连接超时", " 网站无法连接", e.getMessage(), "当前监控URL：%s  ,<br>%s 网络连接超时，错误信息为：<br>%s");
        }catch (ConnectException e){
            sendExceptionInfo(url, description, to, "网络连接异常", " 网站无法连接", e.getMessage(), "当前监控URL：%s  ,<br>%s 网络连接异常，错误信息为：<br>%s");
        }catch (IOException e) {
            log.error("{}异常",description, e);
        }
    }

    private void sendExceptionInfo(String url, String description, String[] to, String errInfo, String tag, String message, String formatString) {
        log.error(errInfo);
        Response response = retry(url);
        if (response != null) {
            return;
        }
        String titleAll = "【报警】" + description + tag;
        String html = message;
        String content = String.format(formatString, url, description, html);
        sendEmail.sendMineMessage(titleAll, content, to);
    }

    /**
     * 发送异常信息
     * @param url
     * @param description
     * @param urlMd5
     * @param code
     * @param to
     * @throws IOException
     */
    private void sendErrInfo(String url, String description, String urlMd5, int code, String[] to) throws IOException {
        // 再确认一次，是不是误报
        Response response = retry(url);
        if (response == null) {return;}
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
            // 用于频繁报警校验
            lastSendDateMap.put(url, curDate);
            // 用于恢复正常时，是否需要发送恢复正常通知，如果发现有该url的md5值，则发送恢复正常通知，如果没有，则不发送通知
            lastSendDateMap.put(urlMd5, curDate);
        }
        log.error("url:{}------response code:{}", url, code);
        String titleAll = "【报警】" + description + " 异常";
        String html = response.body().string();
        String content = String.format("当前监控URL：%s  ,<br>%s访问状态码非200，当前状态码为：%s，错误信息为：<br>%s",url, description, code, html);
        sendEmail.sendMineMessage(titleAll, content, to);
    }

    private Response retry(String url)  {
        Response response = null;
        try {
            response = HttpUtils.getResponse(url);
            if (HTTP_OK == response.code()) {
                log.info("此次属于误报，不报警");
                return null;
            }
        } catch (IOException e) {
            log.error("retry error",e);
        }
        return response;
    }
}
