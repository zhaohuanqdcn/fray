package cmu.pasta.fray.runtime;

public class TargetTerminateException extends Error {
    private final int status;
    public TargetTerminateException(int stats) {
        super();
        this.status = stats;
    }

    public int getStatus() {
        return status;
    }

}
