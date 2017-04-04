import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/*+----------------------------------------------------------------------
||
||  Class [Class Name] 
||
||         Author:  Alexander Yee
||
||        Purpose:  This class scrubs the data from the provided csv files
||					in the arguments. By scrubbing I mean:
||					1. removing the row with the column labels (first row) 
||					2. removing the rows that aren't complete (have blank values)
||					*I'm leaving the asterisks to represent them as null values when I insert them in SQL
||					After scrubbing, it writes the data to a new file: [filename]_Scrubbed.csv
||					Then it creates a .sql file that is ready to execute to create a table of the data: aims[year].sql
||
||  Inherits From:  None
||
||     Interfaces:  None
||
|+-----------------------------------------------------------------------
||
||      Constants:  None
||
|+-----------------------------------------------------------------------
||
||   Constructors:  Why
||
||  Class Methods:  null
||
||  Inst. Methods:  nada
||
++-----------------------------------------------------------------------*/
public class Scrub {
	private static BufferedReader br;
	private static BufferedWriter bwCSV;
	private static BufferedWriter bwSQL;
	private static String sqlTableName;

	public static void main(String[] args) {
		for (String filename : args) {
			try {
				br = new BufferedReader(new FileReader(new File(filename)));
				bwCSV = new BufferedWriter(
						new FileWriter(new File(filename.substring(0, filename.length() - 4) + "_Scrubbed.csv")));
				sqlTableName = "aims" + filename.substring(filename.length() - 8, filename.length() - 4);
				bwSQL = new BufferedWriter(new FileWriter(new File(sqlTableName + ".sql"))); // aims[year].sql
				/* INITIALIZE SQL FILE */
				bwSQL.write("set autocommit off;\nset define off;\nDROP TABLE " + sqlTableName + " PURGE;\n"
						+ "CREATE TABLE " + sqlTableName + " (\n" + "year	integer,\n" + "state	varchar2(7),\n"
						+ "county 	varchar2(69),\n" + "LEA_ID	integer,\n" + "LEA_CTDS	integer,\n"
						+ "LEA_name	varchar2(169),\n" + "school_id	integer,\n" + "school_CTDS	integer,\n"
						+ "school_name	varchar2(169),\n" + "is_charter	char(1),\n" + "math_mean 	integer,\n"
						+ "math_pctFFB	integer,\n" + "math_pctA	integer,\n" + "math_pctM	integer,\n"
						+ "math_pctE	integer,\n" + "math_pctP	integer,\n" + "read_mean	integer,\n"
						+ "read_pctFFB	integer,\n" + "read_pctA	integer,\n" + "read_pctM	integer,\n"
						+ "read_pctE	integer,\n" + "read_pctP	integer,\n" + "writ_mean	integer,\n"
						+ "writ_pctFFB	integer,\n" + "writ_pctA	integer,\n" + "writ_pctM	integer,\n"
						+ "writ_pctE	integer,\n" + "writ_pctP	integer,\n" + "sci_mean	integer,\n"
						+ "sci_pctFFB	integer,\n" + "sci_pctA	integer,\n" + "sci_pctM	integer,\n"
						+ "sci_pctE	integer,\n" + "sci_pctP	integer);\n");

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Object[] lines = br.lines().toArray();
			ArrayList<String> listOfLines = new ArrayList<String>();
			for (Object line : lines) {
				listOfLines.add((String) line);
			}
			listOfLines.remove(0); // get rid of the labels

			for (String line : listOfLines) {
				if (!line.contains(",,") && !line.contains(",\\s+,")) {
					// ignore any blank values

					/* CSV FILE WRITING */
					String[] splitLine = commaSplit(line, false);
					StringBuilder sb = new StringBuilder("");
					// convert the comma-split array back into a line with
					// commas (kinda lazy I know)
					for (String s : splitLine)
						sb.append(s + ",");
					sb.deleteCharAt(sb.length() - 1); // remove that last comma
					try {
						bwCSV.write(sb.toString() + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}

					/* SQL FILE WRITING */
					String[] splitLineSQL = commaSplit(line, true);
					StringBuilder sbSQL = new StringBuilder("");
					// convert the comma-split array back into a line with
					// commas (kinda lazy I know)
					for (String s : splitLineSQL)
						sbSQL.append(s + ",");
					sbSQL.deleteCharAt(sbSQL.length() - 1); // remove that last
															// comma
					try {
						bwSQL.write("INSERT INTO " + sqlTableName + " VALUES (" + sbSQL.toString() + ");" + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} 

			}
			try {
				bwCSV.close();
				bwSQL.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * commaSplit(line) -- returns a String[] containing all contents of the
	 * record split by a String also handles extra commas in quotes and wrapping
	 * strings with single quotes ALSO, the boolean values Y/N are converted to
	 * TRUE/FALSE, respectively.
	 */
	private static String[] commaSplit(String line, boolean sqlFlag) {
		String[] result = new String[34];
		int commaIndex = line.indexOf(',');
		if (sqlFlag && line.contains("'")) { // need to insert double single
												// quotes for SQL
			StringBuilder sb = new StringBuilder("");
			String[] splitLineByApostrophe = line.split("'");
			for (String s : splitLineByApostrophe)
				sb.append(s + "''");
			sb.delete(sb.length() - 2, sb.length()); // delete the last two
														// apostrophes
			line = sb.toString();
		}
		for (int i = 0; i < 34; i++) {
			commaIndex = line.indexOf(',');
			if (commaIndex == -1) {
				if (line.equals("*")) {
					result[i] = "NULL";
					break;
				}
				result[i] = line;
				break;
			}

			if (line.charAt(0) == '"') {
				int otherQuoteIndex = line.substring(1).indexOf('"');
				result[i] = "'" + line.substring(1, otherQuoteIndex + 1) + "'";
				line = line.substring(otherQuoteIndex + 3); // skip the comma
															// and add 1
				continue;
			}
			String field = line.substring(0, commaIndex);
			try {
				Integer.parseInt(field);
			} catch (NumberFormatException e) {
				if (!field.equals("*"))
					field = "'" + field + "'";
				else {
					field = "NULL";
				}
			}
			result[i] = field;
			line = line.substring(commaIndex + 1);

		}
		return result;
	}

}
