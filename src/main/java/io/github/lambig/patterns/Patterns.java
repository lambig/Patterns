package io.github.lambig.patterns;

import io.github.lambig.tuplite._2.Tuple2;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.StandardException;
import static io.github.lambig.tuplite._2.Tuple2._2mapWith;
import static io.github.lambig.tuplite._2.Tuple2.tuple;
import static java.util.stream.Collectors.toList;

/**
 * 疑似パターンマッチ定義クラス
 * パターンは定義順に評価します。
 *
 * @param <K> キー型
 * @param <V> 処理結果の戻り型
 */
public class Patterns<K, V> implements Function<K, V> {

    private final List<Tuple2<Predicate<K>, Function<K, V>>> mappings;

    public Patterns(List<Tuple2<Predicate<K>, Function<K, V>>> patterns) {
        this.mappings = patterns;
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @param <O>      出力型
     * @return パターンによるマッピング
     */
    @SafeVarargs
    public static <S, O> Patterns<S, O> of(Tuple2<Predicate<S>, Function<S, O>>... patterns) {
        return patterns(patterns);
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @param <O>      出力型
     * @return パターンによるマッピング
     */
    public static <S, O> Patterns<S, O> of(List<Tuple2<Predicate<S>, Function<S, O>>> patterns) {
        return patterns(patterns);
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @param <O>      出力型
     * @return パターンによるマッピング
     */
    @SafeVarargs
    public static <S, O> Patterns<S, O> patterns(Tuple2<Predicate<S>, Function<S, O>>... patterns) {
        return patterns(Stream.of(patterns).collect(toList()));
    }

    /**
     * パターンを宣言します。
     *
     * @param patterns 宣言する各パターン
     * @param <S>      入力型
     * @param <O>      出力型
     * @return パターンによるマッピング
     */
    public static <S, O> Patterns<S, O> patterns(List<Tuple2<Predicate<S>, Function<S, O>>> patterns) {
        return new Patterns<>(patterns);
    }

    /**
     * キーが最初に該当するパターンの返却値を取得します
     *
     * @param key キー
     * @return 該当するパターンがあればその返却値、なければnull
     * @throws NoSuchPatternException 該当するパターンがない場合
     * @throws NullPointerException   該当するパターンがnullを返却した場合
     */
    @NonNull
    public V get(K key) {
        var pattern = this.mappings.stream()
                .filter(entry -> entry._1().test(key))
                .findFirst()
                .orElseThrow(() -> new NoSuchPatternException("for key: " + key + ". To allow this pattern to return nullable value, consider using Patterns#getOptionally or so."));

        return Optional.ofNullable(pattern._2().apply(key))
                .orElseThrow(() -> new NullPointerException("Pattern computed null result. To allow this pattern to return nullable value, consider using Patterns#getOptionally or so."));
    }

    /**
     * キーが最初に該当するパターンの返却値のをOptionalを取得します。
     * 該当するパターンがない場合やパターンの返却値がnullの場合、
     * Optionalは空となります。
     *
     * @param key キー
     * @return 該当するパターンがあればその返却値のOptional
     */
    @NonNull
    public Optional<V> getOptionally(K key) {
        return this.mappings.stream()
                .filter(entry -> entry._1().test(key))
                .findFirst()
                .map(_2mapWith((ignored, then) -> then.apply(key)));
    }

    /**
     * キーを、値の代わりに「値のOptional」にmapするFunctionを返します。
     *
     * @return このPatternでOptionallyにmapするFunction
     */
    public Function<K, Optional<V>> optional() {
        return this::getOptionally;
    }


    /**
     * キーを値またはデフォルト値にmapするFunctionを返します。
     *
     * @param defaultValue デフォルト値
     * @return このPatternでデフォルト値付きでmapするFunction
     */
    public Function<K, V> orElse(V defaultValue) {
        return this.optional().andThen(optional -> optional.orElse(defaultValue));
    }

    /**
     * キーを値またはデフォルト値にmapするFunctionを返します。
     *
     * @param defaultValueSupplier デフォルト値のSupplier
     * @return このPatternでデフォルト値付きでmapするFunction
     */
    public Function<K, V> orElseGet(Supplier<V> defaultValueSupplier) {
        return this.optional().andThen(optional -> optional.orElseGet(defaultValueSupplier));
    }

    /**
     * キーを値にmapし、キーが未設定の場合例外を送出するFunctionを返します。
     *
     * @param exceptionSupplier 指定例外のSupplier
     * @return このPatternでmapするFunction Functionはキーが未設定の場合指定例外を送出する
     */
    public Function<K, V> orElseThrow(Supplier<RuntimeException> exceptionSupplier) {
        return this.optional().andThen(optional -> optional.orElseThrow(exceptionSupplier));
    }

    @Override
    public V apply(K k) {
        return this.get(k);
    }

    /**
     * キーがwhenを満たした場合に返却する値を指定することで
     * その値を返却する関数を返却します。
     * 関数でなく返却値を直接設定できます。
     *
     * @param value 返却する値
     * @param <S>   キー型
     * @param <O>   値型
     * @return パターンに設定する返却関数(固定値返却)
     */
    public static <S, O> Function<S, O> then(O value) {
        return anything -> value;
    }

    /**
     * キーがwhenを満たした場合に値を返却するsupplierを指定することで
     * 入力値を無視してその値を返却する関数を返却します。
     *
     * @param supplier 返却する値のsupplier
     * @param <S>      キー型
     * @param <O>      値型
     * @return パターンに設定する返却関数
     */
    public static <S, O> Function<S, O> thenSupply(Supplier<? extends O> supplier) {
        return anything -> supplier.get();
    }

    /**
     * キーがwhenを満たした場合にキーに適用して返却する関数を明示します。
     * (実はthenApplyを書かなくても直接whenの第2引数に関数を指定すれば動きます
     *
     * @param function 適用する関数
     * @param <S>      キー型
     * @param <O>      関数の戻り型
     * @return パターンに設定する返却関数
     */
    public static <S, O> Function<S, O> thenApply(Function<? super S, ? extends O> function) {
        return function::apply;
    }

    /**
     * パターンを定義します。
     *
     * @param when      キーがこのパターンに該当する条件
     * @param thenApply キーがこのパターンに該当する場合、キーに適用する関数
     * @param <S>       キー型
     * @param <O>       関数の戻り型
     * @return パターン
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> when(Predicate<? super S> when, Function<? super S, ? extends O> thenApply) {
        return tuple(when::test, thenApply::apply);
    }

    /**
     * 引数と等価であることをwhenと定義します。
     *
     * @param target キーとの比較対象
     * @param <S>    キー型
     * @return 等価判定
     */
    public static <S> Predicate<S> equalsTo(S target) {
        return s -> Objects.equals(s, target);
    }

    /**
     * あらゆるキーに該当するパターンを返却します。
     * デフォルト値の設定に有用です。
     *
     * @param thenApply キーがこのパターンに該当する場合、キーに適用する関数
     * @param <S>キー型
     * @param <O>       関数の戻り型
     * @return デフォルトパターン
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElse(Function<? super S, ? extends O> thenApply) {
        return tuple(anything -> true, thenApply::apply);
    }

    /**
     * あらゆるキーに対し例外を送出させます。
     * 想定外パターンの対応に有用です。
     *
     * @param thenApply キーがこのパターンに該当する場合、キーに適用して例外を生成する関数
     * @param <S>キー型
     * @param <O>       関数の戻り型
     * @return 例外処理送出処理
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElseThrow(Function<? super S, RuntimeException> thenApply) {
        return tuple(anything -> true, input -> {
            throw thenApply.apply(input);
        });
    }

    /**
     * クラスによるパターンマッチを定義します。
     *
     * @param clazz     キーがこのパターンに該当する条件となるクラス
     * @param thenApply キーがこのパターンに該当する場合、clazzクラスにキャストしたキーに適用する関数
     * @param <S>       キー型
     * @param <O>       関数の戻り型
     * @return パターン
     */
    public static <S, T extends S, O> Tuple2<Predicate<S>, Function<S, O>> whenMatch(Class<T> clazz, Function<? super T, ? extends O> thenApply) {
        return tuple(clazz::isInstance, instance -> thenApply.apply(clazz.cast(instance)));
    }

    /**
     * クラスによるパターンマッチを定義します。当該クラスのインスタンスとしたうえで満たすべき述語を定義できます。
     *
     * @param clazz     キーがこのパターンに該当する条件となるクラス
     * @param when      上記クラスかつ満たすべき条件
     * @param thenApply キーがこのパターンに該当する場合、clazzクラスにキャストしたキーに適用する関数
     * @param <S>       キー型
     * @param <O>       関数の戻り型
     * @return パターン
     */
    public static <S, T extends S, O> Tuple2<Predicate<S>, Function<S, O>> whenMatch(Class<T> clazz, Predicate<? super T> when, Function<? super T, ? extends O> thenApply) {
        return tuple(key -> clazz.isInstance(key) && when.test(clazz.cast(key)), instance -> thenApply.apply(clazz.cast(instance)));
    }

}

