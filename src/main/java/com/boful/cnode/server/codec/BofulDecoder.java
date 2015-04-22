package com.boful.cnode.server.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.protocol.Operation;
import com.boful.net.fserver.protocol.TransferProtocol;

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
			}
		}
		inBuffer.reset();
		return false;
	}

}
