import com.jmix.temp3.*;
import java.util.*;

public class TestSolution {
    public static void main(String[] args) {
        // 创建一个示例解决方案
        List<PartResult> parts = Arrays.asList(
            new PartResult("sd1", true, 2),
            new PartResult("md1", false, 0)
        );
        Solution solution = new Solution(parts, -600.0);
        System.out.println("Solution toString: " + solution.toString());
    }
}
