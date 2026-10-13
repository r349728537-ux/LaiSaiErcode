package com.lesaire.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 本地款式图片存储：只接收解码后的位图，文件路径由服务器决定。 */
@Component
public class ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);
    /**
     * 上传文件最大为 5 MB，解码前先限制文件大小。
     */
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path directory;

    public ImageStorage(@Value("${lesaire.uploads.directory:.local/uploads/styles}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    /**
     * 只接收可解码的 JPG/PNG，检查像素尺寸后重新编码，并使用随机文件名保存。
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("请选择要上传的图片");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "图片不能超过 5 MB");
        BufferedImage image;
        String format;
        try (var input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw bad("仅支持有效的 JPG 或 PNG 图片");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw bad("仅支持有效的 JPG 或 PNG 图片");
            ImageReader reader = readers.next();
            try {
                format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) throw bad("仅支持 JPG 或 PNG 图片");
                reader.setInput(stream, true, true);
                // 在读取完整像素前限制宽高和总像素，控制解码所需的内存。
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 6000 || height > 6000 || (long) width * height > 12_000_000) {
                    throw bad("图片尺寸过大，请缩小至 1200 万像素以内，且单边不超过 6000 像素");
                }
                image = reader.read(0);
                if (image == null) throw bad("图片无法读取，请换一张图片");
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw bad("图片文件损坏或格式不正确，请换一张 JPG 或 PNG 图片");
        }
        String extension = format.equals("jpeg") ? "jpg" : "png";
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = directory.resolve(filename);
        try {
            Files.createDirectories(directory);
            // 重新编码图片，去掉原始上传文件的元数据和附加字节；JPG 转成白底 RGB。
            if (format.equals("jpeg")) {
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = rgb.createGraphics();
                try { graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight()); graphics.drawImage(image, 0, 0, null); }
                finally { graphics.dispose(); }
                image = rgb;
            }
            if (!ImageIO.write(image, format, destination.toFile())) throw new IOException("No image encoder");
            return filename;
        } catch (IOException ex) {
            delete(filename);
            log.error("Unable to save uploaded style photo", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "图片暂时无法保存，请稍后重试");
        }
    }

    /**
     * 只允许服务器生成的 UUID 图片名，并确认规范化后的路径仍在上传目录内。
     */
    public Path find(String filename) {
        if (filename == null || !filename.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(png|jpg)")) throw missing();
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw missing();
        return file;
    }

    /**
     * 清理未成功绑定数据库地址的图片，删除失败时记录日志。
     */
    public void delete(String filename) {
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory)) return;
        try { Files.deleteIfExists(path); }
        catch (IOException ex) { log.warn("Unable to clean up unlinked style image {}", filename); }
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"); }
}
