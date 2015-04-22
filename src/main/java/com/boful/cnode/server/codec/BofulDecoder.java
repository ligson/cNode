package com.boful.cnode.server.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.boful.cnode.protocol.ConvertStateProtocol;
import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.protocol.Operation;

public class BofulDecoder extends CumulativeProtocolDecoder {

	@Override
	protected boolean doDecode(IoSession session, IoBuffer inBuffer,
			ProtocolDecoderOutput out) throws Exception {
		if (inBuffer.remaining() > 0) {
			inBuffer.mark();
			if (inBuffer.remaining() < 4) {
				inBuffer.reset();
				return false;
			}
			int operation = inBuffer.getInt();
			// 转码任务
			if (operation == Operation.TAG_CONVERT_TASK) {
				ConvertTaskProtocol transferProtocol = ConvertTaskProtocol
						.parse(inBuffer);
				if (transferProtocol == null) {
					inBuffer.reset();
					return false;
				} else {
					out.write(transferProtocol);
					return true;
				}
				
				// 转码状态
			} else if (operation == Operation.TAG_CONVERT_STATE) {
				ConvertStateProtocol convertStateProtocol = ConvertStateProtocol
						.parse(inBuffer);
				if (convertStateProtocol == null) {
					inBuffer.reset();
					return false;
				} else {
					out.write(convertStateProtocol);
					return true;
				}
			}
		}
		inBuffer.reset();
		return false;
	}

}
