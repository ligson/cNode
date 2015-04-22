package com.boful.cnode.server.codec;

import java.lang.reflect.Method;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class BofulEncoder extends ProtocolEncoderAdapter {

	@Override
	public void encode(IoSession session, Object message,
			ProtocolEncoderOutput out) throws Exception {
		Method method = null;
		try {
			method = message.getClass().getMethod("toByteArray");
		} catch (Exception e) {
		}
		if (method != null) {
			Object object = method.invoke(message);
			if (object instanceof IoBuffer) {
				IoBuffer ioBuffer2 = (IoBuffer) object;
				ioBuffer2.flip();
				out.write(ioBuffer2);

			}
		} else {
			if (message instanceof IoBuffer) {
				IoBuffer ioBuffer2 = (IoBuffer) message;
				ioBuffer2.flip();
				out.write(ioBuffer2);
			}
		}
	}

}
