package com.boful.cnode.server;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.boful.cnode.protocol.ConvertStateProtocol;
import com.boful.cnode.protocol.ConvertTaskProtocol;
import com.boful.cnode.protocol.Operation;
import com.boful.common.file.utils.FileType;

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
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
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
				ConvertStateProtocol convertStateProtocol = doConvert(convertTaskProtocol);
				session.write(convertStateProtocol);
			}
		}
	}

	private ConvertStateProtocol doConvert(
			ConvertTaskProtocol convertTaskProtocol) {
		// 获取命令行
		String[] cmdArgs = convertTaskProtocol.getCmd().split(" ");

		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("vb", "videoBitrate", false, "");
		options.addOption("ab", "audioBitrate", false, "");
		options.addOption("size", "size", false, "");
		options.addOption("disk", "diskFile", true, "");
		options.addOption("dest", "destFile", true, "");

		ConvertStateProtocol convertStateProtocol = new ConvertStateProtocol();
		try {
			CommandLine commandLine = parser.parse(options, cmdArgs);

			// 元文件
			File diskFile = null;
			if (commandLine.hasOption("disk")) {
				String arg = commandLine.getOptionValue("disk");
				diskFile = new File(arg);
				if (!diskFile.exists()) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("文件" + arg + "不存在！");
					return convertStateProtocol;
				}
			} else {
				convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
				convertStateProtocol.setMessage("没有设置disk参数！");
				return convertStateProtocol;
			}

			// 转码文件
			File destFile = null;
			if (commandLine.hasOption("dest")) {
				String arg = commandLine.getOptionValue("dest");
				destFile = new File(arg);
				File path = new File(destFile.getParent());
				if (!path.exists()) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("文件" + arg + "的路径错误！");
					return convertStateProtocol;
				}
			} else {
				convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
				convertStateProtocol.setMessage("没有设置dest参数！");
				return convertStateProtocol;
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
						convertStateProtocol
								.setState(ConvertStateProtocol.STATE_FAIL);
						convertStateProtocol.setMessage("参数vb的值必须是正数！");
						return convertStateProtocol;
					}
				} catch (NumberFormatException e) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("参数vb的值必须是整数！");
					return convertStateProtocol;
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
						convertStateProtocol
								.setState(ConvertStateProtocol.STATE_FAIL);
						convertStateProtocol.setMessage("参数ab的值必须是正数！");
						return convertStateProtocol;
					}
				} catch (NumberFormatException e) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("参数ab的值必须是整数！");
					return convertStateProtocol;
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
						convertStateProtocol
								.setState(ConvertStateProtocol.STATE_FAIL);
						convertStateProtocol.setMessage("参数size的格式错误！");
						return convertStateProtocol;
					}

					try {
						width = Integer.parseInt(array[0]);
						height = Integer.parseInt(array[1]);

						if (width < 0 || height < 0) {
							convertStateProtocol
									.setState(ConvertStateProtocol.STATE_FAIL);
							convertStateProtocol.setMessage("参数size的值错误！");
							return convertStateProtocol;
						}

					} catch (NumberFormatException e) {
						convertStateProtocol
								.setState(ConvertStateProtocol.STATE_FAIL);
						convertStateProtocol.setMessage("参数size的值错误！");
						return convertStateProtocol;
					}
				}
			}

			// 视频转码相关参数验证
			if (FileType.isVideo(diskFile.getName())) {
				if (videoBitrate == 0 || audioBitrate == 0 || width == 0
						|| height == 0) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("视频文件转码必须设置参数:vb、ab和size！");
					return convertStateProtocol;
				}
				if (FileType.isVideo(destFile.getName())) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("参数dest不是视频文件！");
					return convertStateProtocol;
				}
			}

			// 音频转码相关参数验证
			if (FileType.isAudio(diskFile.getName())) {
				if (audioBitrate == 0) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("音频文件转码必须设置参数:vb、ab和size！");
					return convertStateProtocol;
				}
				if (FileType.isVideo(destFile.getName())) {
					convertStateProtocol
							.setState(ConvertStateProtocol.STATE_FAIL);
					convertStateProtocol.setMessage("参数dest不是音频文件！");
					return convertStateProtocol;
				}
			}

		} catch (ParseException e) {
			convertStateProtocol.setState(ConvertStateProtocol.STATE_FAIL);
			convertStateProtocol.setMessage("参数错误！");
		}

		return convertStateProtocol;
	}

}
