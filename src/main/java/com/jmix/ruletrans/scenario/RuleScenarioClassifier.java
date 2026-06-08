package com.jmix.ruletrans.scenario;

import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.sdk.SdkProfile;

import java.util.Locale;

/**
 * Lightweight scenario classifier for SDK routing.
 */
public final class RuleScenarioClassifier {

    public RuleScenario classify(String naturalLanguage, RuleContext context) {
        String text = naturalLanguage == null ? "" : naturalLanguage.toLowerCase(Locale.ROOT);
        RuleScope scope = context != null && !context.isProductLevel()
                ? RuleScope.PART_CATEGORY
                : RuleScope.PRODUCT;
        boolean post = containsAny(text,
                "post", "后置", "写回", "回写", "设置参数", "产品参数", "实例属性", "setvalue");
        RuleFamily family = familyOf(text, post);
        return new RuleScenario(
                scope,
                post ? RuleCalcStage.POST : RuleCalcStage.NON_POST,
                family,
                post ? SdkProfile.POST : SdkProfile.CONSTRAINT);
    }

    private RuleFamily familyOf(String text, boolean post) {
        if (post) {
            return RuleFamily.POST;
        }
        if (containsAny(text, "兼容", "不能", "不可", "incompatible", "requires", "依赖", "同选")) {
            return RuleFamily.COMPATIBLE;
        }
        if (containsAny(text, "隐藏", "hide", "hidden")) {
            return RuleFamily.HIDDEN;
        }
        if (containsAny(text, "优先", "priority", "最小", "最大", "目标")) {
            return RuleFamily.PRIORITY;
        }
        if (containsAny(text, "汇总", "总和", "sum", "容量", "数量")) {
            return RuleFamily.AGGREGATE;
        }
        if (containsAny(text, "如果", "if", "计算", "等于")) {
            return RuleFamily.CALCULATE;
        }
        if (containsAny(text, "最多", "至多", "只能", "exactly", "at most", "选择")) {
            return RuleFamily.SELECT;
        }
        return RuleFamily.UNKNOWN;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
