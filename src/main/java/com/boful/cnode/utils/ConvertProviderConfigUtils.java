package com.boful.cnode.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.boful.convert.core.ConvertProviderConfig;

public class ConvertProviderConfigUtils {

    private static ConvertProviderConfig config = null;
    private static Logger logger = Logger.getLogger(ConvertProviderConfigUtils.class);

    public static void initConvertProviderConfig() throws IOException, DocumentException {
        ConvertProviderConfig config = new ConvertProviderConfig();
        config.init(new File("e:/convert.xml"));
        logger.debug("配置文件初始化成功...........");
    }

    public static ConvertProviderConfig getConfig() {
        return config;
    }
}
