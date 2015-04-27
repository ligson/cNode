package com.boful.cnode.utils;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.boful.convert.core.ConvertProviderConfig;
import com.boful.convert.core.impl.BofulConvertProvider;
import com.boful.net.fserver.ClientMain;

public class ConvertProviderUtils {

    private static ConvertProviderConfig config = null;
    private static BofulConvertProvider bofulConvertProvider = null;
    private static Logger logger = Logger.getLogger(ConvertProviderUtils.class);
    private static ClientMain client = null;

    public static boolean initConvertProviderConfig() {
        try {
            config = new ConvertProviderConfig();
            URL url = ClassLoader.getSystemResource("conf/convert.xml");
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

    public static boolean initClient() {
        try {
            SAXReader SR = new SAXReader();
            URL url = ClassLoader.getSystemResource("conf/client.xml");
            Document doc = SR.read(url.getPath());
            Element rootElement = doc.getRootElement();

            Element clientRootElement = rootElement.element("client");
            Element serverIpElement = clientRootElement.element("ip");
            Element serverPortElement = clientRootElement.element("port");
            String ip = serverIpElement.getText();
            int port = Integer.parseInt(serverPortElement.getText());
            client = new ClientMain();
            client.connect(ip, port);

            logger.debug("客户端初始化成功...........");
            return true;
        } catch (Exception e) {
            logger.debug("客户端初始化失败...........");
            logger.debug("错误信息：" + e.getMessage());
            return false;
        }
    }

    public static ClientMain getClient() {
        return client;
    }
}
