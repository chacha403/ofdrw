package org.ofdrw.reader;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * ZIP 文件解压工具
 */
public class ZipUtil {
    /**
     * 设置 解压许可最大字节数
     *
     * @param size 压缩文件解压最大大小,默认值： 100M
     * @deprecated 采用apache compress 默认策略
     */
    @Deprecated
    public static void setMaxSize(long size) {
    }

    /**
     * 设置解压默认字符集
     * <p>
     * 用于解决中文字符集乱码问题
     *
     * @param charset 字符集，如 GBK、UTF8 等
     */
    public static void setDefaultCharset(String charset) {
        ZipUtil.charset = charset;
    }

    /**
     * 默认字符集：GBK
     * 若ZIP压缩包中有默认字符集，则以压缩包中的字符集为准
     */
    private static String charset = "UTF-8";

    /**
     * 解压到指定目录
     *
     * @param zipPath 需要解压的文件路径
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(String zipPath, String descDir) throws IOException {
        unZipFiles(new File(zipPath), descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param src     压缩文件流
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(InputStream src, String encoding, String descDir) throws IOException {
        unzipInputStreamByZip4j(src, encoding, descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param zipFile 需要解压的文件
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(File zipFile, String descDir) throws IOException {
        unzipInput(zipFile, descDir);
    }

    /**
     * 使用apache common compress库 解压zipFile，能支持更多zip包解压的特性
     *
     * @param srcFile 带解压的源文件
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unzipInput(File srcFile, String descDir) throws IOException {
        if (srcFile == null || !srcFile.exists()) {
            throw new IOException("解压文件不存在: " + srcFile);
        }
        try (FileInputStream fin = new FileInputStream(srcFile)) {
            //String zipEncoding = zipEncoding(srcFile);
            unzipInputStreamByZip4j(fin, null, descDir);
        } catch (Exception e) {
            // 使用linux系统自带的unzip命令解压
            String cmd = String.format("/bin/sh -c unzip -o %s -d %s", srcFile.getAbsolutePath(), descDir);
            Process process = Runtime.getRuntime().exec(cmd);
            try {
                int exitCode = process.waitFor();
            } catch (InterruptedException e1) {
                throw new IOException("解压文件异常", e1);
            }
        }
    }

    /**
     * 解压zipFile
     *
     * @param inputStream 带解压的源文件流
     * @param descDir     解压到目录
     * @throws IOException IO异常
     */
    public static void unzipInputStreamByZip4j(InputStream inputStream, String charset, String descDir) throws IOException {
        Path pathFile = Files.createDirectories(Paths.get(descDir));
        Charset zipCharset = charset == null ? null : Charset.forName(charset);
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, zipCharset)) {
            LocalFileHeader fileHeader = null;
            while ((fileHeader = zipInputStream.getNextEntry()) != null) {
                Path f = null; //校验路径合法性
                try {
                    f = pathFile.resolve(fileHeader.getFileName());
                } catch (InvalidPathException e) {
                    throw new IOException(String.format("ZipEntry文件名乱码：%s", fileHeader.getFileName()));
                }
                if (!f.startsWith(pathFile)) {
                    throw new IOException(String.format("不合法的路径：%s", f));
                }
                if (fileHeader.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        IOUtils.copy(zipInputStream, o);
                    }
                }
            }
        } catch (ZipException e) {
            throw new IOException("解压文件异常", e);
        }
    }

    /**
     * 判断该使用哪种编码方式解压
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static String zipEncoding(File file) throws Exception {
        String encoding = "GBK";
        try (ZipFile zipFile = new ZipFile(file)) {
            zipFile.setCharset(Charset.forName(encoding));
            List<FileHeader> headers = zipFile.getFileHeaders();
            for (FileHeader fileHeader : headers) {
                String fileName = fileHeader.getFileName();
                if (isMessyCode(fileName)) {
                    encoding = "UTF-8";
                    break;
                }
            }
            return encoding;
        }
    }

    private static boolean isMessyCode(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 当从Unicode编码向某个字符集转换时，如果在该字符集中没有对应的编码，则得到0x3f（即问号字符?）
            // 从其他字符集向Unicode编码转换时，如果这个二进制数在该字符集中没有标识任何的字符，则得到的结果是0xfffd
            if ((int) c == 0xfffd) {
                // 存在乱码
                return true;
            }
        }
        return false;
    }
}
