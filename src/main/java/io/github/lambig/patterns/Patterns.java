package io.github.lambig.patterns;

import static io.github.lambig.tuplite._2.Tuple2._2mapWith;
import static io.github.lambig.tuplite._2.Tuple2.tuple;
import static java.util.stream.Collectors.toList;


import io.github.lambig.tuplite._2.Tuple2;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 疑似パターンマッチ定義クラス
 * パターンは定義順に評価します。
 *
 * @param <K> キー型
 * @param <V> 処理結果の戻り型
 */
public class Patterns<K, V> implements Function<K, V> {

  private final List<Tuple2<Predicate<K>, Function<K, V>>> map;

  public Patterns(List<Tuple2<Predicate<K>, Function<K, V>>> patterns) {
    this.map = patterns;
  }

  @SafeVarargs
  public static <S, O> Patterns<S, O> patterns(Tuple2<Predicate<S>, Function<S, O>>... patterns) {
    return patterns(Stream.of(patterns).collect(toList()));
  }

  public static <S, O> Patterns<S, O> patterns(List<Tuple2<Predicate<S>, Function<S, O>>> patterns) {
    return new Patterns<>(patterns);
  }

  /**
   * キーが最初に該当するパターンの返却値を取得します
   *
   * @param key キー
   * @return 該当するパターンがあればその返却値、なければnull
   */
  public V get(K key) {
    return this.map.stream()
        .filter(entry -> entry._1().test(key))
        .findFirst()
        .map(_2mapWith((ignored, then) -> then.apply(key)))
        .orElse(null);
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
  public static <S, O> Function<S, O> thenSupply(Supplier<O> supplier) {
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
  public static <S, O> Function<S, O> thenApply(Function<S, O> function) {
    return function;
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
  public static <S, O> Tuple2<Predicate<S>, Function<S, O>> when(Predicate<S> when, Function<S, O> thenApply) {
    return tuple(when, thenApply);
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
  public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElse(Function<S, O> thenApply) {
    return tuple(anything -> true, thenApply);
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
  public static <S, O> Tuple2<Predicate<S>, Function<S, O>> orElseThrow(Function<S, RuntimeException> thenApply) {
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
  public static <S, T extends S, O> Tuple2<Predicate<S>, Function<S, O>> whenMatch(Class<T> clazz, Function<T, O> thenApply) {
    return tuple(clazz::isInstance, instance -> thenApply.apply(clazz.cast(instance)));
  }
}

