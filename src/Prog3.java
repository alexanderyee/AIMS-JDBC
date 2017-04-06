import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
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
||      Constants:  None
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
					} catch (NumberFormatException e) {}
					
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
		try {
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
    |			 result.
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
		String query = "SELECT count(school_name) FROM " + tablePrefix + year
				+ " WHERE school_name LIKE '% High %' AND "
				+ "school_name NOT LIKE '% Junior %' AND "
				+ "school_name NOT LIKE '% Jr %' AND "
				+ "school_name NOT LIKE '% Jr. %'";
		Statement stmt = null;
		ResultSet answer = null;
		// Send the query to the DBMS, and get and display the results
		try {

            stmt = dbconn.createStatement();
            answer = stmt.executeQuery(query);

            if (answer != null) {

                System.out.println("\nThe results of the query [" + query 
                                 + "] are:\n");

                    // Get the data about the query result to learn
                    // the attribute names and use them as column headers

                ResultSetMetaData answermetadata = answer.getMetaData();

                for (int i = 1; i <= answermetadata.getColumnCount(); i++) {
                    System.out.print(answermetadata.getColumnName(i) + "\t");
                }
                System.out.println();

                    // Use next() to advance cursor through the result
                    // tuples and print their attribute values

                while (answer.next()) {
                    System.out.println(answer.getString("sno") + "\t"
                        + answer.getInt("status"));
                }
            }
            System.out.println();

                // Shut down the connection to the DBMS.

            stmt.close();  

        } catch (SQLException e) {

                System.err.println("*** SQLException:  "
                    + "Could not fetch query results.");
                System.err.println("\tMessage:   " + e.getMessage());
                System.err.println("\tSQLState:  " + e.getSQLState());
                System.err.println("\tErrorCode: " + e.getErrorCode());
                System.exit(-1);

        }
		
	}

	private static void query2(Connection dbconn) {
		Statement stmt = null;
		ResultSet answer = null;
		
	}

	private static void query3(Connection dbconn) {
		Statement stmt = null;
		ResultSet answer = null;
		
	}

	private static void bestQuery(Connection dbconn) {
		Statement stmt = null;
		ResultSet answer = null;
		
	}

	public static void printMenu() {
		System.out.println("Please select a menu item:");
		System.out.println("a) How many High Schools are there?");
		System.out.println(
				"b) Display # of charter schools and how many had more Falls Far Below \n\tand Approaches than Passing for each year.");
		System.out.println(
				"c) For each county in 2014, which 10 schools had the greatest differences between\n\t the Passing percentages in Reading and Writing?");
		System.out.println("d) In construction...");
	}
}
