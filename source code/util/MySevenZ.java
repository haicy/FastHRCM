package cn.hpc.util;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import java.io.*;

public class MySevenZ {

    public static void compress(SevenZOutputFile sevenZOutput, File[] files) {
        try {
            for (File orgFile : files) {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(orgFile));
                SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(orgFile, orgFile.getName());
                sevenZOutput.putArchiveEntry(entry);

                int len;
                byte[] buffer = new byte[1024];
                while ((len = bis.read(buffer)) != -1) {
                    sevenZOutput.write(buffer, 0, len);
                }
                sevenZOutput.closeArchiveEntry();
                bis.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void decompress(String orgPath, String desPath) {
        File orgFile = new File(orgPath);
        if (!orgFile.exists()) {
            throw new MyException(orgFile.getPath() + "所指文件不存在");
        }

        SevenZArchiveEntry entry;
        File file;
        try {
            SevenZFile sevenZFile = new SevenZFile(orgFile);
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    file = new File(desPath, entry.getName());
                    if (!file.exists()) {
                        new File(file.getParent()).mkdirs();
                    }
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = sevenZFile.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    bos.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
