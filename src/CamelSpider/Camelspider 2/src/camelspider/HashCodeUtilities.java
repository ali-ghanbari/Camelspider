package camelspider;

public class HashCodeUtilities {
	public static final int MONITOR_N_HASH = 1;
	public static final int MONITOR_X_HASH = 2;
	public static final int OBJECT_R_HASH = 3;
	public static final int OBJECT_W_HASH = 4;
	public static final int NEUTRAL_HASH = 5;
	
	public static int codePair (int a, int b) {
		if (b == 0)
			return a;
		int lenB = Integer.SIZE;
		int mask = 0;
		
		for (int i = 0; i < 31 && ((b << i) & 0x80000000) == 0; i ++)
			lenB = 31 - i;
		for (int i = 0; i < lenB; i ++)
			mask = (mask << 1) + 1;
		while ((a & mask) != 0) 
			a <<= 1;
		return a + b;
	}
}
