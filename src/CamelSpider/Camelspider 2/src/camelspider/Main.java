package camelspider;

import soot.*;

public class Main {

	public static void main(String[] args) {
		System.out.println("********************* <<CamelSpider 2>> **********************");
		System.out.println("Data-Race detection tool by Ali Ghanbari and Mehran S. Fallah.");
		System.out.println("This program is based on Soot framework.");
		System.out.println("");

		PackManager.v().getPack("wjtp")
				.add(new Transform("wjtp.cs2Trans", new CamelSpiderSceneTransformer(true)));
		soot.Main.main(args);
	}

}
