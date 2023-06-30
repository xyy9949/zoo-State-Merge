package edu.upenn.zootester.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class PropertyModUtil {
    public static void setNodeNum(int nodeNum) throws IOException {
        Properties properties = new Properties();
        URL url = PropertyModUtil.class.getClassLoader().getResource("test.properties");
        InputStream inputStream = url.openStream();
//        InputStream inputStream = PropertyModUtil.class.getResourceAsStream("test.properties");
        properties.load(inputStream);
        properties.setProperty("nodeNum", String.valueOf(nodeNum));
//        String path = ClassLoader.getSystemResource("test.properties").getPath();
        String path =PropertyModUtil.class.getClassLoader().getResource("test.properties").getPath();
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(path);
            properties.store(fileOutputStream, "comment");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileOutputStream)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setRequestId(int requestId) throws IOException {
        Properties properties = new Properties();
        URL url = PropertyModUtil.class.getClassLoader().getResource("test.properties");
        InputStream inputStream = url.openStream();
//        InputStream inputStream = PropertyModUtil.class.getResourceAsStream("test.properties");
        properties.load(inputStream);
        properties.setProperty("requestId", String.valueOf(requestId));
//        String path = ClassLoader.getSystemResource("test.properties").getPath();
        String path =PropertyModUtil.class.getClassLoader().getResource("test.properties").getPath();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
            properties.store(fileOutputStream, "comment");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileOutputStream)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
