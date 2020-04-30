package cn.hpc.util;

public class MyException extends RuntimeException {

    public MyException() {
        super();
    }

    public MyException(String mes, Throwable cause, boolean str, boolean str1) {
        super(mes, cause, str, str1);
    }

    public MyException(String mes, Throwable cause) {
        super(mes, cause);
    }

    public MyException(String mes) {
        super(mes);
    }

    public MyException(Throwable cause) {
        super(cause);
    }
}
