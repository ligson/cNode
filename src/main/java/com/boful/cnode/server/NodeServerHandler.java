package com.boful.cnode.server;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.boful.cnode.event.AudioTranscodeEvent;
import com.boful.cnode.utils.ConvertProviderUtils;
import com.boful.common.file.utils.FileType;
import com.boful.common.file.utils.FileUtils;
import com.boful.convert.core.impl.utils.ImageMagickUtils;
import com.boful.convert.model.DiskFile;
import com.boful.net.cnode.protocol.ConvertStateProtocol;
import com.boful.net.cnode.protocol.ConvertTaskProtocol;
import com.boful.net.cnode.protocol.Operation;
import com.boful.net.utils.CommandLineUtils;

public class NodeServerHandler extends IoHandlerAdapter {
    private Set<IoSession> sessions = new HashSet<IoSession>();
    private static Logger logger = Logger.getLogger(NodeServerHandler.class);

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        sessions.remove(session);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
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

    private void doConvert(ConvertTaskProtocol convertTaskProtocol, IoSession session) {

        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        try {
            // 命令行解析
            Map<String, String> commandMap = CommandLineUtils.parse(convertTaskProtocol.getCmd());
            if (commandMap == null) {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("命令行" + convertTaskProtocol.getCmd() + "错误！");
                session.write(convertStateProtocol);
                return;
            }

            String checkMsg = CommandLineUtils.checkCmd(commandMap);
            if (checkMsg != null) {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage(checkMsg);
                session.write(convertStateProtocol);
                return;
            }

            // jobId
            String jobId = commandMap.get("jobid");
            // 元文件
            File diskFile = new File(commandMap.get("diskFile"));
            // 转码文件
            File destFile = new File(commandMap.get("destFile"));
            if (!destFile.exists()) {
                destFile.getParentFile().mkdirs();
            }
            // 视频码率
            int videoBitrate = Integer.parseInt(commandMap.get("videoBitrate"));

            // 音频码率
            int audioBitrate = Integer.parseInt(commandMap.get("audioBitrate"));

            // 视频的宽度和高度
            int width = 0;
            int height = 0;
            String size = commandMap.get("size");
            if (size != null) {
                String[] array = size.split("x");
                width = Integer.parseInt(array[0]);
                height = Integer.parseInt(array[1]);
            }

            String diskSufix = FileUtils.getFileSufix(diskFile.getName());
            diskSufix = diskSufix.toUpperCase();
            session.setAttribute("destFile", destFile.getAbsolutePath());
            // 视频转码
            if (FileType.isVideo(diskFile.getName())) {
                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcodeVideo(new DiskFile(diskFile),
                        new DiskFile(destFile), width, height, videoBitrate, audioBitrate, event, jobId);

                // 音频转码
            } else if (FileType.isAudio(diskFile.getName())) {

                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcodeAudio(new DiskFile(diskFile),
                        new DiskFile(destFile), audioBitrate, event, jobId);

                // 文档转码
            } else if (FileType.isDocument(diskFile.getName()) || diskSufix.equals("PDF") || diskSufix.equals("SWF")) {
                String destSufix = FileUtils.getFileSufix(destFile.getName());
                destSufix = destSufix.toUpperCase();
                // 都是PDF文件，不需要转码
                if (diskSufix.equals("PDF") && destSufix.equals("PDF")) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
                    convertStateProtocol.setMessage(diskFile.getAbsolutePath());
                    session.write(convertStateProtocol);

                    // 都是SWF文件，不需要转码
                } else if (diskSufix.equals("SWF") && destSufix.equals("SWF")) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
                    convertStateProtocol.setMessage(diskFile.getAbsolutePath());
                    session.write(convertStateProtocol);

                } else {
                    // 转码为SWF文件，被转码文件只能是PDF文件
                    if (destSufix.equals("SWF")) {
                        if (diskSufix.equals("PDF")) {
                            // 转码开始
                            AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                            ConvertProviderUtils.getBofulConvertProvider().transcode2SWF(new DiskFile(diskFile),
                                    new DiskFile(destFile), event, jobId);

                        } else {
                            // 转码开始
                            File pdfFile = new File(destFile.getParent(), jobId + ".pdf");
                            ConvertProviderUtils.getBofulConvertProvider().transcode2PDF(new DiskFile(diskFile),
                                    new DiskFile(pdfFile), null, jobId);

                            AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                            ConvertProviderUtils.getBofulConvertProvider().transcode2SWF(new DiskFile(diskFile),
                                    new DiskFile(destFile), event, jobId);
                        }
                    }

                    // 转码为PDF文件，被转码文件只能是SWF和PDF以外的文件
                    if (destSufix.equals("PDF")) {
                        // 转码开始
                        AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                        ConvertProviderUtils.getBofulConvertProvider().transcode2PDF(new DiskFile(diskFile),
                                new DiskFile(destFile), event, jobId);
                    }
                }

                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcode2PDF(new DiskFile(diskFile),
                        new DiskFile(destFile), event, jobId);

                // 图片转码
            } else if (FileType.isImage(diskFile.getName())) {
                String imageMagickBaseHome = ConvertProviderUtils.getConfig().getHosts().get(0).getParams()
                        .get("imageMagickSearchPath");
                ImageMagickUtils.compress(diskFile, destFile, imageMagickBaseHome);
                convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
                convertStateProtocol.setMessage(destFile.getAbsolutePath());
                session.write(convertStateProtocol);
            }

        } catch (Exception e) {
            convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
            convertStateProtocol.setMessage(e.getMessage());
            session.write(convertStateProtocol);
            return;
        }
    }
}
