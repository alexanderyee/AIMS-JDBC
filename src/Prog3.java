import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
/*=============================================================================
|   Assignment:  Program #3:  JDBC
|       Author:  Alex Yee, netid: alexanderyee
|       Grader:  Yawen Chen and/or Jacob Combs
|
|       Course:  CSc 460
|   Instructor:  L. McCann
|     Due Date:  4/5/17 3:30pm
|
|  Description:  The goal of this program is to provide the user with choosable
|				 queries on the AIMS 2010-2014 data found here:
| 				 http://www.azed.gov/research-evaluation/aims-assessment-results/
|				 The data is assumed to already been entered into an Oracle database
|				 by executing the sql files provided by Scrub.java
|
|     Language:  Java
| Ex. Packages:  none
|                
| Deficiencies:  none yet...
*===========================================================================*/

/*+----------------------------------------------------------------------
||
||  Class Prog3
||
||         Author:  Alexander Yee
||
||        Purpose:  This class offers the user a menu of questions that 
||					are answered by using JDBC to interact with the database,
||					created by the SQL files that are created using Scrub.java,
||					to fetch the query results and display in a special format.
||					
||					
||					The snippet below is from Dr.McCann's sample JDBC program.
						* At the time of this writing, the version of Oracle is 11.2g, and
						* the Oracle JDBC driver can be found at
						*   /opt/oracle/product/10.2.0/client/jdbc/lib/ojdbc14.jar
						* on the lectura system in the UofA CS dept.
						* (Yes, 10.2, not 11.2.  It's the correct jar file but in a strange location.)
						*
						* To compile and execute this program on lectura:
						*
						*   Add the Oracle JDBC driver to your CLASSPATH environment variable:
						*
						*       export CLASSPATH=/opt/oracle/product/10.2.0/client/jdbc/lib/ojdbc14.jar:${CLASSPATH}
						*
						*     (or whatever shell variable set-up you need to perform to add the
						*     JAR file to your Java CLASSPATH)
						*
						*   Compile this file:
						*
						*         javac Prog3.java
						*
						*   Finally, run the program:
						*
						*         java Prog3 <oracle username> <oracle password>
						*
						* Author:  L. McCann (2008/11/19; updated 2015/10/28)
						* 
||		*CONTINUE CLASS COMMENTS*			
||		QUERY A (count of high schools given year):
||			As I understand now from Piazza, the conditions for what make a school a high school
||			My conditions are that as long as it contains the word ' High ' and do not contain the words
||			' Jr. ', ' Jr ', or ' Junior ', it is a high school.
||
||  Inherits From:  None
||
||     Interfaces:  None
||
|+-----------------------------------------------------------------------
||
||      Constants:  tablePrefix -- a string constant that represents the prefix of the table names
||					years -- a string containing the years the data was collected in
||					schoolDashes -- a string used for formatting the output of query3
||
|+-----------------------------------------------------------------------
||
||   Constructors:  none
||
||  Class Methods:  void printMenu() -- prints the menu for user input
||
||  Inst. Methods:  nada
||
++-----------------------------------------------------------------------*/
public class Prog3 {
	private final static String tablePrefix = "alexanderyee.aims";
	private final static String[] years = { "2010", "2011", "2012", "2013", "2014", };
	private final static String schoolDashes = String.format("%0" + 74 + "d", 0).replace("0","-");
	public static void main(String[] args) {
		final String oracleURL = // Magic lectura -> aloe access spell
				"jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
		String username = null, // Oracle DBMS username
				password = null; // Oracle DBMS password

		if (args.length == 2) { // get username/password from cmd line args
			username = args[0];
			password = args[1];
		} else {
			System.out.println("\nUsage:  java JDBC <username> <password>\n"
					+ "    where <username> is your Oracle DBMS" + " username,\n    and <password> is your Oracle"
					+ " password (not your system password).\n");
			System.exit(-1);
		}

		// load the (Oracle) JDBC driver by initializing its base
		// class, 'oracle.jdbc.OracleDriver'.

		try {

			Class.forName("oracle.jdbc.OracleDriver");

		} catch (ClassNotFoundException e) {

			System.err.println("*** ClassNotFoundException:  " + "Error loading Oracle JDBC driver.  \n"
					+ "\tPerhaps the driver is not on the Classpath?");
			System.exit(-1);

		}

		// make and return a database connection to the user's
		// Oracle database

		Connection dbconn = null;

		try {
			dbconn = DriverManager.getConnection(oracleURL, username, password);

		} catch (SQLException e) {

			System.err.println("*** SQLException:  " + "Could not open JDBC connection.");
			System.err.println("\tMessage:   " + e.getMessage());
			System.err.println("\tSQLState:  " + e.getSQLState());
			System.err.println("\tErrorCode: " + e.getErrorCode());
			System.exit(-1);

		}

		/* Begin prompting user for menu option */
		Scanner sc = new Scanner(System.in);
		System.out.println("~~~ Welcome to Alex's Delicious JDBC~~~");
		printMenu();
		while (sc.hasNext()) {
			String userInput = sc.nextLine().toLowerCase().trim();
			System.out.println();
			if (userInput.equals("a")) { // get the year from the user
				System.out.println("Enter a year (2010-2014): ");
				String year = "";
				while (sc.hasNext()) {
					year = sc.nextLine();
					year = year.trim();
					try {
						if (Integer.parseInt(year) <= 2014 && Integer.parseInt(year) >= 2010)
							break;
					} catch (NumberFormatException e) {
					}

					System.out.println("Invalid input/year, please try again: ");
					year = "";
				}
				if (!year.equals(""))
					query1(year, dbconn);
			} else if (userInput.equals("b")) {
				query2(dbconn);
			} else if (userInput.equals("c")) {
				query3(dbconn);
			} else if (userInput.equals("d")) {
				bestQuery(dbconn);
			} else {

				System.out.println("I didn't understand what you entered :(\nPlease try again...");
				continue;
			}

			System.out.println();
			printMenu();
		}
		sc.close();
		try { // Shut down the connection to the DBMS.
			dbconn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/*---------------------------------------------------------------------
	|  Method query1
	|
	|  Purpose:  Executes a query to find out how many high schools there are
	|			 in the year given by the string argument, then prints out the
	|			 result. The query is based on if the school name has the term
	|			 ' High ' in it but not ' Jr. ', ' Jr ', or ' Junior '. Also,
	|			 the aggregate function count is used and the result is obtained
	|			 from that.
	|
	|  Pre-condition:  The Connection dbconn must already connected to the Oracle DBMS
	|
	|  Post-condition: none
	|
	|  Parameters:
	|      String year -- the year entered by the user for which AIMS data to use
	|	   Connection dbconn -- the connection to the Oracle DBMS
	|
	|  Returns:  none
	*-------------------------------------------------------------------*/
	private static void query1(String year, Connection dbconn) {
		String query = "SELECT count(SCHOOL_NAME) AS total FROM " + tablePrefix + year + " WHERE "
				+ "SCHOOL_NAME LIKE '% High %' AND " + "SCHOOL_NAME NOT LIKE '% Junior %' AND "
				+ "SCHOOL_NAME NOT LIKE '% Jr %' AND " + "SCHOOL_NAME NOT LIKE '% Jr. %'";

		Statement stmt = null;
		ResultSet answer = null;
		// Send the query to the DBMS, and get and display the results
		try {

			stmt = dbconn.createStatement();
			answer = stmt.executeQuery(query);

			if (answer != null) {

				System.out.println("\nThe number of High Schools in " + year + " is:");

				// Get the data about the query result to learn
				// the attribute names and use them as column headers

				ResultSetMetaData answermetadata = answer.getMetaData();
				answer.next(); // advance pointer once
				System.out.println(answer.getInt("total")); // get the count and
															// prints it

			}

			stmt.close();
			System.out.println();
		} catch (SQLException e) {

			System.err.println("*** SQLException:  " + "Could not fetch query results.");
			System.err.println("\tMessage:   " + e.getMessage());
			System.err.println("\tSQLState:  " + e.getSQLState());
			System.err.println("\tErrorCode: " + e.getErrorCode());
			System.exit(-1);

		}

	}

	/*---------------------------------------------------------------------
	|  Method query2
	|
	|  Purpose:  Executes 5 queries to find out how many charter schools there are
	|			and how many of those charter schools had a sum of the math percentages
	|			Falls far below and appraoaches that was less than the percent passing
	|			for each of the five years (2010-2014)
	|
	|  Pre-condition:  The Connection dbconn must already connected to the Oracle DBMS
	|
	|  Post-condition: none
	|
	|  Parameters:
	|	   Connection dbconn -- the connection to the Oracle DBMS
	|
	|  Returns:  none
	*-------------------------------------------------------------------*/
	private static void query2(Connection dbconn) {
		for (String year : years) {
			String query = "SELECT count(school_name) AS total_charter FROM " + tablePrefix + year +
					" WHERE is_charter = 'Y'";
			Statement stmt = null;
			ResultSet answer = null;
			
			// Send the query to the DBMS, and get and display the results
			try {

				stmt = dbconn.createStatement();
				answer = stmt.executeQuery(query);

				if (answer != null) {

					System.out.print("The number of Charter Schools in " + year + " is: ");
					ResultSetMetaData answermetadata = answer.getMetaData();
					answer.next(); // advance pointer once
					System.out.println(answer.getInt("total_charter")); // get the count and
																		// prints it

				}

				stmt.close();
				
				// Send the second query to the DBMS (those that had more passing), and get and display the results
				query = "SELECT count(school_name) AS total_charter_good FROM " + tablePrefix + year +
						" WHERE is_charter = 'Y' AND math_pctFFB + math_pctA < math_pctP";
				stmt = dbconn.createStatement();
				answer = stmt.executeQuery(query);
				
				if (answer != null) {

					System.out.print("The number of those that had more percentage in Passing than the sum of Falls Far Below and Approaches: ");
					ResultSetMetaData answermetadata = answer.getMetaData();
					answer.next(); // advance pointer once
					System.out.println(answer.getInt("total_charter_good") + "\n"); // get the count and
																			// prints it
				}
			} catch (SQLException e) {

				System.err.println("*** SQLException:  " + "Could not fetch query results.");
				System.err.println("\tMessage:   " + e.getMessage());
				System.err.println("\tSQLState:  " + e.getSQLState());
				System.err.println("\tErrorCode: " + e.getErrorCode());
				System.exit(-1);

			}
		}

	}
	/*---------------------------------------------------------------------
	|  Method query3
	|
	|  Purpose:  For each county in 2014, prints out the top 10 schools that
	|			had the greatest differences in the Passing percentage in
	|			Reading and Writing. Each county is displayed in one table
	|			in a special format that includes ties. The tables are listed
	|			in ascending order by county name and schools are listed in
	|			descending order by their rank/abs. difference in the pctgs.
	|			Each table includes each school's rank, name, reading passing %,
	|			writing passing%, and absolute difference.
	|			
	|
	|  Pre-condition:  The Connection dbconn must already connected to the Oracle DBMS
	|
	|  Post-condition: none
	|
	|  Parameters:
	|	   Connection dbconn -- the connection to the Oracle DBMS
	|
	|  Returns:  none
	*-------------------------------------------------------------------*/
	private static void query3(Connection dbconn) {
		Statement stmt = null;
		ResultSet answer = null;
		// First, get all the county names
		String query = "SELECT DISTINCT county FROM " + tablePrefix + years[4] + " ORDER BY county"; 
		List<String> counties = new ArrayList<String>();
		try {

			stmt = dbconn.createStatement();
			answer = stmt.executeQuery(query);

			if (answer != null) {

				ResultSetMetaData answermetadata = answer.getMetaData();
				// Use next() to advance cursor through the result
                // tuples and print their attribute values
	
	            while (answer.next()) {
	                counties.add(answer.getString("county"));
	            }
	            
			}

			stmt.close();
			
			// Then get the absolute differences for each county's schools in desc. order
			for (String county: counties) {
				List<School> schools = new ArrayList<School>();
				
				query = "SELECT DISTINCT school_name, read_pctP, writ_pctP FROM " + tablePrefix + years[4] + 
						" WHERE county = '" + county + "' AND NOT read_pctP = 0 AND NOT writ_pctP = 0 " ;
				stmt = dbconn.createStatement();
				answer = stmt.executeQuery(query);
				
				if (answer != null) {

					ResultSetMetaData answermetadata = answer.getMetaData();
					// Use next() to advance cursor through the result
	                // tuples and print their attribute values
		
		            while (answer.next()) {
		                schools.add(new School(answer.getString("school_name"), answer.getInt("read_pctP"), answer.getInt("writ_pctP")));
		            }
		            printQuery3Results(county, schools);
				}
				stmt.close();
			}
			
		} catch (SQLException e) {

			System.err.println("*** SQLException:  " + "Could not fetch query results.");
			System.err.println("\tMessage:   " + e.getMessage());
			System.err.println("\tSQLState:  " + e.getSQLState());
			System.err.println("\tErrorCode: " + e.getErrorCode());
			System.exit(-1);

		}
		// Format the result to the top 10 with ties.
		
	}


	private static void bestQuery(Connection dbconn) {
		Statement stmt = null;
		ResultSet answer = null;
		
	}

	/*
	 * printMenu -- a method that prints out the list of available query options for the user
	 */
	public static void printMenu() {
		System.out.println("Please select a menu item:");
		System.out.println("a) How many High Schools are there?");
		System.out.println(
				"b) Display # of charter schools and how many had more Falls Far Below and Approaches\n\t than Passing percentages in Math for each year.");
		System.out.println(
				"c) For each county in 2014, which 10 schools had the greatest differences between\n\t the Passing percentages in Reading and Writing?");
		System.out.println("d) In construction...");
	}
	
	/*
	 * void printQuery3Results(String, List<School>)
	 * -- a method that prints out the top 10 schools with the greatest difference in
	 * reading passing pct and writing passing pct for the county given in the String argument.
	 * The list of schools is a representation of the schools and their differences in the county.
	 * Also accounts for ties.
	 */
	private static void printQuery3Results(String county, List<School> schools) {
		Collections.sort(schools);
		int pos = 1;
		int nTies = 1;
		int prevDiff = -1;
		System.out.println(county + " County\n" +
		"===============\n" +
		String.format("%101s", "Reading   Writing\n") +
		String.format("  Pos  School Name%98s", "Passing%  Passing%  |Difference|\n") +
		"  ---  " + schoolDashes +"  --------  --------  ------------\n");
		if (schools.size() > 0) {
			System.out.println("  " + String.format("%3s  %-74s     %2d        %2d          %2d", 
					pos, schools.get(0).getName(), schools.get(0).getReadPct(), schools.get(0).getWritingPct(), schools.get(0).getDiff())); // print out the first one, there shouldn't be a tie
			prevDiff = schools.get(0).getDiff();
		}
		for (int i = 1; i < schools.size(); i++) {
			
			if (prevDiff == schools.get(i).getDiff()) {
				nTies++;
			} else {
				pos += nTies;
				nTies = 1;
				prevDiff = schools.get(i).getDiff();
				if (pos > 10)
					break;
			}
			System.out.println("  " + String.format("%3s  %-74s     %2d        %2d          %2d", 
					pos, schools.get(i).getName(), schools.get(i).getReadPct(), schools.get(i).getWritingPct(), schools.get(i).getDiff())); // print out the others
				
		}
		System.out.println();
	}
	
}

/*+----------------------------------------------------------------------
	||
	||  Class School
	||
	||         Author:  Alexander Yee
	||
	||        Purpose:  This class represents a tuple for a school and its 
	||					absolute difference between its reading percentage
	||					passing and writing percentage passing. Also holds
	||					the percentages.
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
	||   Constructors:  School(String name, int readPct, int writingPct)
	||
	||  Class Methods:  
	||
	||  Inst. Methods:  int getDiff() -- returns that absolute difference
	||					String getName() -- returns the school name
	||					int getWritingPct() -- getter for writing percentage passing
	||					int getReadPct() -- getter for reading percentage passing
	++-----------------------------------------------------------------------*/
class School implements Comparable<School>{
	private int absDiffReadWritPass;
	private String name;
	private int readPct;
	private int writingPct;
	public School(String name, int readPct, int writingPct) {
		this.name = name;
		this.readPct = readPct;
		this.writingPct = writingPct;
		this.absDiffReadWritPass = Math.abs(readPct - writingPct);
	}
	public String getName() {
		return this.name;
	}
	public int getDiff() {
		return this.absDiffReadWritPass;
	}
	public int getWritingPct() {
		return this.writingPct;
	}
	public int getReadPct() {
		return this.readPct;
	}
	@Override
	public int compareTo(School o) {
		return ((School) o).getDiff() - this.absDiffReadWritPass; 
	}
}
