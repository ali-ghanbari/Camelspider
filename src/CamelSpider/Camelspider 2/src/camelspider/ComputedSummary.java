package camelspider;

import java.util.*;

public class ComputedSummary {
	private Object val;
	private boolean threadSummary;
	
	public ComputedSummary (AbstractEvent event) {
		val = event;
		threadSummary = false;
	}
	
	public ComputedSummary (AbstractExecution ae, boolean threadSummary) throws InappropriateSummaryException {
		Set <AbstractEvent> heads = ae.getHeads ();
		Set <AbstractEvent> tails = ae.getTails();
		
		if (heads == null || heads.size() > 1)
			if (tails == null || tails.size() > 1)
				throw new InappropriateSummaryException();
		val = ae;
		this.threadSummary = threadSummary;		
	}
	
	public Object getStoredValue () {
		return val;
	}
	
	public boolean isThreadSummary () {
		return threadSummary;
	}
	
	public AbstractEvent getEntry () {
		if (val instanceof AbstractEvent)
			return (AbstractEvent) val;
		return ((AbstractExecution) val).getHeads().iterator().next();
	}
	
	public AbstractEvent getExit () {
		if (val instanceof AbstractEvent)
			return (AbstractEvent) val;
		AbstractExecution ae = (AbstractExecution) val;
		
		return threadSummary ? ae.getHeads().iterator().next() : ae.getTails().iterator().next();
	}
}
