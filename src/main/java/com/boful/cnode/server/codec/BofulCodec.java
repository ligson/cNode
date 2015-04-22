package com.boful.cnode.server.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class BofulCodec implements ProtocolCodecFactory {

	/***
	 * 编码器
	 */
	private BofulEncoder bofulEncoder = new BofulEncoder();
	/***
	 * 解码器
	 */
	private BofulDecoder bofulDecoder = new BofulDecoder();

	@Override
	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		return bofulEncoder;
	}

	@Override
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		return bofulDecoder;
	}

}
