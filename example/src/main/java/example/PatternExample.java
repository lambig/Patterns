package example;

import io.github.lambig.patterns.ConsumingPatterns;
import io.github.lambig.patterns.Patterns;
import io.github.lambig.tuplite._2.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import static io.github.lambig.patterns.Patterns.then;
import static io.github.lambig.patterns.Patterns.when;

/** Patternsだけを依存宣言した外部パッケージから、分類と処理の振り分けを利用する例。 */
public class PatternExample {
    public static void main(String[] args) {
        Tuple2<Predicate<Integer>, Function<Integer, String>> large = when(n -> n >= 10, then("large"));
        Patterns<Integer, String> classification = Patterns.of(large, when(n -> n > 0, then("small")));
        if (!classification.get(12).equals("large")) throw new AssertionError("first match");
        if (!classification.get(3).equals("small")) throw new AssertionError("second match");
        if (classification.getOptionally(0).isPresent()) throw new AssertionError("unmatched");
        if (!classification.orElse("unknown").apply(0).equals("unknown")) throw new AssertionError("fallback");

        Patterns<Number, String> integers = Patterns.of(Patterns.whenMatch(Integer.class,
                n -> n > 0, n -> "integer:" + n));
        if (!integers.get(3).equals("integer:3") || integers.getOptionally(1.5).isPresent()) {
            throw new AssertionError("type condition");
        }

        List<String> events = new ArrayList<>();
        ConsumingPatterns<Integer> routing = ConsumingPatterns.of(
                ConsumingPatterns.when(n -> n >= 10, n -> events.add("large:" + n)),
                ConsumingPatterns.when(n -> n > 0, n -> events.add("small:" + n)));
        routing.accept(12);
        routing.orElseDo(n -> events.add("other:" + n)).accept(0);
        if (!events.equals(List.of("large:12", "other:0"))) throw new AssertionError(events);
        System.out.println("classification=" + classification.get(12)
                + "; fallback=" + classification.orElse("unknown").apply(0)
                + "; events=" + events + "; tuple-api=OK");
    }
}
