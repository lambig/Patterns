package io.github.lambig.patterns;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static io.github.lambig.patterns.ConsumingPatterns.consumingPatterns;
import static io.github.lambig.patterns.ConsumingPatterns.equalsTo;
import static io.github.lambig.patterns.ConsumingPatterns.orElse;
import static io.github.lambig.patterns.ConsumingPatterns.orElseThrow;
import static io.github.lambig.patterns.ConsumingPatterns.thenAcceptWith;
import static io.github.lambig.patterns.ConsumingPatterns.when;
import static io.github.lambig.patterns.ConsumingPatterns.whenMatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("NonAsciiCharacters")
class ConsumingPatternsTest {

    @Nested
    class 設定と処理のテスト_handle {
        @Test
        void 該当キーに対応する処理が実行されること() {
            //SetUp
            AtomicReference<Integer> integer1 = new AtomicReference<>();
            AtomicReference<Integer> integer2 = new AtomicReference<>();
            AtomicReference<Integer> integer3 = new AtomicReference<>();

            ConsumingPatterns<Integer> target =
                    ConsumingPatterns.of(
                            when(equalsTo(3), thenAcceptWith(integer1::set)),
                            when(equalsTo(4), thenAcceptWith(integer2::set)),
                            orElse(integer3::set));

            //Exercise
            Stream.of(-1, 0, 1, 2, 3, 4).forEach(target);
            //Verify
            assertAll(
                    () -> assertThat(integer1).hasValue(3),
                    () -> assertThat(integer2).hasValue(4),
                    () -> assertThat(integer3).hasValue(2));
        }

        @Test
        void 該当キーに対応する値がなければNoSuchPatternExceptionが取得できること() {
            //SetUp
            AtomicReference<Integer> integer1 = new AtomicReference<>();
            AtomicReference<Integer> integer2 = new AtomicReference<>();

            ConsumingPatterns<Integer> target =
                    consumingPatterns(
                            when(equalsTo(3), thenAcceptWith(integer1::set)),
                            when(equalsTo(4), thenAcceptWith(integer2::set)));

            //Exercise
            var actual = assertThatThrownBy(() -> Stream.of(4, 3, 2).forEach(target));
            //Verify
            actual
                    .isInstanceOf(NoSuchPatternException.class)
                    .extracting("message")
                    .isEqualTo("for key: 2. To allow this pattern to accept value that match no defined pattern, consider setting default consumer.");
        }

        @Test
        void orElseThrowで指定した例外がThrowできること() {
            //SetUp
            class ExpectedException extends RuntimeException {
            }
            AtomicReference<Integer> integer1 = new AtomicReference<>();
            AtomicReference<Integer> integer2 = new AtomicReference<>();

            ConsumingPatterns<Integer> target =
                    ConsumingPatterns.of(List.of(
                            when(equalsTo(3), thenAcceptWith(integer1::set)),
                            when(equalsTo(4), thenAcceptWith(integer2::set)),
                            orElseThrow(anything -> new ExpectedException())));

            //Exercise
            var actual = assertThatThrownBy(() -> Stream.of(-1, 0, 1, 2, 3).forEach(target));
            //Verify
            actual.isInstanceOf(ExpectedException.class);
        }

        @Test
        void 型パターンマッチのテスト() {
            //SetUp
            @RequiredArgsConstructor
            @Accessors(fluent = true)
            @Getter
            class A {
                final String value;
            }
            class B extends A {
                public B(String value) {
                    super(value);
                }
            }
            class C extends A {
                public C(String value) {
                    super(value);
                }

                public String say() {
                    return "C here. I've got " + this.value() + ".";
                }
            }

            AtomicReference<String> string1 = new AtomicReference<>();
            AtomicReference<String> string2 = new AtomicReference<>();
            AtomicReference<String> string3 = new AtomicReference<>();

            ConsumingPatterns<A> target =
                    consumingPatterns(
                            whenMatch(B.class, thenAcceptWith(b -> string2.set("it's a B. value: " + b.value() + "."))),
                            whenMatch(C.class, Objects::isNull, thenAcceptWith(c -> fail("unexpected"))),
                            whenMatch(C.class, thenAcceptWith(c -> string3.set(c.say()))),
                            orElse(thenAcceptWith(a -> string1.set("it's a plain A."))));

            //Exercise
            Stream.of(new A("aaa"), new B("bbb"), new C("ccc")).forEach(target);
            //Verify
            assertThat(Stream.of(string1, string2, string3)).map(AtomicReference::get)
                    .containsExactly("it's a plain A.", "it's a B. value: bbb.", "C here. I've got ccc.");
        }
    }

    @Nested
    class 設定と処理のテスト_orElse {
        @Test
        void 該当キーに対応する処理が実行されること() {
            //SetUp
            AtomicReference<Integer> integer1 = new AtomicReference<>();
            AtomicReference<Integer> integer2 = new AtomicReference<>();
            AtomicReference<Integer> integer3 = new AtomicReference<>();

            ConsumingPatterns<Integer> target =
                    ConsumingPatterns.of(
                            when(equalsTo(3), thenAcceptWith(integer1::set)),
                            when(equalsTo(4), thenAcceptWith(integer2::set)));

            //Exercise
            Stream.of(-1, 0, 1, 2, 3, 4).forEach(target.orElseDo(integer3::set));
            //Verify
            assertAll(
                    () -> assertThat(integer1).hasValue(3),
                    () -> assertThat(integer2).hasValue(4),
                    () -> assertThat(integer3).hasValue(2));
        }

        @Test
        void 該当キーに対応するExceptionが送出できること() {
            //SetUp
            AtomicReference<Integer> integer1 = new AtomicReference<>();
            AtomicReference<Integer> integer2 = new AtomicReference<>();
            class ExpectedException extends RuntimeException {
            }
            ConsumingPatterns<Integer> target =
                    consumingPatterns(
                            when(equalsTo(3), thenAcceptWith(integer1::set)),
                            when(equalsTo(4), thenAcceptWith(integer2::set)));

            //Exercise
            var actual = assertThatThrownBy(() -> Stream.of(4, 3, 2).forEach(target.orElseThrow(ExpectedException::new)));
            //Verify
            actual.isInstanceOf(ExpectedException.class);
        }

    }
}