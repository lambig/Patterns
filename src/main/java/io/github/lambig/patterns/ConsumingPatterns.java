package io.github.lambig.patterns;

import io.github.lambig.tuplite._2.Tuple2;
import java.util.ArrayList;
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
 * 入力に最初に一致する条件の処理を実行するConsumer。
 *
 * <p>条件の並び順が優先順位です。一致した処理だけを実行するため、通知先の振り分けなどに使えます。
 * 一致する条件がない入力は{@link #handle(Object)}でNoSuchPatternExceptionとなり、
 * {@link #orElseDo(Consumer)}で既定の処理を選べます。
 *
 * <p>作成後に元のリストや配列の要素を入れ替えても、条件と処理の組合せ・順序は保たれます。
 * 条件・処理が参照する状態は共有され、入力がnullの場合の結果もそれらの定義によります。
 * 条件や処理の例外は呼出元へ伝わり、その入力に対する処理はそこで終了します。
 *
 * @param <K> 入力の型
 */
public class ConsumingPatterns<K> implements Consumer<K> {

    private final List<Tuple2<Predicate<K>, Consumer<K>>> mappings;

    private ConsumingPatterns(@NonNull List<Tuple2<Predicate<K>, Consumer<K>>> patterns) {
        this.mappings = new ArrayList<>(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の処理を行う振り分けを作ります。
     *
     * <p>元の配列の変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param patterns 条件と処理の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うConsumingPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    @SafeVarargs
    public static <S> ConsumingPatterns<S> of(@NonNull Tuple2<Predicate<S>, Consumer<S>>... patterns) {
        return consumingPatterns(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の処理を行う振り分けを作ります。
     *
     * <p>元のリストの変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param patterns 条件と処理の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うConsumingPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    public static <S> ConsumingPatterns<S> of(@NonNull List<Tuple2<Predicate<S>, Consumer<S>>> patterns) {
        return consumingPatterns(patterns);
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の処理を行う振り分けを作ります。
     *
     * <p>元の配列の変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param patterns 条件と処理の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うConsumingPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    @SafeVarargs
    public static <S> ConsumingPatterns<S> consumingPatterns(@NonNull Tuple2<Predicate<S>, Consumer<S>>... patterns) {
        return consumingPatterns(Stream.of(patterns).collect(toList()));
    }

    /**
     * 指定順を優先順位として、最初に一致する条件の処理を行う振り分けを作ります。
     *
     * <p>元のリストの変更にかかわらず、作成時の組合せ・順序を保持します。空の指定ではすべての入力が未一致となります。
     *
     * @param <S> 入力の型
     * @param patterns 条件と処理の組。各組とその要素は非null
     * @return 指定した条件と順序で入力を扱うConsumingPatterns
     * @throws NullPointerException patterns自体がnullの場合
     */
    public static <S> ConsumingPatterns<S> consumingPatterns(@NonNull List<Tuple2<Predicate<S>, Consumer<S>>> patterns) {
        return new ConsumingPatterns<>(patterns);
    }

    /**
     * 入力に最初に一致する条件の処理を実行します。
     *
     * @param key 入力
     * @throws NoSuchPatternException 一致する条件がない場合
     */
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
     * 一致する条件がない入力に、既定の処理を行うConsumerを返します。
     *
     * @param defaultConsumer 未一致の入力に対する処理
     * @return 一致した条件の処理、または既定の処理を行うConsumer
     * @throws NullPointerException defaultConsumerがnullの場合
     */
    public Consumer<K> orElseDo(@NonNull Consumer<K> defaultConsumer) {
        return k -> this.getConsumer(k).orElse(defaultConsumer).accept(k);
    }

    /**
     * 一致する条件がない入力に、指定した例外を送出するConsumerを返します。
     *
     * @param exceptionSupplier 未一致の場合に送出する例外を提供するSupplier
     * @return 一致した処理を実行し、未一致なら指定した例外を送出するConsumer
     * @throws NullPointerException exceptionSupplierがnullの場合
     */
    public Consumer<K> orElseThrow(@NonNull Supplier<RuntimeException> exceptionSupplier) {
        return k -> this.getConsumer(k).orElseThrow(exceptionSupplier).accept(k);
    }

    /**
     * 入力に最初に一致する条件の処理を実行します。
     *
     * @param k 入力
     * @throws NoSuchPatternException 一致する条件がない場合
     */
    @Override
    public void accept(K k) {
        this.handle(k);
    }

    /**
     * 入力に指定した処理を行うConsumerを返します。
     *
     * @param <S> 入力の型
     * @param consumer 入力に対する処理
     * @return 指定した処理を行うConsumer
     * @throws NullPointerException consumerがnullの場合
     */
    public static <S> Consumer<S> thenAcceptWith(@NonNull Consumer<? super S> consumer) {
        return consumer::accept;
    }

    /**
     * 条件と、一致した入力に対する処理をひとつの規則として返します。
     *
     * @param <S> 入力の型
     * @param when この規則に該当する入力の条件
     * @param consumer 一致した入力に適用する処理
     * @return 第1要素が条件、第2要素が処理のTuple2
     * @throws NullPointerException 条件または処理がnullの場合
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> when(@NonNull Predicate<? super S> when, @NonNull Consumer<? super S> consumer) {
        return tuple(when::test, consumer::accept);
    }

    /**
     * 入力が比較対象と等しいかを表す述語を返します。
     *
     * @param <S> 入力の型
     * @param target 比較対象。非null
     * @return Objects.equalsで入力とtargetが等しい場合にtrueとなる述語
     * @throws NullPointerException targetがnullの場合
     */
    public static <S> Predicate<S> equalsTo(@NonNull S target) {
        return s -> Objects.equals(s, target);
    }

    /**
     * すべての入力に一致する既定の規則を返します。
     *
     * <p>先行する条件がすべて未一致だった入力を扱うには、最後の規則として指定します。
     *
     * @param <S> 入力の型
     * @param thenAcceptWith 既定の処理
     * @return 常に一致する条件と処理の組
     * @throws NullPointerException thenAcceptWithがnullの場合
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> orElse(@NonNull Consumer<? super S> thenAcceptWith) {
        return tuple(anything -> true, thenAcceptWith::accept);
    }

    /**
     * すべての入力に一致し、その入力から作った例外を送出する規則を返します。
     *
     * <p>最後の規則として指定すると、先行する条件がすべて未一致だった入力を例外として報告できます。
     *
     * @param <S> 入力の型
     * @param thenAcceptWith 入力に応じた例外を作る関数
     * @return 常に一致する条件と例外を送出する処理の組
     * @throws NullPointerException thenAcceptWithがnullの場合
     */
    public static <S> Tuple2<Predicate<S>, Consumer<S>> orElseThrow(@NonNull Function<? super S, @NonNull RuntimeException> thenAcceptWith) {
        return tuple(anything -> true, input -> {
            throw thenAcceptWith.apply(input);
        });
    }

    /**
     * 指定した型のインスタンスに一致する規則を返します。
     *
     * <p>入力がnullの場合は未一致となります。
     *
     * @param <S> 入力の型
     * @param <T> 条件が対象とする型
     * @param clazz 一致する型。サブクラス・実装クラスのインスタンスも対象
     * @param thenAcceptWith 型Tの入力に適用する処理
     * @return 型の条件と処理の組
     * @throws NullPointerException clazzまたはthenAcceptWithがnullの場合
     */
    public static <S, T extends S> Tuple2<Predicate<S>, Consumer<S>> whenMatch(@NonNull Class<T> clazz, @NonNull Consumer<? super T> thenAcceptWith) {
        return tuple(clazz::isInstance, instance -> thenAcceptWith.accept(clazz.cast(instance)));
    }

    /**
     * 指定した型のインスタンスで、追加の条件も満たす入力に一致する規則を返します。
     *
     * <p>入力がnullの場合は未一致となります。追加の条件は型が一致した入力だけを受け取ります。
     *
     * @param <S> 入力の型
     * @param <T> 条件が対象とする型
     * @param clazz 一致する型。サブクラス・実装クラスのインスタンスも対象
     * @param when 型が一致した入力に求める追加の条件
     * @param thenAcceptWith 型Tの入力に適用する処理
     * @return 型の条件と処理の組
     * @throws NullPointerException clazzまたはthenAcceptWithがnullの場合
     */
    public static <S, T extends S> Tuple2<Predicate<S>, Consumer<S>> whenMatch(@NonNull Class<T> clazz, Predicate<? super T> when, @NonNull Consumer<? super T> thenAcceptWith) {
        return tuple(key -> clazz.isInstance(key) && when.test(clazz.cast(key)), instance -> thenAcceptWith.accept(clazz.cast(instance)));
    }

}
