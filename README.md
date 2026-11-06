[![CI](https://github.com/lambig/Patterns/actions/workflows/ci.yml/badge.svg)](https://github.com/lambig/Patterns/actions/workflows/ci.yml)

# Patterns

入力に応じた値や処理を、優先順位のある条件の組で表すJavaライブラリです。
`Patterns<K, V>`は分類名や料金などの結果を返す`Function<K, V>`、
`ConsumingPatterns<K>`は通知先の振り分けなどを行う`Consumer<K>`として使えます。
どちらも、最初に一致した条件の結果・処理がその入力に対する答えになります。

## 導入

Java 11以上が対象です。このブランチは1.3.2の公開候補です。
Maven Centralへの公開後は、以下の依存宣言で利用できます。
候補版を試す手順は[開発・検証手順](DEVELOPING.md)にあります。

Gradle:

```groovy
repositories { mavenCentral() }
dependencies { implementation 'io.github.lambig:Patterns:1.3.2' }
```

Maven:

```xml
<dependency>
  <groupId>io.github.lambig</groupId>
  <artifactId>Patterns</artifactId>
  <version>1.3.2</version>
</dependency>
```

公開APIに現れる`Tuple2`はTuplite 1.0.1から利用できます。
Patternsの依存宣言でTupliteとfunctional-interface-extension 1.1.3も解決されます。

## 入力から分類を得る

```java
import io.github.lambig.patterns.Patterns;
import java.util.List;
import java.util.stream.Collectors;
import static io.github.lambig.patterns.Patterns.then;
import static io.github.lambig.patterns.Patterns.when;

public class ClassificationExample {
    public static void main(String[] args) {
        Patterns<Integer, String> classification = Patterns.of(
                when(n -> n >= 10, then("large")),
                when(n -> n > 0, then("small")));

        System.out.println(classification.get(12));
        System.out.println(classification.getOptionally(0));
        System.out.println(List.of(12, 3, 0).stream()
                .map(classification.orElse("unknown"))
                .collect(Collectors.toList()));
    }
}
```

出力:

```text
large
Optional.empty
[large, small, unknown]
```

12は両方の条件に一致するため、先に指定した`large`になります。
具体的な条件を先に、広い条件や既定の規則を後に置くことで優先順位を表現できます。
最初に一致した条件より後の述語と関数は呼ばれません。

結果の使い方は次のように選べます。

| 呼び出し | 結果が得られる場合 | 条件が未一致の場合 | 最初に一致した関数の結果がnullの場合 |
| --- | --- | --- | --- |
| `get(key)` / `apply(key)` | 結果の値 | `NoSuchPatternException` | `NullPointerException` |
| `getOptionally(key)` / `optional().apply(key)` | 値を含むOptional | 空のOptional | 空のOptional |
| `orElse(value).apply(key)` | 結果の値 | 指定した値 | 指定した値 |
| `orElseGet(supplier).apply(key)` | 結果の値 | Supplierの結果 | Supplierの結果 |
| `orElseThrow(supplier).apply(key)` | 結果の値 | Supplierが返す例外 | Supplierが返す例外 |

`orElseGet`のSupplierは、結果が空の入力ごとに呼ばれます。
最初に一致した関数がnullを返す場合も、その入力の結果は空で確定します。
`Patterns.orElse(function)`はすべての入力に一致する**規則**です。
最後に置くと、先行する条件がすべて未一致だった入力を扱えます。
対して、インスタンスの`classification.orElse(value)`は、上表のとおり結果の空も補います。

## 値・型に応じた条件と結果

`when(predicate, function)`は、条件と結果を求める関数を組にします。
`then(value)`は固定値、`thenSupply(supplier)`はその規則が選ばれるたびに得る値、
`thenApply(function)`は入力を変換した結果を表します。
`when`の第2引数にはラムダ式やメソッド参照を直接指定できます。

`whenMatch(Integer.class, function)`はIntegerの入力に一致します。
`whenMatch(Integer.class, predicate, function)`では、その型の値が満たす追加条件を指定できます。
対象の型のサブクラス・実装クラスも一致し、nullは未一致になります。
追加条件と関数は指定した型で入力を受け取れます。

## 入力に応じた処理を行う

```java
import io.github.lambig.patterns.ConsumingPatterns;
import java.util.ArrayList;
import java.util.List;

public class RoutingExample {
    public static void main(String[] args) {
        List<String> events = new ArrayList<>();
        ConsumingPatterns<Integer> routing = ConsumingPatterns.of(
                ConsumingPatterns.when(n -> n >= 10, n -> events.add("large:" + n)),
                ConsumingPatterns.when(n -> n > 0, n -> events.add("small:" + n)));
        routing.handle(12);
        routing.orElseDo(n -> events.add("other:" + n)).accept(0);
        System.out.println(events);
    }
}
```

出力は`[large:12, other:0]`です。
`handle`と`accept`は最初に一致した条件の処理をひとつ実行します。
未一致の入力には`NoSuchPatternException`となり、`orElseDo`で既定の処理、
`orElseThrow`で未一致時の例外を選べます。
ひとつの入力に対して該当する処理をすべて実行したい用途では、各条件を個別に判定する構成が適しています。

## null・例外・作成後の変更

入力のnullは、指定した述語・関数・処理が受け取ります。
`Patterns.equalsTo(null)`はnullの入力に一致します。
`ConsumingPatterns.equalsTo`の比較対象は非nullです。nullの入力を条件にする場合は`Objects::isNull`を使えます。
規則のリスト・配列、各規則、その条件と関数・処理には非nullの値を指定します。
空のリスト・可変長引数では、すべての入力が未一致になります。

述語・関数・処理からの例外は呼出元へ伝わり、その入力の判定・処理はそこで終了します。
`getOptionally`などで空を扱う場合も、例外はそのまま伝わります。
処理が例外の前に行った更新は、その処理の結果として残ります。

作成後に元のリスト・配列の要素を入れ替えても、規則の組合せと順序は保たれます。
述語・関数・処理が参照する状態は共有されるため、可変状態を参照する条件の答えは状態に応じて変わります。
複数スレッドから使う場合は、これらの処理と参照先の状態を、その実行方法に合わせて設計します。

## API文書と実行例

- [独立したMaven／Gradleプロジェクト](example/)は、分類、型条件、処理の振り分けと`Tuple2`を含むAPIを利用します。
- `./gradlew javadoc`で生成するAPI文書は`build/docs/javadoc/index.html`から参照できます。
- 公開物には同じ版の`Patterns-1.3.2-sources.jar`と`Patterns-1.3.2-javadoc.jar`が含まれます。
- [開発・検証手順](DEVELOPING.md)に候補版・公開版の導入確認と公開用bundleの作成手順を記載しています。
