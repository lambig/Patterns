# 開発・検証

ビルドにはJDK 17以上、導入例の確認にはMaven 3.8以上、Python 3、POSIXシェルを使います。
Gradleは同梱Wrapperから取得します。ライブラリと導入例はJava 11のAPI・バイトコードを対象にコンパイルします。
`TEST_JAVA_HOME`を指定すると、試験と導入例をそのJavaで実行できます。
CIはビルドJDK 17を用い、実行環境Java 11／25を確認します。

```sh
sh ./verify-consumers.sh candidate
```

このコマンドは単体試験、sources/Javadoc JAR、1.3.1との公開シグネチャ比較、
旧版でコンパイルした利用コード、READMEのプログラム、独立したMaven／Gradle導入例を確認します。
候補版は`build/repository`に生成されます。導入例は一時キャッシュを作り、
Patternsだけを直接の依存として宣言してコンパイル・実行します。
結果は`build/consumer-results/candidate`、試験詳細は`build/reports/tests/test`に残ります。

## 導入例を個別に実行する

```sh
sh ./gradlew publishMavenJavaPublicationToCandidateRepository
sh ./gradlew -p example -PcandidateRepository="$PWD/build/repository" runExample
mvn -f example/pom.xml -DcandidateRepository="file://$PWD/build/repository" \
  compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "example/target/classes:$(cat example/target/runtime-classpath.txt)" example.PatternExample
```

出力は次のとおりです。

```text
classification=large; fallback=unknown; events=[large:12, other:0]; tuple-api=OK
```

次のコマンドで、Maven Centralの1.3.2を新しいキャッシュへ取得し、
同じ導入例とREADMEのプログラムを確認できます。

```sh
sh ./verify-consumers.sh public 1.3.2
```

## 公開用bundle

公開用のASCII形式秘密鍵を`SIGNING_KEY`、そのパスフレーズを`SIGNING_PASSWORD`に設定して実行します。
認証情報は環境変数で渡します。

```sh
sh ./gradlew clean check -PreleaseBundle centralBundle
```

`build/central/Patterns-1.3.2-central.zip`には本体、sources、Javadoc、POM、
Gradle Module Metadataと、それぞれの署名・チェックサムが入ります。
公開者は対象コミット、検証結果とbundle内容を確認したうえでCentral Portalへ登録します。
公開後は上記の`public`による取得・導入確認を行います。
