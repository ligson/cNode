package com.boful.cnode.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.boful.convert.core.ConvertProviderConfig;
import com.boful.convert.core.impl.BofulConvertProvider;

public class ConvertProviderUtils {

    private static ConvertProviderConfig config = null;
    private static BofulConvertProvider bofulConvertProvider = null;
    private static Logger logger = Logger.getLogger(ConvertProviderUtils.class);

    public static void initConvertProviderConfig() throws IOException, DocumentException {
        config = new ConvertProviderConfig();
        config.init(new File("src/main/resources/convert.xml"));
        logger.debug("配置文件初始化成功...........");
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
