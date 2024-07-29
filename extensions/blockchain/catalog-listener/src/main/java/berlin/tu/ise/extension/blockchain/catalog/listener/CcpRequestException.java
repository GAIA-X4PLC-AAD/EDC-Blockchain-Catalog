package berlin.tu.ise.extension.blockchain.catalog.listener;

public class CcpRequestException extends Exception {
    public CcpRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public CcpRequestException(String message) {
        super(message);
    }
}
