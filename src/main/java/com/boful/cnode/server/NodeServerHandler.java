package com.boful.cnode.server;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.boful.cnode.protocol.ConvertStateProtocol;
import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.protocol.Operation;

public class NodeServerHandler extends IoHandlerAdapter {
	private Set<IoSession> sessions = new HashSet<IoSession>();
	private static Logger logger = Logger.getLogger(NodeServerHandler.class);

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		super.sessionCreated(session);
		sessions.add(session);

	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		sessions.remove(session);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		Field field = null;
		try {
			field = message.getClass().getDeclaredField("OPERATION");
		} catch (NoSuchFieldException exception) {
			logger.debug(exception);
		}
		if (field != null) {
			int operation = field.getInt(message);
			if (operation == Operation.TAG_CONVERT_TASK) {
				ConvertTaskProtocol convertTaskProtocol = (ConvertTaskProtocol) message;
				doConvert(convertTaskProtocol, session);
			}
		}

	}

	private void doConvert(ConvertTaskProtocol convertTaskProtocol,
			IoSession session) {
		ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
		session.write(convertStateProtocol);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		// TODO Auto-generated method stub
		super.messageSent(session, message);
	}

}
