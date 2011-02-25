package replicatorg.drivers;

/**
 * A retry exception indicates that there was a non-fatal problem with processing a command; ordinarily, that the
 * buffer is full.  The machine controller should retry the command.
 * @author phooky
 *
 */
public class RetryException extends Exception {
}
