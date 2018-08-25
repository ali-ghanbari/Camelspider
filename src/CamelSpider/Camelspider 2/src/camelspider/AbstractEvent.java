package camelspider;

public abstract class AbstractEvent implements Cloneable {
	protected String eventID;
	protected ThreadID threadID;
	private boolean mark;
	
	public AbstractEvent (ThreadID threadID) {
		eventID = LabelGenerator.getNext();
		this.threadID = threadID;
		mark = false;
	}
	
	public void markEvent () {
		mark = true;
	}
	
	public boolean marked () {
		return mark;
	}
	
	public String getEventID () {
		return eventID;
	}
	
	public void setEventID (String eventID) {
		this.eventID = eventID; 
	}
	
	public ThreadID getThreadID () {
		return threadID;
	}
	
	public void setThreadID (ThreadID threadID) {
		this.threadID = threadID;
	}
	
	public void instantiateEventID (String eidPrefix) {
		eventID = eidPrefix + eventID;
	}
	
	public void instantiateThreadID (String tidPrefix, Multiplicities m) {
		if (threadID != null) {
			threadID.setThreadID(tidPrefix + threadID.getThreadID());
			threadID.setMultiplicities(threadID.getMultiplicities().oTimes(m));
		}
	}
	
	private boolean isMonitorEvent (AbstractEvent e) {
		return e instanceof AbstractEnterMonitorEvent || e instanceof AbstractExitMonitorEvent;
	}
	
	public boolean isMonitorEvent () {
		return isMonitorEvent(this);
	}
	
	public boolean mayMatch(AbstractEvent e2) {
		boolean res;
		
		res = isMonitorEvent (this) && isMonitorEvent(e2);
		res = res && (!this.getClass().getName().equals(e2.getClass().getName()));
		if (getEventLoci () != null && e2.getEventLoci() != null)
			return res && getEventLoci().equals(e2.getEventLoci());
		return false;
	}
	
	private boolean isAccessEvent (AbstractEvent e) {
		return e instanceof AbstractReadEvent || e instanceof AbstractWriteEvent; 
	}
	
	public boolean conf(AbstractEvent e) {
		if(!isAccessEvent(this) || !isAccessEvent(e))
			return false;
		if((this instanceof AbstractWriteEvent && isAccessEvent(e)) 
				|| (e instanceof AbstractWriteEvent && isAccessEvent(this)))
			if(!threadID.getThreadID().equals(e.threadID.getThreadID()))
				if (((FieldObjectLoci) getEventLoci()).hasNonEmptyIntersection((FieldObjectLoci) e.getEventLoci()))
					return true;
		return false;
	}

	public boolean mwr() {
		return (this instanceof AbstractWriteEvent) && threadID.getMultiplicities() == Multiplicities.MULTIPLE;
	}
	
	public String toString () {
		if (this instanceof AbstractNeutralEvent)
			return "NEUTRAL EVENT " + eventID;
		String kind = null;
		
		if (this instanceof AbstractEnterMonitorEvent)
			kind = "N";
		else if (this instanceof AbstractExitMonitorEvent)
			kind = "X";
		else if (this instanceof AbstractReadEvent)
			kind = "R";
		else if (this instanceof AbstractWriteEvent)
			kind = "W";
		
		return kind + " <" + eventID + ", " + threadID + ", " + getEventLoci() + ">";
	}
	
	public abstract Object getEventLoci ();
	
	public abstract AbstractEvent clone ();
	
	public abstract boolean equals (Object o);
	
	public abstract int hasCode ();
}
