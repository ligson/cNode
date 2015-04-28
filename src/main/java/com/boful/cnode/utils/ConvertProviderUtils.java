package com.boful.cnode.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.boful.convert.core.ConvertProviderConfig;
import com.boful.convert.core.impl.BofulConvertProvider;

public class ConvertProviderUtils {

    private static ConvertProviderConfig config = null;
    private static BofulConvertProvider bofulConvertProvider = null;
    private static Logger logger = Logger.getLogger(ConvertProviderUtils.class);

    public static int[] initServerConfig() {
        int[] config = new int[3];
        try {
            URL url = ClassLoader.getSystemResource("conf/config.properties");
            if (url == null) {
                url = ClassLoader.getSystemResource("config.properties");
            }
            InputStream in = new BufferedInputStream(new FileInputStream(url.getPath()));
            Properties props = new Properties();
            props.load(in);

            // 取得内容
            int bufferSize = Integer.parseInt(props.getProperty("server.bufferSize"));
            int idleTime = Integer.parseInt(props.getProperty("server.idleTime"));
            int port = Integer.parseInt(props.getProperty("server.port"));

            config[0] = bufferSize;
            config[1] = idleTime;
            config[2] = port;

            return config;
        } catch (Exception e) {
            logger.debug("配置文件初始化失败...........");
            logger.debug("错误信息：" + e.getMessage());
            return config;
        }

    }

    public static boolean initConvertProviderConfig() {
        try {
            config = new ConvertProviderConfig();
            URL url = ClassLoader.getSystemResource("conf/convert.xml");
            if (url == null) {
                url = ClassLoader.getSystemResource("convert.xml");
            }
            config.init(new File(url.getPath()));
            logger.debug("配置文件初始化成功...........");
            return true;
        } catch (Exception e) {
            logger.debug("配置文件初始化失败...........");
            logger.debug("错误信息：" + e.getMessage());
            return false;
        }

    }

    public static ConvertProviderConfig getConfig() {
        return config;
    }

    public static BofulConvertProvider getBofulConvertProvider() {
        if (bofulConvertProvider == null) {
            bofulConvertProvider = new BofulConvertProvider(config);
        }
        return bofulConvertProvider;
    }
}
