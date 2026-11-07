package io.github.lambig.patterns;

/**
 * 入力に一致する条件がなく、結果または処理を選べなかったことを表す例外。
 * {@link Patterns#get(Object)}と{@link ConsumingPatterns#handle(Object)}から送出されます。
 */
public class NoSuchPatternException extends RuntimeException {
    /** 詳細メッセージと原因が空の例外を作ります。 */
    public NoSuchPatternException() {
        this(null, null);
    }

    /**
     * 詳細メッセージを持つ例外を作ります。
     * @param message 入力や該当条件に関する説明。nullも指定可能
     */
    public NoSuchPatternException(String message) {
        this(message, null);
    }

    /**
     * 原因と、その詳細メッセージを持つ例外を作ります。
     * @param cause 原因。nullの場合は詳細メッセージもnull
     */
    public NoSuchPatternException(Throwable cause) {
        this(cause == null ? null : cause.getMessage(), cause);
    }

    /**
     * 指定した詳細メッセージと原因を持つ例外を作ります。
     * @param message 詳細メッセージ。nullも指定可能
     * @param cause 原因。nullも指定可能
     */
    public NoSuchPatternException(String message, Throwable cause) {
        super(message);
        if (cause != null) initCause(cause);
    }
}
