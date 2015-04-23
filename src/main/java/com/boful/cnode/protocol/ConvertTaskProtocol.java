package com.boful.cnode.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;

public class ConvertTaskProtocol {
    public static int OPERATION = Operation.TAG_CONVERT_TASK;

    /** 命令行 */
    private String cmd;

    // 编码
    public IoBuffer toByteArray() throws IOException {
        int count = countLength();
        IoBuffer ioBuffer = IoBuffer.allocate(count);
        ioBuffer.putInt(OPERATION);
        byte[] cmdBuffer = cmd.getBytes("UTF-8");
        ioBuffer.putInt(cmdBuffer.length);
        ioBuffer.put(cmdBuffer);
        return ioBuffer;
    }

    // 解码
    public static ConvertTaskProtocol parse(IoBuffer ioBuffer) throws IOException {
        if (ioBuffer.remaining() < 4) {
            return null;
        }

        int cmdLen = ioBuffer.getInt();
        if (ioBuffer.remaining() != cmdLen) {
            return null;
        }
        ConvertTaskProtocol convertTaskProtocol = new ConvertTaskProtocol();
        byte[] cmdBuffer = new byte[cmdLen];
        ioBuffer.get(cmdBuffer);
        convertTaskProtocol.setCmd(new String(cmdBuffer, "UTF-8"));
        return convertTaskProtocol;
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

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
}
