package com.boful.cnode.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.boful.cnode.server.codec.BofulCodec;
import com.boful.cnode.utils.ConvertProviderUtils;

public class CNodeServer {
    /***
     * 解码器定义
     */
    private static BofulCodec bofulCodec = new BofulCodec();
    /***
     * 服务器端业务处理
     */
    private static NodeServerHandler serverHandler = new NodeServerHandler();

    private static NioSocketAcceptor acceptor = new NioSocketAcceptor();
    private static Logger logger = Logger.getLogger(CNodeServer.class);

    public static void main(String[] args) throws Exception {
        startServer();
    }

    public static void startServer() {
        int[] config = ConvertProviderUtils.initServerConfig();
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(bofulCodec));
        acceptor.setHandler(serverHandler);

        acceptor.getSessionConfig().setReadBufferSize(config[0]);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, config[1]);
        try {
            acceptor.bind(new InetSocketAddress(config[2]));
        } catch (IOException e) {
            logger.debug("服务器启动失败...........");
            logger.debug("错误信息：" + e.getMessage());
            System.exit(0);
        }
        logger.debug("starting...........");

        // 初始化ConvertProviderConfig
        boolean initState = ConvertProviderUtils.initConvertProviderConfig();
        if (!initState) {
            logger.debug("程序退出...........");
            System.exit(0);
        }

        // 初始化客户端
        initState = ConvertProviderUtils.initClient();
        if (!initState) {
            logger.debug("程序退出...........");
            System.exit(0);
        }
    }
}
