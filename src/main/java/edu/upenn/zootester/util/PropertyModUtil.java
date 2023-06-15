package edu.upenn.zootester.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyModUtil {
    public static void setNodeNum(int nodeNum) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("test.properties");
        properties.load(inputStream);
        properties.setProperty("nodeNum", String.valueOf(nodeNum));
        String path = ClassLoader.getSystemResource("test.properties").getPath();;
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
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("test.properties");
        properties.load(inputStream);
        properties.setProperty("requestId", String.valueOf(requestId));
        String path = ClassLoader.getSystemResource("test.properties").getPath();;
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
