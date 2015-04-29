package com.boful.cnode.server;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
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
        System.out.println("connect ......");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        sessions.remove(session);
        System.out.println("disconnect ......");
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
            CommandLine commandLine = CommandLineUtils.parse(convertTaskProtocol.getCmd());
            if (commandLine == null) {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("命令行" + convertTaskProtocol.getCmd() + "错误！");
                session.write(convertStateProtocol);
                return;
            }

            // jobId
            String jobId = null;
            if (commandLine.hasOption("id")) {
                jobId = commandLine.getOptionValue("id");
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置jobId！");
                session.write(convertStateProtocol);
                return;
            }

            // 元文件
            File diskFile = null;
            if (commandLine.hasOption("i")) {
                diskFile = new File(commandLine.getOptionValue("i"));
                if (!diskFile.exists()) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("文件" + diskFile.getAbsolutePath() + "不存在！");
                    session.write(convertStateProtocol);
                    return;
                }
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置diskFile！");
                session.write(convertStateProtocol);
                return;
            }

            // 转码文件
            File destFile = null;
            if (commandLine.hasOption("o")) {
                destFile = new File(commandLine.getOptionValue("o"));
                File parent = new File(destFile.getParent());
                // 创建文件夹失败
                if (!parent.mkdirs()) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("文件" + destFile.getAbsolutePath() + "的路径错误！");
                    session.write(convertStateProtocol);
                    return;
                }
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置destFile！");
                session.write(convertStateProtocol);
                return;
            }

            // 视频码率
            int videoBitrate = 0;
            if (commandLine.hasOption("vb")) {
                String arg = commandLine.getOptionValue("vb");
                try {
                    if (arg != null && arg.length() > 0) {
                        videoBitrate = Integer.parseInt(arg);
                    }
                    if (videoBitrate < 0) {
                        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                        convertStateProtocol.setMessage("参数videoBitrate的值必须是正数！");
                        session.write(convertStateProtocol);
                        return;
                    }
                } catch (NumberFormatException e) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数videoBitrate的值必须是整数！");
                    session.write(convertStateProtocol);
                    return;
                }
            }

            // 音频码率
            int audioBitrate = 0;
            if (commandLine.hasOption("ab")) {
                String arg = commandLine.getOptionValue("ab");
                try {
                    if (arg != null && arg.length() > 0) {
                        audioBitrate = Integer.parseInt(arg);
                    }
                    if (audioBitrate < 0) {
                        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                        convertStateProtocol.setMessage("参数audioBitrate的值必须是正数！");
                        session.write(convertStateProtocol);
                        return;
                    }
                } catch (NumberFormatException e) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数audioBitrate的值必须是整数！");
                    session.write(convertStateProtocol);
                    return;
                }
            }

            // 视频的宽度和高度
            int width = 0;
            int height = 0;
            if (commandLine.hasOption("size")) {
                String arg = commandLine.getOptionValue("size");
                if (arg != null && arg.length() > 0) {
                    String[] array = arg.split("x");
                    if (array.length != 2) {
                        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                        convertStateProtocol.setMessage("参数size的值错误！");
                        session.write(convertStateProtocol);
                        return;
                    }

                    try {
                        width = Integer.parseInt(array[0]);
                        height = Integer.parseInt(array[1]);

                        if (width < 0 || height < 0) {
                            convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                            convertStateProtocol.setMessage("参数size的值错误！");
                            session.write(convertStateProtocol);
                            return;
                        }

                    } catch (NumberFormatException e) {
                        convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                        convertStateProtocol.setMessage("参数size的值错误！");
                        session.write(convertStateProtocol);
                        return;
                    }
                }
            }

            String diskSufix = FileUtils.getFileSufix(diskFile.getName());
            diskSufix = diskSufix.toUpperCase();
            session.setAttribute("destFile", destFile.getAbsolutePath());
            // 视频转码
            if (FileType.isVideo(diskFile.getName())) {
                if (videoBitrate == 0 || audioBitrate == 0 || width == 0 || height == 0) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("视频文件转码必须设置参数:videoBitrate、audioBitrate和size！");
                    session.write(convertStateProtocol);
                    return;
                }
                if (!FileType.isVideo(destFile.getName())) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数destFile不是视频文件！");
                    session.write(convertStateProtocol);
                    return;
                }

                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcodeVideo(new DiskFile(diskFile),
                        new DiskFile(destFile), width, height, videoBitrate, audioBitrate, event, jobId);

                // 音频转码
            } else if (FileType.isAudio(diskFile.getName())) {
                if (audioBitrate == 0) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("音频文件转码必须设置参数:audioBitrate！");
                    session.write(convertStateProtocol);
                    return;
                }
                if (!FileType.isAudio(destFile.getName())) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数destFile不是音频文件！");
                    session.write(convertStateProtocol);
                    return;
                }

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
                        if (diskSufix.equals("SWF")) {
                            convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                            convertStateProtocol.setMessage("不能将SWF文件转换为PDF文件！");
                            session.write(convertStateProtocol);
                            return;

                        } else {
                            // 转码开始
                            AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                            ConvertProviderUtils.getBofulConvertProvider().transcode2PDF(new DiskFile(diskFile),
                                    new DiskFile(destFile), event, jobId);
                        }
                    }
                }

                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcode2PDF(new DiskFile(diskFile),
                        new DiskFile(destFile), event, jobId);

                // 图片转码
            } else if (FileType.isImage(diskFile.getName())) {
                if (!FileType.isImage(destFile.getName())) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数o不是图片文件！");
                    session.write(convertStateProtocol);
                    return;
                }

                String imageMagickBaseHome = ConvertProviderUtils.getConfig().getHosts().get(0).getParams()
                        .get("imageMagickSearchPath");
                ImageMagickUtils.compress(diskFile, destFile, imageMagickBaseHome);
                convertStateProtocol.setState(ConvertStateProtocol.STATE_SUCCESS);
                convertStateProtocol.setMessage(destFile.getAbsolutePath());
                session.write(convertStateProtocol);

                // 其他类型文件
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("转码的文件类型错误，只能是视频、音频、文档和图片！");
                session.write(convertStateProtocol);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
            convertStateProtocol.setMessage(e.getMessage());
            session.write(convertStateProtocol);
            return;
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        super.messageSent(session, message);
    }
}
