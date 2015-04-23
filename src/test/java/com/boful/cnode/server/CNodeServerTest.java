package com.boful.cnode.server;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.server.codec.BofulCodec;

public class CNodeServerTest {

	public static void main(String[] args) {
		CNodeServerTest test = new CNodeServerTest();
		try {
			test.connect("127.0.0.1", 8888);
			test.send("e:/爱情公寓番外篇温酒煮华雄.f4v", "e:/test/bak.mp4");
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		test.disconnect();
	}

	private ConnectFuture cf;
	private NioSocketConnector connector = new NioSocketConnector();
	private Logger logger = Logger.getLogger(CNodeServerTest.class);
	/***
	 * 解码器定义
	 */
	private static BofulCodec bofulCodec = new BofulCodec();
	private static ClientHandler clientHandler = new ClientHandler();

	public void connect(String address, int port) {
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
	}

	public void send(String diskFile, String destFile) throws Exception {
		IoSession ioSession = cf.getSession();
		if (ioSession != null) {
			ConvertTaskProtocol convertTaskProtocol = new ConvertTaskProtocol();
			
			// 命令行
			String cmd = "";
			cmd += " -i "+diskFile;
			cmd += " -o "+destFile;
			cmd += " -vb 30000";
			cmd += " -ab 20000";
			cmd += " -size 300x200";
			convertTaskProtocol.setCmd(cmd);
			ioSession.write(convertTaskProtocol);
		} else {
			throw new Exception("未连接上");
		}

	}
	
	public void disconnect(){
		connector.dispose();
	}
}
