package com.boful.cnode.protocol;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;

import com.boful.net.fserver.protocol.DownloadProtocol;

public class ConvertTaskProtocol {
	public static int OPERATION = Operation.TAG_CONVERT_TASK;
	private String cmd;

	// 编码
	public IoBuffer toByteArray() throws IOException {

		int count = countLength();

		IoBuffer ioBuffer = IoBuffer.allocate(count);

		return ioBuffer;
	}

	// 解码
	public static ConvertTaskProtocol parse(IoBuffer ioBuffer)
			throws IOException {

		return null;
	}

	public int countLength() {
		// TAG+CMDBUFFERLEN+CMDBUFFER

		try {
			byte[] cmdBuffer = cmd.getBytes("UTF-8");
			return 4 + 4 + cmdBuffer.length;

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
