package org.ofdrw.reader;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * @param zipFile 需要解压的文件
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(File zipFile, String descDir) throws IOException {
        unZipFileByZip4j(zipFile, descDir);
    }

    /**
     * 解压zipFile
     *
     * @param src     带解压的源文件流
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByZip4j(InputStream src, String descDir) throws IOException {
        Path pathFile = Files.createDirectories(Paths.get(descDir));
        try (ZipInputStream zipFile = new ZipInputStream(src, Charset.forName(charset))) {
            LocalFileHeader entry;
            while ((entry = zipFile.getNextEntry()) != null) {
                //校验路径合法性
                Path f;
                try {
                    f = pathFile.resolve(entry.getFileName());
                } catch (InvalidPathException e) {
                    // 尝试使用GBK解析
                    f = pathFile.resolve(new String(entry.getFileName().getBytes(), "GBK"));
                }

                if (!f.startsWith(pathFile)) {
                    throw new IOException(String.format("不合法的路径：%s", f));
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        IOUtils.copy(zipFile, o);
                    }
                }
            }
        }
    }

    public static void unZipFileByZip4j(File zipFile, String descDir) throws IOException {
        try (InputStream inputStream = Files.newInputStream(zipFile.toPath())) {
            unZipFileByZip4j(inputStream, descDir);
        }
    }
}
