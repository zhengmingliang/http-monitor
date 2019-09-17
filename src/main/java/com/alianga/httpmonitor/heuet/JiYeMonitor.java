package com.alianga.httpmonitor.heuet;
/**
 * Created by 郑明亮 on 2019/9/17 11:30.
 */

import com.alianga.httpmonitor.utils.MonitorUtils;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <ol>
 *  2019/9/17 11:30 <br>
 *
 * </ol>
 *
 * @author 郑明亮
 * @version 1.0
 */
@Component
@Order(2)
public class JiYeMonitor implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(JiYeMonitor.class);


    static String url = "http://jy.heuet.edu.cn/JY/index/selectIndex";

    @Autowired
    MonitorUtils monitor;


    @Override
    public void run(String... args) {
        // 轮训时间间隔，单位分钟
        int period = 1;

        if (args.length > 0) {
            try {
                period = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("java -jar xxx.jar period(轮训时间间隔，单位分钟)");
        }
        try {
            String fileName = "notice.txt";
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            List<String> noticeList = Files.readLines(file, Charset.forName("UTF-8"));
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    monitor.monitor(url, "河北经贸大学就业信息服务网",noticeList.toArray(new String[noticeList.size()]));
                }
            }, 0, period, TimeUnit.MINUTES);
        } catch (IOException e) {
            log.error("创建文件异常", e);
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }


    }
}
