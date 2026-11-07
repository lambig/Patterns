package io.github.lambig.patterns;

import io.github.lambig.tuplite._2.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import static io.github.lambig.patterns.Patterns.then;
import static io.github.lambig.patterns.Patterns.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

class PublicContractTest {
    @Test
    void constructorAndListFactoriesKeepOriginalRules() {
        List<Tuple2<Predicate<Integer>, Function<Integer, String>>> rules = new ArrayList<>(
                List.of(when(n -> n > 0, then("original"))));
        List<Patterns<Integer, String>> classifiers = List.of(new Patterns<>(rules), Patterns.of(rules), Patterns.patterns(rules));
        rules.set(0, when(n -> true, then("changed")));
        rules.clear();
        classifiers.forEach(p -> assertThat(p.get(1)).isEqualTo("original"));
    }

    @Test
    void consumingListFactoriesKeepOriginalRules() {
        List<String> events = new ArrayList<>();
        List<Tuple2<Predicate<Integer>, Consumer<Integer>>> rules = new ArrayList<>(List.of(
                ConsumingPatterns.when(n -> n > 0, n -> events.add("original"))));
        List<ConsumingPatterns<Integer>> handlers = List.of(ConsumingPatterns.of(rules), ConsumingPatterns.consumingPatterns(rules));
        rules.set(0, ConsumingPatterns.when(n -> true, n -> events.add("changed")));
        rules.clear();
        handlers.forEach(p -> p.accept(1));
        assertThat(events).containsExactly("original", "original");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void arrayFactoriesKeepOriginalRules() {
        Tuple2<Predicate<Integer>, Function<Integer, String>>[] rules = new Tuple2[]{when(n -> true, then("original"))};
        Patterns<Integer, String> classifier = Patterns.of(rules);
        rules[0] = when(n -> true, then("changed"));
        assertThat(classifier.get(1)).isEqualTo("original");
        List<String> events = new ArrayList<>();
        Tuple2<Predicate<Integer>, Consumer<Integer>>[] actions = new Tuple2[]{ConsumingPatterns.when(n -> true, n -> events.add("original"))};
        ConsumingPatterns<Integer> handler = ConsumingPatterns.of(actions);
        actions[0] = ConsumingPatterns.when(n -> true, n -> events.add("changed"));
        handler.accept(1);
        assertThat(events).containsExactly("original");
    }

    @Test
    void matchingNullResultEndsSelectionAndUsesOptionalFallback() {
        Patterns<Integer, String> p = Patterns.of(when(n -> true, then(null)),
                when(n -> { fail("later predicate"); return true; }, then("later")));
        assertThatThrownBy(() -> p.get(1)).isInstanceOf(NullPointerException.class);
        assertThat(p.getOptionally(1)).isEmpty();
        assertThat(p.optional().apply(1)).isEmpty();
        assertThat(p.orElse("fallback").apply(1)).isEqualTo("fallback");
        assertThat(p.orElseGet(() -> "supplied").apply(1)).isEqualTo("supplied");
        RuntimeException expected = new IllegalArgumentException("empty result");
        assertThatThrownBy(() -> p.orElseThrow(() -> expected).apply(1)).isSameAs(expected);
    }

    @Test
    void onlyFirstMatchingConsumerRuns() {
        List<Integer> events = new ArrayList<>();
        ConsumingPatterns<Integer> p = ConsumingPatterns.of(ConsumingPatterns.when(n -> true, events::add),
                ConsumingPatterns.when(n -> { fail("later predicate"); return true; }, n -> fail("later consumer")));
        p.handle(2);
        p.orElseDo(n -> fail("default consumer")).accept(3);
        assertThat(events).containsExactly(2, 3);
    }

    @Test
    void emptyRulesHaveTheSameUnmatchedBehaviorAsRejectedConditions() {
        Patterns<Integer, String> p = Patterns.of();
        assertThatThrownBy(() -> p.apply(0)).isInstanceOf(NoSuchPatternException.class);
        assertThat(p.getOptionally(0)).isEmpty();
        assertThat(p.orElse((String) null).apply(0)).isNull();
        ConsumingPatterns<Integer> c = ConsumingPatterns.of();
        assertThatThrownBy(() -> c.accept(0)).isInstanceOf(NoSuchPatternException.class);
        AtomicInteger value = new AtomicInteger();
        c.orElseDo(value::set).accept(7);
        assertThat(value).hasValue(7);
    }

    @Test
    void suppliersAreCalledOnlyForSelectedResultsOrEmptyFallbacks() {
        AtomicInteger results = new AtomicInteger();
        AtomicInteger fallbacks = new AtomicInteger();
        Patterns<Integer, Integer> p = Patterns.of(when(n -> n > 0, Patterns.thenSupply(results::incrementAndGet)));
        Function<Integer, Integer> f = p.orElseGet(fallbacks::incrementAndGet);
        assertThat(f.apply(1)).isEqualTo(1);
        assertThat(f.apply(1)).isEqualTo(2);
        assertThat(fallbacks).hasValue(0);
        assertThat(f.apply(0)).isEqualTo(1);
        assertThat(results).hasValue(2);
    }

    @Test
    void nullInputsCanBeMatchedExplicitlyAndAreUnmatchedByTypeRules() {
        assertThat(Patterns.equalsTo(null).test(null)).isTrue();
        assertThat(Patterns.of(when(Patterns.equalsTo(null), then("null"))).get(null)).isEqualTo("null");
        assertThatThrownBy(() -> ConsumingPatterns.equalsTo(null)).isInstanceOf(NullPointerException.class);
        List<String> events = new ArrayList<>();
        ConsumingPatterns.of(ConsumingPatterns.when(java.util.Objects::isNull, n -> events.add("null"))).accept(null);
        assertThat(events).containsExactly("null");
        Patterns<Object, String> p = Patterns.of(Patterns.whenMatch(Number.class, n -> n.intValue() > 0, Object::toString));
        assertThat(p.get(2)).isEqualTo("2");
        assertThat(p.getOptionally(null)).isEmpty();
        assertThat(p.getOptionally("2")).isEmpty();
        assertThat(p.getOptionally(-1)).isEmpty();
    }

    @Test
    void typeGuardReceivesOnlyInstancesOfTheTargetType() {
        List<Integer> events = new ArrayList<>();
        ConsumingPatterns<Object> c = ConsumingPatterns.of(ConsumingPatterns.whenMatch(Integer.class,
                n -> n > 0, events::add));
        c.orElseDo(n -> {}).accept(null);
        c.orElseDo(n -> {}).accept("2");
        c.orElseDo(n -> {}).accept(-1);
        c.accept(2);
        assertThat(events).containsExactly(2);
    }

    @Test
    void exceptionsFromConditionsAndFunctionsRemainTheOriginalException() {
        RuntimeException expected = new IllegalStateException("condition");
        Patterns<Integer, String> p = Patterns.of(when(n -> { throw expected; }, then("unused")));
        assertThatThrownBy(() -> p.orElse("fallback").apply(1)).isSameAs(expected);
        Patterns<Integer, String> f = Patterns.of(when(n -> true, n -> { throw expected; }));
        assertThatThrownBy(() -> f.getOptionally(1)).isSameAs(expected);
        ConsumingPatterns<Integer> c = ConsumingPatterns.of(ConsumingPatterns.when(n -> { throw expected; }, n -> fail()));
        assertThatThrownBy(() -> c.orElseDo(n -> fail()).accept(1)).isSameAs(expected);
    }

    @Test
    void consumerUpdatesBeforeAnExceptionRemainVisible() {
        List<Integer> events = new ArrayList<>();
        RuntimeException expected = new IllegalStateException("consumer");
        ConsumingPatterns<Integer> p = ConsumingPatterns.of(ConsumingPatterns.when(n -> true,
                n -> { events.add(n); throw expected; }));
        assertThatThrownBy(() -> p.orElseDo(n -> fail()).accept(2)).isSameAs(expected);
        assertThat(events).containsExactly(2);
    }

    @Test
    void ruleStateRemainsSharedWhileTheListIsIndependent() {
        AtomicInteger threshold = new AtomicInteger(10);
        Patterns<Integer, String> p = Patterns.of(when(n -> n >= threshold.get(), then("accepted")));
        assertThat(p.getOptionally(5)).isEmpty();
        threshold.set(3);
        assertThat(p.get(5)).isEqualTo("accepted");
    }

    @Test
    void nullRuleContainersFailAtConstruction() {
        assertThatThrownBy(() -> new Patterns<Integer, String>(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Patterns.of((List<Tuple2<Predicate<Integer>, Function<Integer, String>>>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ConsumingPatterns.of((List<Tuple2<Predicate<Integer>, Consumer<Integer>>>) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void exceptionConstructorsPreserveMessagesAndCauses() {
        RuntimeException cause = new IllegalStateException("reason");
        assertThat(new NoSuchPatternException()).hasMessage(null).hasCause(null);
        assertThat(new NoSuchPatternException("detail")).hasMessage("detail").hasCause(null);
        assertThat(new NoSuchPatternException(cause)).hasMessage("reason").hasCause(cause);
        assertThat(new NoSuchPatternException("detail", cause)).hasMessage("detail").hasCause(cause);
        assertThat(new NoSuchPatternException((Throwable) null)).hasMessage(null).hasCause(null);
    }
}
