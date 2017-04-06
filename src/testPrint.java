
public class testPrint {

	private final static String schoolDashes = String.format("%0" + 74 + "d", 0).replace("0","-");
	public static void main(String[] args) {
		System.out.println("Something" + " County\n" +
				"===============\n" +
				String.format("%101s", "Reading   Writing\n") +
				String.format("  Pos  School Name%98s", "Passing%  Passing%  |Difference|\n") +
				"  ---  " + schoolDashes +"  --------  --------  ------------\n");
	}
}
