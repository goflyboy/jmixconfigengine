package com.jmix.configengine.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.impl.ModuleRefRelationGraph;
import com.jmix.executor.impl.util.Pair;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ModuleRefRelationGraph 测试类
 * 测试模块引用关系图的各种功能
 * 
 * @since 2025-09-23
 */
public class ModuleRefRelationGraphTest {
    private ModuleRefRelationGraph graph;

    private RefProgObjSchema p0;

    private RefProgObjSchema p11;

    private RefProgObjSchema p21;

    private RefProgObjSchema p22;

    private RefProgObjSchema pt1;

    private RefProgObjSchema pt2;

    /**
     * 设置测试环境
     */
    @Before
    public void setUp() {
        graph = new ModuleRefRelationGraph();

        // 创建测试用的编程对象
        p0 = createRefProgObjSchema("Para", "P0");
        p11 = createRefProgObjSchema("Para", "P11");
        p21 = createRefProgObjSchema("Para", "P21");
        p22 = createRefProgObjSchema("Para", "P22");
        pt1 = createRefProgObjSchema("Part", "PT1");
        pt2 = createRefProgObjSchema("Part", "PT2");
    }

    /**
     * 创建RefProgObjSchema的辅助方法
     */
    private RefProgObjSchema createRefProgObjSchema(String type, String code) {
        RefProgObjSchema schema = new RefProgObjSchema();
        schema.setProgObjType(type);
        schema.setProgObjCode(code);
        return schema;
    }

    /**
     * 测试添加单一关系
     */
    @Test
    public void testAddSingleRelation() {
        // 执行
        graph.add("Rule01", p0, p11);

        // 验证
        assertTrue(graph.containsNode("P0"));
        assertTrue(graph.containsNode("P11"));
        assertEquals(2, graph.getAllNodes().size());

        Map<String, String> outEdges = graph.getOutEdges("P0");
        assertEquals(1, outEdges.size());
        assertEquals("Rule01", outEdges.get("P11"));

        Map<String, String> inEdges = graph.getInEdges("P11");
        assertEquals(1, inEdges.size());
        assertEquals("Rule01", inEdges.get("P0"));
    }

    /**
     * 测试添加多个左侧关系
     */
    @Test
    public void testAddMultipleFromLeftsRelation() {
        // 执行：Rule21: From(left):[P21,P22],To(Right):PT2
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 验证节点
        assertTrue(graph.containsNode("P21"));
        assertTrue(graph.containsNode("P22"));
        assertTrue(graph.containsNode("PT2"));
        assertEquals(3, graph.getAllNodes().size());

        // 验证P21的出边
        Map<String, String> p21OutEdges = graph.getOutEdges("P21");
        assertEquals(1, p21OutEdges.size());
        assertEquals("Rule21", p21OutEdges.get("PT2"));

        // 验证P22的出边
        Map<String, String> p22OutEdges = graph.getOutEdges("P22");
        assertEquals(1, p22OutEdges.size());
        assertEquals("Rule21", p22OutEdges.get("PT2"));

        // 验证PT2的入边
        Map<String, String> pt2InEdges = graph.getInEdges("PT2");
        assertEquals(2, pt2InEdges.size());
        assertEquals("Rule21", pt2InEdges.get("P21"));
        assertEquals("Rule21", pt2InEdges.get("P22"));
    }

    /**
     * 测试添加空参数关系
     */
    @Test
    public void testAddRelationWithNullParameters() {
        // 测试null fromLeft
        graph.add("Rule01", null, p11);

        // 测试null toRight
        graph.add("Rule02", p0, null);

        // 测试null fromLefts列表
        graph.add("Rule03", (List<RefProgObjSchema>) null, Arrays.asList(pt1));

        // 测试空fromLefts列表
        graph.add("Rule04", Arrays.asList(), Arrays.asList(pt1));

        // 验证图应该为空
        assertTrue(graph.getAllNodes().isEmpty());
    }

    /**
     * 测试添加重复关系
     */
    @Test
    public void testAddDuplicateRelation() {
        // 添加相同的关系两次
        graph.add("Rule01", p0, p11);
        graph.add("Rule01", p0, p11);

        // 验证只添加了一次
        assertEquals(2, graph.getAllNodes().size());
        Map<String, String> outEdges = graph.getOutEdges("P0");
        assertEquals(1, outEdges.size());
        assertEquals("Rule01", outEdges.get("P11"));
    }

    /**
     * 测试查询单一目标简单链
     */
    @Test
    public void testQuerySingleTargetSimpleChain() {
        // 构建复杂的依赖关系图
        // Rule01: P0 -> P11
        // Rule11: P11 -> PT1
        graph.add("Rule01", p0, p11);
        graph.add("Rule11", p11, pt1);

        // 查询PT1的依赖
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("PT1");

        // 验证规则
        List<String> ruleCodes = result.getFirst();
        assertTrue(ruleCodes.contains("Rule11"));
        assertTrue(ruleCodes.contains("Rule01"));
        assertEquals(2, ruleCodes.size());

        // 验证编程对象
        List<RefProgObjSchema> progObjs = result.getSecond();
        assertTrue(containsProgObjCode(progObjs, "PT1"));
        assertTrue(containsProgObjCode(progObjs, "P11"));
        assertTrue(containsProgObjCode(progObjs, "P0"));
        assertEquals(3, progObjs.size());
    }

    /**
     * 测试查询单一目标多个分支
     */
    @Test
    public void testQuerySingleTargetMultipleBranches() {
        // 构建复杂的依赖关系图
        // Rule01: P0 -> P11
        // Rule02: P0 -> P21
        // Rule11: P11 -> PT1
        // Rule21: [P21, P22] -> PT2
        graph.add("Rule01", p0, p11);
        graph.add("Rule02", p0, p21);
        graph.add("Rule11", p11, pt1);
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 查询PT2的依赖
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("PT2");

        // 验证规则
        List<String> ruleCodes = result.getFirst();
        assertTrue(ruleCodes.contains("Rule21"));
        assertTrue(ruleCodes.contains("Rule02"));
        assertEquals(2, ruleCodes.size());

        // 验证编程对象
        List<RefProgObjSchema> progObjs = result.getSecond();
        assertTrue(containsProgObjCode(progObjs, "PT2"));
        assertTrue(containsProgObjCode(progObjs, "P21"));
        assertTrue(containsProgObjCode(progObjs, "P22"));
        assertTrue(containsProgObjCode(progObjs, "P0"));
        assertEquals(4, progObjs.size());
    }

    /**
     * 测试查询多个目标
     */
    @Test
    public void testQueryMultipleTargets() {
        // 构建复杂的依赖关系图
        graph.add("Rule01", p0, p11);
        graph.add("Rule02", p0, p21);
        graph.add("Rule11", p11, pt1);
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 查询PT1和P0的依赖
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("PT1", "P0");

        // 验证规则
        List<String> ruleCodes = result.getFirst();
        assertTrue(ruleCodes.contains("Rule11"));
        assertTrue(ruleCodes.contains("Rule01"));
        assertEquals(2, ruleCodes.size());

        // 验证编程对象（应该包含输入）
        List<RefProgObjSchema> progObjs = result.getSecond();
        assertTrue(containsProgObjCode(progObjs, "PT1"));
        assertTrue(containsProgObjCode(progObjs, "P0"));
        assertTrue(containsProgObjCode(progObjs, "P11"));
        assertEquals(3, progObjs.size());
    }

    /**
     * 测试查询不存在的目标
     */
    @Test
    public void testQueryNonExistentTarget() {
        // 构建简单的依赖关系图
        graph.add("Rule01", p0, p11);

        // 查询不存在的目标
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("NonExistent");

        // 验证只包含输入本身
        List<String> ruleCodes = result.getFirst();
        assertTrue(ruleCodes.isEmpty());

        List<RefProgObjSchema> progObjs = result.getSecond();
        assertTrue(progObjs.isEmpty());
    }

    /**
     * 测试查询空输入
     */
    @Test
    public void testQueryEmptyInput() {
        // 查询空输入
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph();

        // 验证结果为空
        assertTrue(result.getFirst().isEmpty());
        assertTrue(result.getSecond().isEmpty());
    }

    /**
     * 测试查询循环依赖
     */
    @Test
    public void testQueryCircularDependency() {
        // 添加循环依赖：A -> B -> A
        RefProgObjSchema a = createRefProgObjSchema("Para", "A");
        RefProgObjSchema b = createRefProgObjSchema("Para", "B");

        graph.add("RuleA", a, b);
        graph.add("RuleB", b, a);

        // 查询应该不会无限循环
        try {
            Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("A");
            // 验证结果合理
            assertNotNull(result);
            assertTrue(containsProgObjCode(result.getSecond(), "A"));
        } catch (Exception e) {
            fail("Circular dependency should not cause infinite loop: " + e.getMessage());
        }
    }

    /**
     * 测试复杂依赖图
     */
    @Test
    public void testComplexDependencyGraph() {
        // 构建复杂的依赖图
        // Level 1: P0
        // Level 2: P11, P21, P22 (都依赖P0)
        // Level 3: PT1 (依赖P11), PT2 (依赖P21和P22)

        graph.add("Rule01", p0, p11);
        graph.add("Rule02", p0, p21);
        graph.add("Rule03", p0, p22);
        graph.add("Rule11", p11, pt1);
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 测试查询PT1
        Pair<List<String>, List<RefProgObjSchema>> result1 = graph.querySubGraph("PT1");
        assertEquals(2, result1.getFirst().size());
        assertTrue(result1.getFirst().contains("Rule11"));
        assertTrue(result1.getFirst().contains("Rule01"));
        assertEquals(3, result1.getSecond().size());

        // 测试查询PT2
        Pair<List<String>, List<RefProgObjSchema>> result2 = graph.querySubGraph("PT2");
        assertEquals(3, result2.getFirst().size());
        assertTrue(result2.getFirst().contains("Rule21"));
        assertTrue(result2.getFirst().contains("Rule02"));
        assertTrue(result2.getFirst().contains("Rule03"));
        assertEquals(4, result2.getSecond().size());
    }

    /**
     * 测试多路径依赖
     */
    @Test
    public void testMultiplePathDependency() {
        // 构建多路径依赖：A -> B -> D, A -> C -> D
        RefProgObjSchema a = createRefProgObjSchema("Para", "A");
        RefProgObjSchema b = createRefProgObjSchema("Para", "B");
        RefProgObjSchema c = createRefProgObjSchema("Para", "C");
        RefProgObjSchema d = createRefProgObjSchema("Part", "D");

        graph.add("Rule1", a, b);
        graph.add("Rule2", a, c);
        graph.add("Rule3", b, d);
        graph.add("Rule4", c, d);

        // 查询D的依赖
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("D");

        // 验证所有路径都被找到
        assertEquals(4, result.getFirst().size());
        assertTrue(result.getFirst().contains("Rule1"));
        assertTrue(result.getFirst().contains("Rule2"));
        assertTrue(result.getFirst().contains("Rule3"));
        assertTrue(result.getFirst().contains("Rule4"));

        assertEquals(4, result.getSecond().size());
        assertTrue(containsProgObjCode(result.getSecond(), "A"));
        assertTrue(containsProgObjCode(result.getSecond(), "B"));
        assertTrue(containsProgObjCode(result.getSecond(), "C"));
        assertTrue(containsProgObjCode(result.getSecond(), "D"));
    }

    /**
     * 测试大数据集
     */
    @Test
    public void testLargeDataSet() {
        // 创建大量节点和关系
        for (int i = 0; i < 100; i++) {
            RefProgObjSchema from = createRefProgObjSchema("Para", "P" + i);
            RefProgObjSchema to = createRefProgObjSchema("Para", "P" + (i + 1));
            graph.add("Rule" + i, from, to);
        }

        // 查询最后一个节点
        Pair<List<String>, List<RefProgObjSchema>> result = graph.querySubGraph("P100");

        // 验证结果
        assertEquals(100, result.getFirst().size());
        assertEquals(101, result.getSecond().size()); // 包含P0到P100
    }

    /**
     * 测试获取图信息
     */
    @Test
    public void testGetGraphInfo() {
        // 添加一些关系
        graph.add("Rule01", p0, p11);
        graph.add("Rule02", p0, p21);
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 获取图信息
        String info = graph.getGraphInfo();

        // 验证信息格式
        assertNotNull(info);
        assertTrue(info.contains("ModuleRefRelationGraph"));
        assertTrue(info.contains("nodes"));
        assertTrue(info.contains("edges"));
    }

    /**
     * 测试图结构
     */
    @Test
    public void testGraphStructure() {
        // 构建测试图
        graph.add("Rule01", p0, p11);
        graph.add("Rule21", Arrays.asList(p21, p22), Arrays.asList(pt2));

        // 验证节点
        Set<String> allNodes = graph.getAllNodes();
        assertEquals(5, allNodes.size());
        assertTrue(allNodes.contains("P0"));
        assertTrue(allNodes.contains("P11"));
        assertTrue(allNodes.contains("P21"));
        assertTrue(allNodes.contains("P22"));
        assertTrue(allNodes.contains("PT2"));

        // 验证P0的出边
        Map<String, String> p0OutEdges = graph.getOutEdges("P0");
        assertEquals(1, p0OutEdges.size());
        assertEquals("Rule01", p0OutEdges.get("P11"));

        // 验证PT2的入边
        Map<String, String> pt2InEdges = graph.getInEdges("PT2");
        assertEquals(2, pt2InEdges.size());
        assertEquals("Rule21", pt2InEdges.get("P21"));
        assertEquals("Rule21", pt2InEdges.get("P22"));
    }

    /**
     * 辅助方法：检查编程对象列表中是否包含指定编码的对象
     */
    private boolean containsProgObjCode(List<RefProgObjSchema> progObjs, String code) {
        return progObjs.stream()
                .anyMatch(obj -> code.equals(obj.getProgObjCode()));
    }
}