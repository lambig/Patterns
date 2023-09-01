package io.github.lambig.patterns;

import io.github.lambig.tuplite._2.Tuple2;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import static io.github.lambig.tuplite._2.Tuple2.tuple;
import static java.util.stream.Collectors.toList;

/**
 * 副作用前提の疑似パターンマッチ定義クラス
 * パターンは定義順に評価します。
 *
 * @param <K> キー型
 */
public class ConsumingPatterns<K> implements Consumer<K> {

    private final List<Tuple2<Predicate<K>, Consumer<K>>> mappings;

    private ConsumingPatterns(@NonNull List<Tuple2<Predicate<K>, Consumer<K>>> patterns) {
        this.mappings = patterns;
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @return パターンによるマッピング
     */
    @SafeVarargs
    public static <S> ConsumingPatterns<S> of(@NonNull Tuple2<Predicate<S>, Consumer<S>>... patterns) {
        return consumingPatterns(patterns);
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @return パターンによるマッピング
     */
    public static <S> ConsumingPatterns<S> of(@NonNull List<Tuple2<Predicate<S>, Consumer<S>>> patterns) {
        return consumingPatterns(patterns);
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @return パターンによるマッピング
     */
    @SafeVarargs
    public static <S> ConsumingPatterns<S> consumingPatterns(@NonNull Tuple2<Predicate<S>, Consumer<S>>... patterns) {
        return consumingPatterns(Stream.of(patterns).collect(toList()));
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @return パターンによるマッピング
     */
    public static <S> ConsumingPatterns<S> consumingPatterns(@NonNull List<Tuple2<Predicate<S>, Consumer<S>>> patterns) {
        return new ConsumingPatterns<>(patterns);
    }

    /**
     * キーを最初に該当するパターンの処理に与えて実行します。
     *
     * @param key キー
     * @throws NoSuchPatternException 該当するパターンがない場合
     */
    @NonNull
    public void handle(K key) {
        this.getConsumer(key)
                .orElseThrow(() -> new NoSuchPatternException("for key: " + key + ". To allow this pattern to accept value that match no defined pattern, consider setting default consumer."))
                .accept(key);
    }

    @NonNull
    private Optional<Consumer<K>> getConsumer(K key) {
        return this.mappings.stream()
                .filter(entry -> entry._1().test(key))
                .map(Tuple2::_2)
                .findFirst();
    }

    /**
     * キーを該当パターンの処理またはデフォルト処理に与えて実行します。
     *
     * @param defaultConsumer デフォルト処理
     * @return このPatternでデフォルト値付きで適用するConsumer
     */
    public Consumer<K> orElseDo(@NonNull Consumer<K> defaultConsumer) {
        return k -> this.getConsumer(k).orElse(defaultConsumer).accept(k);
    }

    /**
     * キーを処理に与えて実行し、キーが未設定の場合例外を送出するConsumerを返します。
     *
     * @param exceptionSupplier 指定例外のSupplier
     * @return このPatternでmapするFunction Functionはキーが未設定の場合指定例外を送出する
     */
    public Consumer<K> orElseThrow(@NonNull Supplier<RuntimeException> exceptionSupplier) {
        return k -> this.getConsumer(k).orElseThrow(exceptionSupplier).accept(k);
    }

    @Override
    public void accept(K k) {
        this.handle(k);
    }

    /**
     * キーがwhenを満たした場合にキーに適用する処理を明示します。
     * (実はthenAcceptWithを書かなくても直接whenの第2引数に処理を指定すれば動きます
     *
     * @param consumer 適用する関数
     * @param <S>      キー型
     * @return パターンに設定する返却関数
     */
    public static <S> Consumer<S> thenAcceptWith(@NonNull Consumer<? super S> consumer) {
        return consumer::accept;
    }

    /**
     * パターンを定義します。
     *
     * @param when     キーがこのパターンに該当する条件
     * @param consumer キーがこのパターンに該当する場合、キーに適用する処理
     * @param <S>      キー型
     * @return パターン
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> when(@NonNull Predicate<? super S> when, @NonNull Consumer<? super S> consumer) {
        return tuple(when::test, consumer::accept);
    }

    /**
     * 引数と等価であることをwhenと定義します。
     *
     * @param target キーとの比較対象
     * @param <S>    キー型
     * @return 等価判定
     */
    public static <S> Predicate<S> equalsTo(@NonNull S target) {
        return s -> Objects.equals(s, target);
    }

    /**
     * あらゆるキーに該当するパターンを返却します。
     * デフォルト値の設定に有用です。
     *
     * @param thenAcceptWith キーがこのパターンに該当する場合、キーに適用する処理
     * @param <S>キー型
     * @return デフォルトパターン
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> orElse(@NonNull Consumer<? super S> thenAcceptWith) {
        return tuple(anything -> true, thenAcceptWith::accept);
    }

    /**
     * あらゆるキーに対し例外を送出させます。
     * 想定外パターンの対応に有用です。
     *
     * @param thenAcceptWith キーがこのパターンに該当する場合、キーに適用して例外を生成する関数
     * @param <S>キー型
     * @return 例外パターン
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> orElseThrow(@NonNull Function<? super S, @NonNull RuntimeException> thenAcceptWith) {
        return tuple(anything -> true, input -> {
            throw thenAcceptWith.apply(input);
        });
    }

    /**
     * クラスによるパターンマッチを定義します。
     *
     * @param clazz          キーがこのパターンに該当する条件となるクラス
     * @param thenAcceptWith キーがこのパターンに該当する場合、clazzクラスにキャストしたキーに適用する処理
     * @param <S>            キー型
     * @return パターン
     */
    public static <S, T extends S> Tuple2<Predicate<S>, Consumer<S>> whenMatch(@NonNull Class<T> clazz, @NonNull Consumer<? super T> thenAcceptWith) {
        return tuple(clazz::isInstance, instance -> thenAcceptWith.accept(clazz.cast(instance)));
    }

    /**
     * クラスによるパターンマッチを定義します。当該クラスのインスタンスとしたうえで満たすべき述語を定義できます。
     *
     * @param clazz          キーがこのパターンに該当する条件となるクラス
     * @param when           上記クラスかつ満たすべき条件
     * @param thenAcceptWith キーがこのパターンに該当する場合、clazzクラスにキャストしたキーに適用する処理
     * @param <S>            キー型
     * @return パターン
     */
    public static <S, T extends S> Tuple2<Predicate<S>, Consumer<S>> whenMatch(@NonNull Class<T> clazz, Predicate<? super T> when, @NonNull Consumer<? super T> thenAcceptWith) {
        return tuple(key -> clazz.isInstance(key) && when.test(clazz.cast(key)), instance -> thenAcceptWith.accept(clazz.cast(instance)));
    }

}
