package com.boful.cnode.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;

public class ConvertStateProtocol {
    public static int OPERATION = Operation.TAG_CONVERT_STATE;

    /** 转码错误 */
    public static int STATE_FAIL = 0;
    /** 转码等待 */
    public static int STATE_WAITING = 1;
    /** 转码中 */
    public static int STATE_CONVERTING = 2;
    /** 转码成功 */
    public static int STATE_SUCCESS = 3;

    private int state;
    private String message;

    // 编码
    public IoBuffer toByteArray() throws IOException {
        int count = countLength();
        IoBuffer ioBuffer = IoBuffer.allocate(count);
        ioBuffer.putInt(OPERATION);
        byte[] messageBuffer = message.getBytes("UTF-8");
        ioBuffer.putInt(state);
        ioBuffer.put(messageBuffer);
        return ioBuffer;
    }

    // 解码
    public static ConvertStateProtocol parse(IoBuffer ioBuffer) throws IOException {

        if (ioBuffer.remaining() < 8) {
            return null;
        }

        int messageLen = ioBuffer.getInt();
        if (ioBuffer.remaining() != (messageLen + 4)) {
            return null;
        }

        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ioBuffer.getInt());
        byte[] messageBuffer = new byte[messageLen];
        ioBuffer.get(messageBuffer);
        convertStateProtocol.setMessage(new String(messageBuffer, "UTF-8"));

        return convertStateProtocol;
    }

    public int countLength() {
        // TAG+MESSAGEBUFFERLEN+STATE+MESSAGEBUFFER
        try {
            byte[] messageBuffer = message.getBytes("UTF-8");
            return 4 + 4 + 4 + messageBuffer.length;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
