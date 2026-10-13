package com.lesaire;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通过真实上传 API 验证行为，使用临时文件与回滚事务隔离运行环境数据。 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:product-image-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.password=", "lesaire.demo-accounts=false",
    "lesaire.jwt.secret=" + ApiTestSupport.TEST_SECRET
})
@AutoConfigureMockMvc @ActiveProfiles("h2") @Transactional
class ProductImageIntegrationTest extends ApiTestSupport {
    private static final String STYLE = "RS-CF-001";
    private static final Path STORAGE = createStorage();
    private String adminToken, customerToken;

    @DynamicPropertySource
    /**
     * 把上传目录切换到本测试的临时目录，避免写入运行环境图片。
     */
    static void uploadStorage(DynamicPropertyRegistry registry) {
        registry.add("lesaire.uploads.directory", () -> STORAGE.toString());
    }

    @BeforeEach
    /**
     * 创建测试管理员和普通用户，并通过真实登录获得令牌。
     */
    void createAccounts() throws Exception {
        accounts.register(OWNER, PASSWORD, "上传测试顾客");
        accounts.createDemo("image-test-admin", "13933333333", PASSWORD, "上传测试管理员", "ADMIN", null);
        adminToken = login("image-test-admin"); customerToken = login(OWNER);
    }

    @AfterEach
    /**
     * 每个测试结束后清理临时目录中的图片。
     */
    void removeTestFiles() throws IOException { deleteChildren(); }

    @AfterAll
    /**
     * 全部图片测试结束后删除本测试创建的临时目录。
     */
    static void removeTestStorage() throws IOException { deleteChildren(); Files.deleteIfExists(STORAGE); }

    @Test
    /**
     * 管理员上传后更新款式地址，公开图片接口返回可解码的 PNG。
     */
    void adminUploadPersistsImageAddressAndServesPublicPng() throws Exception {
        var response = mvc.perform(multipart(uploadUrl(STYLE)).file(realImage("png"))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(STYLE)).andReturn();
        String imageUrl = read(response).get("imageUrl").asText();
        assertThat(imageUrl).matches("/uploads/styles/[0-9a-f-]{36}\\.png");
        assertThat(jdbc.queryForObject("select image_url from shirt_style where style_code=?", String.class, STYLE))
            .isEqualTo(imageUrl);
        assertThat(storedFileCount()).isEqualTo(1);

        byte[] served = mvc.perform(get(imageUrl))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andReturn().getResponse().getContentAsByteArray();
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(served));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(12); assertThat(image.getHeight()).isEqualTo(8);
        var catalog = read(mvc.perform(get("/api/catalog")).andExpect(status().isOk()).andReturn());
        boolean catalogUpdated = false;
        for (var product : catalog.get("products")) {
            if (STYLE.equals(product.get("id").asText())) {
                catalogUpdated = imageUrl.equals(product.get("imageUrl").asText());
            }
        }
        assertThat(catalogUpdated).isTrue();
    }

    @Test
    /**
     * 验证 JPEG 经重新编码后仍以正确类型公开返回。
     */
    void jpegUploadIsDecodedAndServedAsJpeg() throws Exception {
        String url = read(mvc.perform(multipart(uploadUrl(STYLE)).file(realImage("jpg"))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andReturn()).get("imageUrl").asText();
        assertThat(url).matches("/uploads/styles/[0-9a-f-]{36}\\.jpg");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    /**
     * 匿名请求在写入图片文件之前被认证拦截。
     */
    void anonymousUploadIsRejectedBeforeWritingFile() throws Exception {
        mvc.perform(multipart(uploadUrl(STYLE)).file(realImage("png")))
            .andExpect(status().isUnauthorized());
        assertThat(storedFileCount()).isZero();
    }

    @Test
    /**
     * 普通用户无法维护目录照片，数据库地址和本地文件均不改变。
     */
    void adminPathGuardPreventsCustomerChangingCatalogImage() throws Exception {
        String previous = currentImage();
        mvc.perform(multipart(uploadUrl(STYLE)).file(realImage("png"))
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
        assertThat(currentImage()).isEqualTo(previous); assertThat(storedFileCount()).isZero();
    }

    @Test
    void encodedApiPathDoesNotBypassUploadAuthentication() throws Exception {
        URI upload = URI.create("/%61pi/admin/styles/" + STYLE + "/image");
        String previous = currentImage();
        mvc.perform(multipart(upload).file(realImage("png")))
            .andExpect(status().isUnauthorized());
        mvc.perform(multipart(upload).file(realImage("png"))
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
        assertThat(currentImage()).isEqualTo(previous); assertThat(storedFileCount()).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    /**
     * 空文件、SVG 和无法解码的内容不能覆盖款式图片地址。
     */
    void rejectsEmptySvgAndUndecodableFilesWithoutUpdatingStyle(MockMultipartFile file) throws Exception {
        String previous = currentImage();
        mvc.perform(multipart(uploadUrl(STYLE)).file(file)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        assertThat(currentImage()).isEqualTo(previous); assertThat(storedFileCount()).isZero();
    }

    /**
     * 构造各种非法上传内容，参数化覆盖格式校验。
     */
    static Stream<Arguments> invalidFiles() {
        return Stream.of(
            Arguments.of(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])),
            Arguments.of(new MockMultipartFile("file", "picture.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8))),
            Arguments.of(new MockMultipartFile("file", "fake.png", "image/png", "plain text".getBytes(StandardCharsets.UTF_8))),
            Arguments.of(new MockMultipartFile("file", "broken.png", "image/png",
                new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
        );
    }

    @Test
    /**
     * 超过 5 MiB 的图片在保存之前被拒绝。
     */
    void rejectsFileLargerThanFiveMiBWithoutWritingFile() throws Exception {
        var oversized = new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        mvc.perform(multipart(uploadUrl(STYLE)).file(oversized)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isPayloadTooLarge());
        assertThat(storedFileCount()).isZero();
    }

    @Test
    /**
     * 款式不存在时拒绝请求，避免产生没有数据库引用的图片。
     */
    void unknownStyleDoesNotLeaveAnOrphanedImage() throws Exception {
        mvc.perform(multipart(uploadUrl("NO-SUCH-STYLE")).file(realImage("png"))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        assertThat(storedFileCount()).isZero();
    }

    @Test
    /**
     * 已停用款式不能上传或更换照片。
     */
    void inactiveStyleCannotReceiveAnImage() throws Exception {
        jdbc.update("update shirt_style set active=false where style_code=?", STYLE);
        mvc.perform(multipart(uploadUrl(STYLE)).file(realImage("png"))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        assertThat(storedFileCount()).isZero();
    }

    private String currentImage() {
        return jdbc.queryForObject("select image_url from shirt_style where style_code=?", String.class, STYLE);
    }
    private static String uploadUrl(String style) { return "/api/admin/styles/" + style + "/image"; }
    /**
     * 在内存中生成真实可解码图片，避免用伪造扩展名代替有效图片。
     */
    private static MockMultipartFile realImage(String format) throws IOException {
        var image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try { graphics.setColor(new Color(36, 83, 70)); graphics.fillRect(0, 0, 12, 8); }
        finally { graphics.dispose(); }
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, format, bytes);
        return new MockMultipartFile("file", "uniform." + format,
            format.equals("png") ? "image/png" : "image/jpeg", bytes.toByteArray());
    }
    private static Path createStorage() {
        try { return Files.createTempDirectory("lesaire-image-test-"); }
        catch (IOException ex) { throw new ExceptionInInitializerError(ex); }
    }
    private static long storedFileCount() throws IOException {
        try (var paths = Files.walk(STORAGE)) { return paths.filter(Files::isRegularFile).count(); }
    }
    /**
     * 只清理本测试创建的临时图片目录内容。
     */
    private static void deleteChildren() throws IOException {
        if (!Files.exists(STORAGE)) return;
        try (var paths = Files.walk(STORAGE)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(STORAGE)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
