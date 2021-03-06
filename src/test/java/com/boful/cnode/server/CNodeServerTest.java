package com.boful.cnode.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.boful.cnode.server.codec.BofulCodec;
import com.boful.net.cnode.protocol.ConvertTaskProtocol;

public class CNodeServerTest {

    public static void main(String[] args) {

        // 视频转码
        CNodeServerTest test1 = new CNodeServerTest();
        try {
            test1.connect("127.0.0.1", 9000);
            String cmd = "-id job_0001 -i e:/爱情公寓番外篇温酒煮华雄.f4v -o e:/test/bak.mp4 -vb 30000 -ab 20000 -size 300x200";
            test1.send(cmd, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 图片转码
        CNodeServerTest test2 = new CNodeServerTest();
        try {
            test2.connect("127.0.0.1", 9000);
            String cmd = "-id job_0002 -i e:/Koala.jpg -o e:/test/Koala1.jpg";
            test2.send(cmd, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 音频转码
        CNodeServerTest test3 = new CNodeServerTest();
        try {
            test3.connect("127.0.0.1", 9000);
            String cmd = "-id job_0003 -i e:/mmd.mp3 -o e:/test/mmd2.wav -ab 20000";
            test3.send(cmd, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 文档转码
        CNodeServerTest test4 = new CNodeServerTest();
        try {
            test4.connect("127.0.0.1", 9000);
            String cmd = "-id job_0004 -i e:/aaa.txt -o e:/test/aaa2.swf";
            test4.send(cmd, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ConnectFuture cf;
    private NioSocketConnector connector = new NioSocketConnector();
    private Logger logger = Logger.getLogger(CNodeServerTest.class);
    private IoSession ioSession;

    /***
     * 解码器定义
     */
    private static BofulCodec bofulCodec = new BofulCodec();
    private static TestClientHandler clientHandler = new TestClientHandler();

    public void connect(String address, int port) throws Exception {
        logger.debug("连接到：" + address + ":" + port);

        // 创建接受数据的过滤器
        DefaultIoFilterChainBuilder chain = connector.getFilterChain();

        // 设定这个过滤器将一行一行(/r/n)的读取数据
        chain.addLast("codec", new ProtocolCodecFilter(bofulCodec));

        // 客户端的消息处理器：一个SamplMinaServerHander对象
        connector.setHandler(clientHandler);

        // set connect timeout
        connector.setConnectTimeoutMillis(60 * 60 * 1000);
        // 连接到服务器：
        cf = connector.connect(new InetSocketAddress(address, port));
        cf.awaitUninterruptibly();
        try {
            ioSession = cf.getSession();
            logger.debug("服务器" + address + ":" + port + "连接成功！");
        } catch (Exception e) {
            logger.debug("服务器" + address + ":" + port + "未连接上！");
            throw e;
        }
    }

    public void send(String cmd, SocketAddress rootAddress) throws Exception {
        if (ioSession != null) {
            ConvertTaskProtocol convertTaskProtocol = new ConvertTaskProtocol();
            convertTaskProtocol.setCmd(cmd);
            ioSession.write(convertTaskProtocol);
        } else {
            throw new Exception("未连接上");
        }
    }

    public void disconnect() {
        System.out.println("disconnect");
        ioSession.getCloseFuture().awaitUninterruptibly();
        connector.dispose();
    }
}
