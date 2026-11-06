package io.github.lambig.patterns;

import io.github.lambig.tuplite._2.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import static io.github.lambig.tuplite._2.Tuple2._2mapWith;
import static io.github.lambig.tuplite._2.Tuple2.tuple;
import static java.util.stream.Collectors.toList;

/**
 * 入力に最初に一致する条件の結果を返すFunction。
 *
 * <p>条件の並び順が優先順位です。分類名や料金など、入力に応じたひとつの結果を求める場合に使えます。
 * {@link #get(Object)}は値が得られる入力に、{@link #getOptionally(Object)}は
 * 一致する条件や値がない入力も含めて扱う場合に使います。
 *
 * <p>作成後に元のリストや配列の要素を入れ替えても、条件と関数の組合せ・順序は保たれます。
 * 条件・関数が参照する状態は共有され、入力がnullの場合の結果もそれらの定義によります。
 * 条件や関数の例外は呼出元へ伝わり、その入力に対する処理はそこで終了します。
 *
 * @param <K> 入力の型
 * @param <V> 結果の型
 */
public class Patterns<K, V> implements Function<K, V> {

    private final List<Tuple2<Predicate<K>, Function<K, V>>> mappings;

    /**
     * 指定順を優先順位として、最初に一致する条件の結果を返す関数を作ります。
     *
     * <p>元のリストの変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param patterns 条件と結果を求める関数の組。各組とその要素は非null
     * @throws NullPointerException patterns自体がnullの場合
     */
    public Patterns(List<Tuple2<Predicate<K>, Function<K, V>>> patterns) {
        this.mappings = new ArrayList<>(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の結果を返す関数を作ります。
     *
     * <p>元の配列の変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param patterns 条件と結果を求める関数の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    @SafeVarargs
    public static <S, O> Patterns<S, O> of(Tuple2<Predicate<S>, Function<S, O>>... patterns) {
        return patterns(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の結果を返す関数を作ります。
     *
     * <p>元のリストの変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param patterns 条件と結果を求める関数の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    public static <S, O> Patterns<S, O> of(List<Tuple2<Predicate<S>, Function<S, O>>> patterns) {
        return patterns(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の結果を返す関数を作ります。
     *
     * <p>元の配列の変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param patterns 条件と結果を求める関数の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    @SafeVarargs
    public static <S, O> Patterns<S, O> patterns(Tuple2<Predicate<S>, Function<S, O>>... patterns) {
        return patterns(Stream.of(patterns).collect(toList()));
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の結果を返す関数を作ります。
     *
     * <p>元のリストの変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param patterns 条件と結果を求める関数の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    public static <S, O> Patterns<S, O> patterns(List<Tuple2<Predicate<S>, Function<S, O>>> patterns) {
        return new Patterns<>(patterns);
    }

    /**
     * 入力に最初に一致する条件の結果を返します。
     *
     * @param key 入力
     * @return 一致した条件の非nullの結果
     * @throws NoSuchPatternException 一致する条件がない場合
     * @throws NullPointerException 一致した条件の関数がnullを返した場合
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
     * 入力に最初に一致する条件の結果をOptionalで返します。
     *
     * <p>最初に一致した条件の結果がnullの場合も、その入力の結果は空となります。後続の条件は対象になりません。
     *
     * @param key 入力
     * @return 結果を含むOptional。条件の未一致または結果がnullなら空のOptional
     */
    @NonNull
    public Optional<V> getOptionally(K key) {
        return this.mappings.stream()
                .filter(entry -> entry._1().test(key))
                .findFirst()
                .map(_2mapWith((ignored, then) -> then.apply(key)));
    }

    /**
     * 入力に対する結果の有無をOptionalで返す関数を返します。
     *
     * <p>最初に一致した条件の結果がnullの場合も、その入力の結果は空となります。後続の条件は対象になりません。
     *
     * @return 結果をOptionalに包む関数。条件の未一致または結果がnullなら空のOptional
     */
    public Function<K, Optional<V>> optional() {
        return this::getOptionally;
    }

    /**
     * 条件の未一致または結果がnullの入力に、既定値を返す関数を返します。
     *
     * @param defaultValue 結果が空の場合の値。nullも指定可能
     * @return 結果があればその値、空なら既定値を返す関数
     */
    public Function<K, V> orElse(V defaultValue) {
        return this.optional().andThen(optional -> optional.orElse(defaultValue));
    }

    /**
     * 条件の未一致または結果がnullの入力に、Supplierから得た値を返す関数を返します。
     *
     * <p>Supplierは結果が空の入力ごとに呼ばれ、その結果がnullの場合はnullを返します。
     *
     * @param defaultValueSupplier 結果が空の場合に値を提供するSupplier
     * @return 結果があればその値、空ならSupplierの結果を返す関数
     */
    public Function<K, V> orElseGet(Supplier<V> defaultValueSupplier) {
        return this.optional().andThen(optional -> optional.orElseGet(defaultValueSupplier));
    }

    /**
     * 条件の未一致または結果がnullの入力に、指定した例外を送出する関数を返します。
     *
     * @param exceptionSupplier 結果が空の場合に送出する例外を提供するSupplier
     * @return 結果を返し、空なら指定した例外を送出する関数
     */
    public Function<K, V> orElseThrow(Supplier<RuntimeException> exceptionSupplier) {
        return this.optional().andThen(optional -> optional.orElseThrow(exceptionSupplier));
    }

    /**
     * 入力に最初に一致する条件の結果を返します。
     *
     * @param k 入力
     * @return 一致した条件の非nullの結果
     * @throws NoSuchPatternException 一致する条件がない場合
     * @throws NullPointerException 一致した条件の関数がnullを返した場合
     */
    @Override
    public V apply(K k) {
        return this.get(k);
    }

    /**
     * 指定した値を結果とする関数を返します。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param value 結果の値。nullも指定可能
     * @return どの入力にもvalueを返す関数
     */
    public static <S, O> Function<S, O> then(O value) {
        return anything -> value;
    }

    /**
     * Supplierが提供する値を結果とする関数を返します。
     *
     * <p>whenの結果に指定すると、その条件が選ばれるたびにSupplierが呼ばれます。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param supplier 結果を提供するSupplier
     * @return 入力ごとにSupplierから結果を得る関数
     */
    public static <S, O> Function<S, O> thenSupply(Supplier<? extends O> supplier) {
        return anything -> supplier.get();
    }

    /**
     * 入力を指定した関数で変換した結果を返す関数を返します。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param function 入力から結果を求める関数
     * @return functionと同じ結果を返す関数
     * @throws NullPointerException functionがnullの場合
     */
    public static <S, O> Function<S, O> thenApply(Function<? super S, ? extends O> function) {
        return function::apply;
    }

    /**
     * 条件と、一致した入力から結果を求める関数をひとつの規則として返します。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param when この規則に該当する入力の条件
     * @param thenApply 一致した入力に適用する関数
     * @return 第1要素が条件、第2要素が関数のTuple2
     * @throws NullPointerException 条件または関数がnullの場合
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> when(Predicate<? super S> when, Function<? super S, ? extends O> thenApply) {
        return tuple(when::test, thenApply::apply);
    }

    /**
     * 入力が比較対象と等しいかを表す述語を返します。
     *
     * @param <S> 入力の型
     * @param target 比較対象。nullも指定可能
     * @return Objects.equalsで入力とtargetが等しい場合にtrueとなる述語
     */
    public static <S> Predicate<S> equalsTo(S target) {
        return s -> Objects.equals(s, target);
    }

    /**
     * すべての入力に一致する既定の規則を返します。
     *
     * <p>先行する条件がすべて未一致だった入力を扱うには、最後の規則として指定します。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param thenApply 既定の結果を求める関数
     * @return 常に一致する条件と関数の組
     * @throws NullPointerException thenApplyがnullの場合
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElse(Function<? super S, ? extends O> thenApply) {
        return tuple(anything -> true, thenApply::apply);
    }

    /**
     * すべての入力に一致し、その入力から作った例外を送出する規則を返します。
     *
     * <p>最後の規則として指定すると、先行する条件がすべて未一致だった入力を例外として報告できます。
     *
     * @param <S> 入力の型
     * @param <O> 結果の型
     * @param thenApply 入力に応じた例外を作る関数
     * @return 常に一致する条件と例外を送出する処理の組
     */
    public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElseThrow(Function<? super S, RuntimeException> thenApply) {
        return tuple(anything -> true, input -> {
            throw thenApply.apply(input);
        });
    }

    /**
     * 指定した型のインスタンスに一致する規則を返します。
     *
     * <p>入力がnullの場合は未一致となります。
     *
     * @param <S> 入力の型
     * @param <T> 条件が対象とする型
     * @param <O> 結果の型
     * @param clazz 一致する型。サブクラス・実装クラスのインスタンスも対象
     * @param thenApply 型Tの入力に適用する関数
     * @return 型の条件と関数の組
     * @throws NullPointerException clazzがnullの場合
     */
    public static <S, T extends S, O> Tuple2<Predicate<S>, Function<S, O>> whenMatch(Class<T> clazz, Function<? super T, ? extends O> thenApply) {
        return tuple(clazz::isInstance, instance -> thenApply.apply(clazz.cast(instance)));
    }

    /**
     * 指定した型のインスタンスで、追加の条件も満たす入力に一致する規則を返します。
     *
     * <p>入力がnullの場合は未一致となります。追加の条件は型が一致した入力だけを受け取ります。
     *
     * @param <S> 入力の型
     * @param <T> 条件が対象とする型
     * @param <O> 結果の型
     * @param clazz 一致する型。サブクラス・実装クラスのインスタンスも対象
     * @param when 型が一致した入力に求める追加の条件
     * @param thenApply 型Tの入力に適用する関数
     * @return 型の条件と関数の組
     */
    public static <S, T extends S, O> Tuple2<Predicate<S>, Function<S, O>> whenMatch(Class<T> clazz, Predicate<? super T> when, Function<? super T, ? extends O> thenApply) {
        return tuple(key -> clazz.isInstance(key) && when.test(clazz.cast(key)), instance -> thenApply.apply(clazz.cast(instance)));
    }

}
