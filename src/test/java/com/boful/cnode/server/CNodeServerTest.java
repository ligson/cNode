package com.boful.cnode.server;

public class CNodeServerTest {

    public static void main(String[] args) {

        // 视频转码
        CNodeClient test1 = new CNodeClient();
        try {
            test1.connect("127.0.0.1", 9000);
            String cmd = "-id job_0001 -i e:/爱情公寓番外篇温酒煮华雄.f4v -o e:/test/bak.mp4 -vb 30000 -ab 20000 -size 300x200";
            test1.send(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 图片转码
        CNodeClient test2 = new CNodeClient();
        try {
            test2.connect("127.0.0.1", 9000);
            String cmd = "-id job_0002 -i e:/Koala.jpg -o e:/test/Koala1.jpg";
            test2.send(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 音频转码
        CNodeClient test3 = new CNodeClient();
        try {
            test3.connect("127.0.0.1", 9000);
            String cmd = "-id job_0003 -i e:/mmd.mp3 -o e:/test/mmd2.wav -ab 20000";
            test3.send(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 文档转码
        CNodeClient test4 = new CNodeClient();
        try {
            test4.connect("127.0.0.1", 9000);
            String cmd = "-id job_0004 -i e:/aaa.txt -o e:/test/aaa2.swf";
            test4.send(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
