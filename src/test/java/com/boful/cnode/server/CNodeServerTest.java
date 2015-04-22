package com.boful.cnode.server;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.boful.cnode.protocol.ConvertTaskProtocol;

public class CNodeServerTest {

	public static void main(String[] args) throws Exception {
		CNodeServerTest test = new CNodeServerTest();
		test.connect("127.0.0.1", 9999);
		test.send("e:/爱情公寓番外篇温酒煮华雄.f4v", "e:/test/bak.mp4");
	}

	private ConnectFuture cf;
	private NioSocketConnector connector = new NioSocketConnector();
	private Logger logger = Logger.getLogger(CNodeServerTest.class);

	public void connect(String address, int port) {
		logger.debug("连接到：" + address + ":" + port);
		// set connect timeout
		connector.setConnectTimeoutMillis(60 * 60 * 1000);
		// 连接到服务器：
		cf = connector.connect(new InetSocketAddress(address, port));
		cf.awaitUninterruptibly();
	}

	public void send(String file, String destFile) throws Exception {
		IoSession ioSession = cf.getSession();
		if (ioSession != null) {

			ConvertTaskProtocol convertTaskProtocol = new ConvertTaskProtocol();
			convertTaskProtocol.setCmd("");
			ioSession.write(convertTaskProtocol);
		} else {
			throw new Exception("未连接上");
		}

	}
}
