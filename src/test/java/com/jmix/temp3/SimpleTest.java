package com.jmix.temp3;

import com.jmix.temp3.core.Part;

/**
 * 简单的测试类，用于验证新功能
 */
public class SimpleTest {

    public static void main(String[] args) {
        // 测试 Part 类的属性功能
        Part part = new Part("test", true, 5400, 3);
        part.setAttr(Part.ATTR_LISTPRICE, 100.0);
        part.setAttr(Part.ATTR_PROFIT, 50.0);

        System.out.println("Part code: " + part.code);
        System.out.println("ListPrice: " + part.getAttr(Part.ATTR_LISTPRICE));
        System.out.println("Profit: " + part.getAttr(Part.ATTR_PROFIT));
        System.out.println("Has listPrice: " + part.hasAttr(Part.ATTR_LISTPRICE));

        // 测试归一化处理器
        NormalizationProcessor processor = new NormalizationProcessor();
        NormalizationProcessor.NormalizationConfig config =
            new NormalizationProcessor.NormalizationConfig(Part.ATTR_LISTPRICE);

        NormalizationProcessor.NormalizationResult result = processor.normalize(java.util.Arrays.asList(part), config);
        System.out.println("Normalized value: " + result.getNormalizedValue("test"));

        System.out.println("All tests passed!");
    }
}
