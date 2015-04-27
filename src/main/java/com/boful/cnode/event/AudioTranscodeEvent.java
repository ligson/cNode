package com.boful.cnode.event;

import org.apache.mina.core.session.IoSession;

import com.boful.net.cnode.protocol.ConvertStateProtocol;
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
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_WAITING);
        convertStateProtocol.setMessage("文件" + diskFile.getAbsolutePath() + "转码等待！");
        session.write(convertStateProtocol);
    }

    @Override
    public void onStartTranscode(DiskFile diskFile, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_START);
        convertStateProtocol.setMessage("文件" + diskFile.getAbsolutePath() + "转码开始！");
        session.write(convertStateProtocol);
    }

    @Override
    public void onTranscodeSuccess(DiskFile diskFile, DiskFile destFile, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
        convertStateProtocol.setMessage(session.getAttribute("destFile").toString());
        session.write(convertStateProtocol);
    }

    @Override
    public void onTranscode(DiskFile diskFile, int process, String jobId) {
        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        if (process == 100) {
            convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
            convertStateProtocol.setMessage(session.getAttribute("destFile").toString());
        } else {
            convertStateProtocol.setState(ConvertStateProtocol.STATE_CONVERTING);
            convertStateProtocol.setMessage("文件" + diskFile.getAbsolutePath() + "转码进度:" + process + "%");
        }
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
