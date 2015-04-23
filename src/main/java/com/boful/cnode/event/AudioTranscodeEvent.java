package com.boful.cnode.event;

import org.apache.mina.core.session.IoSession;

import com.boful.cnode.protocol.ConvertStateProtocol;
import com.boful.convert.core.TranscodeEvent;
import com.boful.convert.model.DiskFile;

public class AudioTranscodeEvent implements TranscodeEvent {

    private IoSession session;

    public AudioTranscodeEvent(IoSession session) {
        this.session = session;
    }

    @Override
    public void onSubmitFail(DiskFile diskFile, String errorMessage, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
        convertStateProtocol.setMessage(errorMessage);
        session.write(convertStateProtocol);
    }

    @Override
    public void onSubmitSuccess(DiskFile diskFile, String jobId) {
        System.out.println("onSubmitSuccess");
    }

    @Override
    public void onStartTranscode(DiskFile diskFile, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
        convertStateProtocol.setMessage("转码开始！");
        session.write(convertStateProtocol);
    }

    @Override
    public void onTranscodeSuccess(DiskFile diskFile, DiskFile destFile, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
        convertStateProtocol.setMessage("文件" + diskFile + "转码完成！");
        session.write(convertStateProtocol);
    }

    @Override
    public void onTranscode(DiskFile diskFile, int process, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
        convertStateProtocol.setMessage("转码进度" + process + "%");
        session.write(convertStateProtocol);
    }

    @Override
    public void onTranscodeFail(DiskFile diskFile, String errorMessage, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
        convertStateProtocol.setMessage(errorMessage);
        session.write(convertStateProtocol);
    }
}
