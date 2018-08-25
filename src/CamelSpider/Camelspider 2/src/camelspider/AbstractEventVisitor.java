package camelspider;

public interface AbstractEventVisitor {
	/**
	 * This method is called by DFS algorithm each time a node has to be visited
	 * @param event
	 *  the event to be visited
	 * @return
	 *  true: if the search must be terminated immediately, and
	 *  false: the search may continue.
	 */
	public boolean visit (AbstractEvent event);
}
