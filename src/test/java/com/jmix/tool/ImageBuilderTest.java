package com.jmix.tool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ImageBuilder 测试类 - 生成文章配图
 * 
 * @since 2025-05-03
 */
@Slf4j
public class ImageBuilderTest {

    private static final String ASSETS_DIR = "persum/assets/";

    public static void main(String[] args) throws Exception {
        // String apiKey = System.getenv("OPENAI_API_KEY");
        // if (apiKey == null || apiKey.trim().isEmpty()) {
        // log.warn("OPENAI_API_KEY not set in environment. Use main() method with
        // explicit API key.");
        // return;
        // }

        // String baseUrl = args.length > 1 ? args[1] : "https://code.codingplay.top";
        String apiKey = "sk-4552f3b96e14a7eeba207a41659b258c74961fd15e77683c9ddfec5954ef8f29";
        String baseUrl = "https://code.codingplay.top";
        Files.createDirectories(Paths.get(ASSETS_DIR));

        ImageBuilder builder = new ImageBuilder(apiKey, baseUrl);

        log.info("Starting image generation...");
        // generateCoverImage(builder);
        generateMethodDiagramImage(builder);
        generateCollabSwimlaneImage(builder);

        log.info("All images generated successfully!");
    }

    @Test
    void generateAllImages() throws Exception {
        // 硬编码测试用 API Key
        String apiKey = "sk-4552f3b96e14a7eeba207a41659b258c74961fd15e77683c9ddfec5954ef8f29";
        String baseUrl = "https://code.codingplay.top";
        
        Files.createDirectories(Paths.get(ASSETS_DIR));

        ImageBuilder builder = new ImageBuilder(apiKey, baseUrl);
        generateCoverImage(builder);
        generateMethodDiagramImage(builder);
        generateCollabSwimlaneImage(builder);
        
        log.info("All images generated successfully!");
    }

    private static void generateCoverImage(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: technical article cover
                Primary request: 生成一张横版技术文章封面，表达"复杂配置产品通过本体实例化变得可理解、可测试"。
                Scene/backdrop: clean abstract engineering workspace, no real brand, no company logo
                Subject: 左侧是复杂工业设备的抽象线稿，中间是产品配置元模型的节点网络，右侧是电脑、硬盘、T 恤等简单隐喻对象，三者通过柔和发光连线连接
                Style/medium: modern technical editorial illustration, polished but restrained
                Composition/framing: wide 16:9 layout, generous whitespace, suitable for WeChat/Zhihu article header
                Color palette: blue-gray base with small warm orange accents
                Text: no text
                Constraints: no brand marks, no real company names, no UI screenshots, no watermark
                """;

        String outputPath = ASSETS_DIR + "01-封面图.png";
        builder.generateImage(prompt, outputPath);
        log.info("封面图已生成: {}", outputPath);
    }

    private static void generateMethodDiagramImage(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: 生成一张三段式方法图，表达"看山是山 -> 看山不是山 -> 看山还是山"。
                Subject: 第一段是真实复杂设备场景，第二段是抽象模型节点和关系线，第三段是电脑硬盘等简单案例变成绿色测试通过标记
                Style/medium: clean technical infographic illustration, not cartoonish
                Composition/framing: horizontal three-panel composition, each panel visually distinct but connected
                Color palette: neutral gray and blue, green only for test-pass signal
                Text: no text
                Constraints: no brand, no company logo, no realistic confidential equipment, no watermark
                """;

        String outputPath = ASSETS_DIR + "02-三段式方法图.png";
        builder.generateImage(prompt, outputPath);
        log.info("三段式方法图已生成: {}", outputPath);
    }

    private static void generateCollabSwimlaneImage(ImageBuilder builder) throws Exception {
        String prompt = """
                Use case: infographic-diagram
                Asset type: article section illustration
                Primary request: 生成一张团队协作场景图，表达业务、BA、开发、测试、AI 围绕同一个产品配置元模型协作。
                Subject: 五个角色：业务专家、BA/领域建模、开发、测试、AI 助手；他们围绕一张白板，白板上是抽象节点模型和测试通过标记
                Style/medium: modern workplace technical illustration, professional and calm
                Composition/framing: medium-wide shot, whiteboard centered, roles arranged around it
                Color palette: blue-gray, white, small orange highlights
                Text: no readable text
                Constraints: no brand marks, no company names, no exaggerated cartoon style, no watermark
                """;

        String outputPath = ASSETS_DIR + "03-协作泳道图.png";
        builder.generateImage(prompt, outputPath);
        log.info("协作泳道图已生成: {}", outputPath);
    }
}
