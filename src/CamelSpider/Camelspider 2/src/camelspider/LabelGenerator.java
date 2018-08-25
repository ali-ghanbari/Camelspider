package camelspider;

public class LabelGenerator {
	private static int counter = 0;
	
	public static String getNext () {
		return "<L" + (counter ++) + ">";
	}
}
