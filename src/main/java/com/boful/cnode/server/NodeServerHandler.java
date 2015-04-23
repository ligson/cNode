package com.boful.cnode.server;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.boful.cnode.event.AudioTranscodeEvent;
import com.boful.cnode.protocol.ConvertStateProtocol;
import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.protocol.Operation;
import com.boful.cnode.utils.ConvertProviderUtils;
import com.boful.common.file.utils.FileType;
import com.boful.common.file.utils.FileUtils;
import com.boful.convert.core.impl.utils.ImageMagickUtils;
import com.boful.convert.model.DiskFile;

public class NodeServerHandler extends IoHandlerAdapter {
    private Set<IoSession> sessions = new HashSet<IoSession>();
    private static Logger logger = Logger.getLogger(NodeServerHandler.class);

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        sessions.add(session);
        System.out.println("connect ......");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
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
        // 获取命令行
        String[] cmdArgs = StringUtils.split(convertTaskProtocol.getCmd(), " ");

        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("i", "diskFile", true, "");
        options.addOption("o", "destFile", true, "");
        options.addOption("vb", "videoBitrate", true, "");
        options.addOption("ab", "audioBitrate", true, "");
        options.addOption("size", "size", true, "");
        options.addOption("id", "jobId", true, "");

        ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
        try {
            CommandLine commandLine = parser.parse(options, cmdArgs);

            // jobId
            String jobId = null;
            if (commandLine.hasOption("id")) {
                jobId = commandLine.getOptionValue("id");
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置id参数！");
                session.write(convertStateProtocol);
                return;
            }

            // 元文件
            File diskFile = null;
            if (commandLine.hasOption("i")) {
                String arg = commandLine.getOptionValue("i");
                diskFile = new File(arg);
                if (!diskFile.exists()) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("文件" + arg + "不存在！");
                    session.write(convertStateProtocol);
                    return;
                }
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置i参数！");
                session.write(convertStateProtocol);
                return;
            }

            // 转码文件
            File destFile = null;
            if (commandLine.hasOption("o")) {
                String arg = commandLine.getOptionValue("o");
                destFile = new File(arg);
                File path = new File(destFile.getParent());
                if (!path.exists()) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("文件" + arg + "的路径错误！");
                    session.write(convertStateProtocol);
                    session.write(convertStateProtocol);
                    return;
                }
            } else {
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("没有设置o参数！");
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
                        convertStateProtocol.setMessage("参数vb的值必须是正数！");
                        session.write(convertStateProtocol);
                        return;
                    }
                } catch (NumberFormatException e) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数vb的值必须是整数！");
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
                        convertStateProtocol.setMessage("参数ab的值必须是正数！");
                        session.write(convertStateProtocol);
                        return;
                    }
                } catch (NumberFormatException e) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数ab的值必须是整数！");
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
                        convertStateProtocol.setMessage("参数size的格式错误！");
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

            // 视频转码
            if (FileType.isVideo(diskFile.getName())) {
                if (videoBitrate == 0 || audioBitrate == 0 || width == 0 || height == 0) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("视频文件转码必须设置参数:vb、ab和size！");
                    session.write(convertStateProtocol);
                    return;
                }
                if (!FileType.isVideo(destFile.getName())) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数o不是视频文件！");
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
                    convertStateProtocol.setMessage("音频文件转码必须设置参数:vb、ab和size！");
                    session.write(convertStateProtocol);
                    return;
                }
                if (!FileType.isAudio(destFile.getName())) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数o不是音频文件！");
                    session.write(convertStateProtocol);
                    return;
                }

                // 转码开始
                AudioTranscodeEvent event = new AudioTranscodeEvent(session);
                ConvertProviderUtils.getBofulConvertProvider().transcodeAudio(new DiskFile(diskFile),
                        new DiskFile(destFile), audioBitrate, event, jobId);

                // 文档转码
            } else if (FileType.isDocument(diskFile.getName())) {
                String sufix = FileUtils.getFileSufix(destFile.getName());
                if (!sufix.toUpperCase().equals("PDF")) {
                    convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                    convertStateProtocol.setMessage("参数o不是PDF文件！");
                    session.write(convertStateProtocol);
                    return;
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
                convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
                convertStateProtocol.setMessage("文件" + diskFile + "转码成功！");
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
