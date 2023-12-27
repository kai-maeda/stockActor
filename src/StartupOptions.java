import java.util.Scanner;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.swing.SwingUtilities;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;

public class StartupOptions {
    public static void main2(OracleConnection connection) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=======================================================================================================================");
        System.out.println("Welcome to the Startup Options:");
        System.out.println("1. Customer Interface");
        System.out.println("2. Manager Interface");
        System.out.println("3. Demo");
        System.out.println("0. Exit");
        System.out.println("=======================================================================================================================");
        System.out.println("Enter your choice (0-3): ");
        int choice = -1;
        while (true) {
            System.out.print("Enter a number between 1 and 3: ");
            
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline
                
                if (choice >= 0 && choice <= 3) {
                    // Valid input, break out of the loop
                    break;
                } else {
                    System.out.println("Error: Input must be between 0 and 3. Try again.");
                }
            } else {
                scanner.nextLine(); // Consume the invalid input
                System.out.println("Error: Invalid input. Please enter a valid number.");
            }
        }
        


        //scanner.nextLine(); // Consume the newline

        switch (choice) {
            case 1:
                System.out.println("You selected Customer Interface.");
                Login.main2(connection, 1);
                // Add your customer interface logic here
                break;
            case 2:
                System.out.println("You selected Manager Interface.");
                Login.main2(connection, 2);
                // Add your manager interface logic here
                break;
            case 3:
                System.out.println("You selected Demo.");
                Demo.initalizeInterface(connection, scanner);
                // Add your demo logic here
                break;
            case 0:
                System.out.println("Exiting...");
                System.out.println("=======================================================================================================================");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please select a valid option (1/2/3).");
        }

        scanner.close();
    }
}
