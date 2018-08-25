package camelspider;

public class ThreadID implements Cloneable {
	private String tid;
	private Multiplicities m;
	
	public ThreadID (String id, Multiplicities mult) {
		tid = id;
		m = mult;
	}
	
	public String getThreadID () {
		return tid;
	}
	
	public void setThreadID (String id) {
		tid = id;
	}
	
	public Multiplicities getMultiplicities () {
		return m;
	}
	
	public void setMultiplicities (Multiplicities mult) {
		m = mult;
	}
	
	public String toString () {
		return "(" + tid + ", " + m + ")";
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof ThreadID))
			return false;
		ThreadID obj = (ThreadID) o;
		
		return tid.equals(obj.tid) && m == obj.m;
	}
	
	public ThreadID clone () {
		return new ThreadID (tid, m);
	}
	
	public int hashCode () {
		return HashCodeUtilities.codePair(tid.hashCode() + 1, m.hasCode());
	}
}
