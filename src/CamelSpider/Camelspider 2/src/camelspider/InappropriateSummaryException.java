package camelspider;

public class InappropriateSummaryException extends Exception {
	private static final long serialVersionUID = -8513719323554473938L;

	public InappropriateSummaryException () {
		super ("In appropriate summary value has passed here! Pass anchored summaries.");
	}
}
