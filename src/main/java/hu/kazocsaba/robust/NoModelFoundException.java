package hu.kazocsaba.robust;

/**
 * Thrown when a robust estimator couldn't find a model.
 * @author Kazó Csaba
 */
public class NoModelFoundException extends Exception {

	/**
	 * Creates a new instance of <code>NoModelFoundException</code> without detail message.
	 */
	public NoModelFoundException() {
	}

	/**
	 * Constructs an instance of <code>NoModelFoundException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public NoModelFoundException(String msg) {
		super(msg);
	}
}
