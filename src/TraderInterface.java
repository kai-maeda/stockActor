import java.util.Scanner;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import javax.swing.SwingUtilities;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Date;

public class TraderInterface {
    public static void main2(OracleConnection connection, String username) {

        Scanner scanner = new Scanner(System.in);

        // check if account exists
        while (doesAccountExist(connection, username) == false) {
            System.out.println(
                    "Account does not exist. Please create an account by typing Deposit and a balance greater than $1000");
            System.out.println("Ex: To Deposit $1000, type: Deposit 1000");

            String userInput = scanner.nextLine();

            // Split the user input into words
            String[] inputWords = userInput.split(" ");

            if (inputWords.length == 2 && inputWords[0].equalsIgnoreCase("Deposit")) {
                try {
                    double amount = Double.parseDouble(inputWords[1]);
                    if (amount >= 1000) {

                        // Deposit logic here
                        // create account
                        // create market account with amount

                        int acc_id = createAccount_Has(username, connection);
                        createMarketAccount(amount, acc_id, username, connection);
                        createDepositTransaction(connection, amount, username);

                        System.out.println("Successfully deposited $" + amount + ".");
                        // You can add code to perform the deposit operation
                        break; // Exit the loop after successful deposit
                    } else {
                        System.out.println("Deposit amount must be $1000 or more. Please try again.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a valid numeric amount.");
                }
            } else {
                System.out.println("Invalid input format. Please enter a valid deposit command.");
            }
        }

        // get account id
        int accountId = getAccountId(username, connection);

        while (true) {
            System.out.println(
                    "=======================================================================================================================");
            System.out.println("Trader Interface Options:");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Buy");
            System.out.println("4. Sell");
            System.out.println("5. Cancel Last Transaction");
            System.out.println("6. Show Balance");
            System.out.println("7. Show Transaction History");
            System.out.println("8. List Current Stock Price");
            System.out.println("9. Movie Information");
            System.out.println("10. List all Reviews for Movie");
            System.out.println("11. Top 10 Movies");
            System.out.println("12. List Stock Account Information");
            System.out.println("0. Exit");
            System.out.println(
                    "=======================================================================================================================");

            System.out.print("Enter your choice (0-12): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline
            System.out.println(
                    "=======================================================================================================================");

            switch (choice) {
                case 1:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    deposit(scanner, connection, accountId, username);
                    break;
                case 2:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    withdraw(scanner, connection, accountId, username);
                    break;
                case 3:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    buy(connection, scanner, accountId, username);
                    break;
                case 4:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    sell(connection, scanner, accountId, username);
                    break;
                case 5:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    cancel(connection, scanner, accountId, username);
                    break;
                case 6:
                    showBalance(connection, accountId);
                    break;
                case 7:
                    showTransactionHistory(connection, username);
                    break;
                case 8:
                    if (isOpen(connection) == false) {
                        break;
                    }
                    listStockPrice(connection, scanner);
                    break;
                case 9:
                    movieInformation(connection, scanner);
                    break;
                case 10:
                    listAllReviews(connection, scanner);
                    break;
                case 11:
                    topMovies(connection, scanner);
                    break;
                case 12:
                    listAllStockAccounts(connection, username);
                    break;
                case 0:
                    System.out.println("Exiting Trader Interface.");
                    // scanner.close();
                    StartupOptions.main2(connection);
                    // System.exit(0);
                default:
                    System.out.println("Invalid option. Please choose a valid option (0-12).");
            }
        }
    }

    public static int getAccountId(String username, Connection connection) {
        int accountId = -1;
        String selectQuery = "SELECT acc_id FROM Market_Account WHERE username = " + "'" + username + "'";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                accountId = resultSet.getInt("acc_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accountId;
    }

    private static int createAccount_Has(String username, OracleConnection connection) {
        int acc_id = 0;
        while (true) {
            Random random = new Random();
            acc_id = random.nextInt(Integer.MAX_VALUE);
            String selectQuery = "SELECT * FROM Account_Has WHERE acc_id = " + Integer.toString(acc_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    continue;
                } else {
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Generated Primary Key: " + acc_id);
        String insertQuery = "INSERT INTO Account_Has (acc_id, username) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, acc_id);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Account created successfully!");
            } else {
                System.out.println("Account creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Account creation failed.");
            e.printStackTrace();
        }

        return (acc_id);
    }

    private static void createMarketAccount(double amount, int acc_id, String username, OracleConnection connection) {
        // create account
        // Random random = new Random();
        // int acc_id = random.nextInt(Integer.MAX_VALUE);
        // System.out.println("Generated Primary Key: " + acc_id);
        String insertQuery = "INSERT INTO Market_Account (Balance, acc_id, username) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, acc_id);
            preparedStatement.setString(3, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Market Account created successfully!");
            } else {
                System.out.println("Market Account creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Market Account creation failed.");
            e.printStackTrace();
        }
    }

    private static void deposit(Scanner scanner, OracleConnection connection, int accountId, String username) {
        System.out.println("Deposit option selected.");
        while (true) {
            System.out.println("Please enter the amount you would like to deposit as a decimal number: ");
            if (scanner.hasNextDouble()) {
                double amount = scanner.nextDouble();
                if (amount <= 0) {
                    System.out.println("Invalid input. Please enter a positive non-zero amount.");
                } else {
                    // Process the valid double input (amount) here
                    System.out.println("You entered: " + amount);
                    depositSQL(amount, connection, accountId, username);
                    createDepositTransaction(connection, amount, username);
                    break; // Exit the loop since valid input was provided
                }
            } else {
                System.out.println("Invalid input. Please enter a valid decimal number.");
                scanner.next(); // Consume the invalid input to avoid an infinite loop
            }
        }
        // Continue with your deposit logic here using the valid amount.
    }

    private static void depositSQL(double amount, OracleConnection connection, int accountId, String username) {

        String insertQuery = "UPDATE Market_Account SET Balance = Balance + ? WHERE acc_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Deposit successful!");
            } else {
                System.out.println("Deposit failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Deposit failed.");
            e.printStackTrace();
        }
        addToTempMoney(connection, username);
    }

    private static void withdraw(Scanner scanner, OracleConnection connection, int accountId, String username) {
        System.out.println("Withdraw option selected.");
        while (true) {
            System.out.println("Please enter the amount you would like to withdraw as a decimal number: ");
            if (scanner.hasNextDouble()) {
                double amount = scanner.nextDouble();
                // Process the valid double input (amount) here
                if (amount <= 0) {
                    System.out.println("Invalid input. Withdrawal amount must be greater than zero.");
                } else {
                    double currentBalance = getBalance(connection, accountId);
                    if (amount > currentBalance) {
                        System.out.println("Insufficient balance. Your current balance is: " + currentBalance);
                    } else {
                        withdrawSQL(amount, connection, accountId, username);
                        System.out.println("Withdrawal of $" + amount + " successful!");
                        createWithdrawTransaction(connection, amount, username);
                        break; // Exit the loop after successful withdrawal
                    }
                }
            } else {
                System.out.println("Invalid input. Please enter a valid decimal number.");
                scanner.next(); // Consume the invalid input to avoid an infinite loop
            }
        }
        // Continue with your withdrawal logic here using the valid amount.
    }

    private static void withdrawSQL(double amount, OracleConnection connection, int accountId, String username) {
        String updateQuery = "UPDATE Market_Account SET Balance = Balance - ? WHERE acc_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Withdrawal successful!");
            } else {
                System.out.println("Withdrawal failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Withdrawal failed.");
            e.printStackTrace();
        }
        addToTempMoney(connection, username);
    }

    private static void createWithdrawTransaction(OracleConnection connection, double amount, String username) {
        int transaction_id = createTransaction(connection, username, 0);
        String insertQuery = "INSERT INTO Withdraw (tid, amount) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, transaction_id);
            preparedStatement.setDouble(2, amount);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Withdraw Transaction created successfully!");
            } else {
                System.out.println("Withdraw Transaction creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Withdraw Transaction creation failed.");
            e.printStackTrace();
        }

    }

    public static double getStockPrice(OracleConnection connection, String symbol) {
        String selectQuery = "SELECT * FROM Stock_Actor WHERE symbol = " + "'" + symbol + "'";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                return resultSet.getDouble("current_price");
            }
        } catch (SQLException e) {
            return (-1.0);
        }
        return -1.0;
    }

    private static void buy(OracleConnection connection, Scanner scanner, int acc_id, String username) {
        // Implement buy logic

        System.out.println("Buy option selected.");
        System.out.println("Type symbol of stock you would like to purchase.");
        String symbol = scanner.nextLine();
        double stockPrice = getStockPrice(connection, symbol);
        if (stockPrice != -1.0) {
            System.out.println("Stock exists.");
            // System.out.println("Type amount of stock you would like to purchase.");
            int numShares = 0;
            while (true) {
                System.out.print("Type the amount of stock you wish to purchase: ");
                try {
                    numShares = Integer.parseInt(scanner.nextLine());
                    break; // Exit the loop if the input is a valid integer
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid integer. Try again.");
                }
            }
            if (numShares <= 0) {
                System.out.println("Invalid input. Number of shares must be greater than zero.");
                return;
            }
            double totalCost = stockPrice * numShares + 20;
            double balance = getBalance(connection, acc_id);
            if (totalCost >= balance) {
                System.out.println("Insufficient funds. Your current balance is: " + balance);
                return;
            } else {
                // withdraw the money
                withdrawSQL(totalCost, connection, acc_id, username);
                String selectQuery = "SELECT balance_share FROM Stock_Account WHERE username = " + "'" + username + "'"
                        + " AND symbol = " + "'" + symbol + "'";
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(selectQuery);
                    if (!resultSet.next()) {
                        createStockAccount(connection, scanner, username, symbol);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // String updateQuery = "UPDATE Stock_Account SET num_share = num_share + ?,
                // balance_share = balance_share + ? WHERE tax_id = ? AND symbol = ?";
                String updateQuery = "UPDATE Stock_Account SET num_share = num_share + " + Integer.toString(numShares)
                        + ", balance_share = balance_share + " + Double.toString(totalCost - 20) + " WHERE username = "
                        + "'" + username + "'" + " AND symbol = " + "'" + symbol + "'";
                try (Statement statement = connection.createStatement()) {
                    // preparedStatement.setInt(1, numShares);
                    // preparedStatement.setDouble(2, totalCost);
                    // preparedStatement.setInt(3, tax_id);
                    // preparedStatement.setString(4, symbol);

                    int rowsAffected = statement.executeUpdate(updateQuery);
                    if (rowsAffected > 0) {
                        System.out.println("Added to Stock Account successful!");
                    } else {
                        System.out.println("Purchase failed.");
                    }
                } catch (SQLException e) {
                    System.out.println("ERROR: Purchase failed.");
                    e.printStackTrace();
                }
                createBuyTransaction(connection, numShares, symbol, stockPrice, username);
                createBought_Stock(connection, numShares, symbol, stockPrice, username);

            }
        } else {
            System.out.println("Stock does not exist.");
        }
    }

    private static void createBought_Stock(OracleConnection connection, int numShares, String symbol, double price,
            String username) {
        // String addStockQuery = "UPDATE Bought_Stock SET shares_bought = shares_bought
        // + ? WHERE tax_id = ? AND symbol = ? AND buy_price = ?";
        String selectQuery = "SELECT * FROM Bought_Stock WHERE username = " + "'" + username + "'" + " AND symbol = "
                + "'" + symbol + "' AND buy_price = " + Double.toString(price);
        boolean found = false;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                found = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (found == true) {
            String addStockQuery = "UPDATE Bought_Stock SET shares_bought = shares_bought + "
                    + Integer.toString(numShares) + " WHERE username = " + "'" + username + "'" + " AND symbol = " + "'"
                    + symbol + "' AND buy_price = " + Double.toString(price);
            try (Statement statement2 = connection.createStatement()) {
                int rowsAffected = statement2.executeUpdate(addStockQuery);
                System.out.println("GOT HERE");
                if (rowsAffected > 0) {
                    System.out.println("Updated Bought_Stock successful!");
                    return;
                } else {
                    System.out.println("No create Bought_Stock. Continue");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        int bought_id = 0;
        while (true) {
            Random random = new Random();
            bought_id = random.nextInt(Integer.MAX_VALUE);
            String updateQuery = "SELECT * FROM Bought_Stock WHERE bought_id = " + Integer.toString(bought_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(updateQuery);
                if (resultSet.next()) {
                    continue;
                } else {
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String selectFromStockAccount = "SELECT * FROM Stock_Account WHERE username = " + "'" + username + "'"
                + " AND symbol = " + "'" + symbol + "'";
        int acc_id = 0;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectFromStockAccount);
            if (resultSet.next()) {
                acc_id = resultSet.getInt("acc_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String insertQuery = "INSERT INTO Bought_Stock (buy_price, shares_bought, bought_id, acc_id, symbol, username) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setDouble(1, price);
            preparedStatement.setInt(2, numShares);
            preparedStatement.setInt(3, bought_id);
            preparedStatement.setInt(4, acc_id);
            preparedStatement.setString(5, symbol);
            preparedStatement.setString(6, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stock_Bought created successfully!");
            } else {
                System.out.println("Stock_Bought creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Stock_Bought creation failed.");
            e.printStackTrace();
        }

    }

    private static void createStockAccount(OracleConnection connection, Scanner scanner, String username,
            String symbol) {
        int acc_id = 0;
        while (true) {
            Random random = new Random();
            acc_id = random.nextInt(Integer.MAX_VALUE);
            String selectQuery1 = "SELECT * FROM Stock_Account WHERE acc_id = " + Integer.toString(acc_id);
            String selectQuery2 = "SELECT * FROM Market_Account WHERE acc_id = " + Integer.toString(acc_id);

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery1);
                if (resultSet.next()) {
                    continue;
                } else {
                    try (Statement statement2 = connection.createStatement()) {
                        ResultSet resultSet2 = statement2.executeQuery(selectQuery2);
                        if (resultSet2.next()) {
                            continue;
                        } else {
                            break;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String insertIntoAccount_Has = "INSERT INTO Account_Has (acc_id, username) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertIntoAccount_Has)) {
            preparedStatement.setInt(1, acc_id);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Account_Has created successfully!");
            } else {
                System.out.println("Account_Has creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Account_Has creation failed.");
            e.printStackTrace();
        }

        String insertQuery = "INSERT INTO Stock_Account (acc_id, symbol, num_share, balance_share, username) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setDouble(1, acc_id);
            preparedStatement.setString(2, symbol);
            preparedStatement.setInt(3, 0);
            preparedStatement.setDouble(4, 0.0);
            preparedStatement.setString(5, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stock Account created successfully!");
            } else {
                System.out.println("Stock Account creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Stock Account creation failed.");
            e.printStackTrace();
        }

    }

    private static void printAllShares(String symbol, OracleConnection connection, String username) {
        System.out.println("All Owned Shares of " + symbol + ":");
        String selectQuery = "SELECT * FROM Bought_Stock WHERE symbol = " + "'" + symbol + "'" + " AND username = "
                + "'" + username + "'";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()) {
                System.out.print("Shares Owned: " + resultSet.getString("shares_bought"));
                System.out.println(" Buy Price: " + resultSet.getString("buy_price"));
                System.out.println(
                        "=======================================================================================================================");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sell(OracleConnection connection, Scanner scanner, int acc_id, String username) {
        // Implement sell logic
        System.out.println("Sell option selected.");
        if (have20(connection, username, acc_id) == false) {
            return;
        }
        System.out.println("Enter the symbol of the stock you would like to sell: ");
        String symbol = scanner.nextLine();
        double stockPrice = getStockPrice(connection, symbol);
        printAllShares(symbol, connection, username);
        if (stockPrice == -1.0) {
            System.out.println("Stock does not exist.");
            return;
        }
        ArrayList<Integer> sellNumSharesArray = new ArrayList<>();
        ArrayList<Double> sellAtBuyPriceArray = new ArrayList<>();
        int totalShares = 0;
        int numShares = 0;
        boolean execute = false;
        while (execute == false) {
            while (true) {
                System.out.print("Type the amount of stock you wish to sell: ");
                try {
                    numShares = Integer.parseInt(scanner.nextLine());
                    if (numShares <= 0) {
                        System.out.println("Invalid input. Number of shares must be an integer greater than zero.");
                        return;
                    }
                    break; // Exit the loop if the input is a valid integer
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid integer. Try again.");
                }
            }
            double sell_at_buy_price = 0;
            while (true) {
                System.out.print("Type the buy price you wish to sell of: ");
                try {
                    sell_at_buy_price = Double.parseDouble(scanner.nextLine());
                    if (sell_at_buy_price <= 0) {
                        System.out.println("Invalid input. Buy price must be greater than zero.");
                        return;
                    }
                    break; // Exit the loop if the input is a valid integer
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid decimal. Try again.");
                }
            }
            sellNumSharesArray.add(numShares);
            totalShares = totalShares + numShares;
            sellAtBuyPriceArray.add(sell_at_buy_price);
            while (true) {
                System.out.println("Do you want to sell additional shares? (Y/N)");
                String answer = scanner.nextLine();

                if (answer.equalsIgnoreCase("Y")) {
                    execute = false;
                    break; // Exit the loop when the user enters "Y"
                } else if (answer.equalsIgnoreCase("N")) {
                    execute = true;
                    break; // Exit the loop when the user enters "N"
                } else {
                    System.out.println("Invalid input. Please enter 'Y' or 'N'.");
                }
            }
        }

        // check if user has enough shares to sell, store total to ensure that user has
        // enough in the balance

        double total = 0;

        for (int i = 0; i < sellNumSharesArray.size(); i++) {
            int numSharesToSell = sellNumSharesArray.get(i);
            double sell_at_buy_price = sellAtBuyPriceArray.get(i);
            String selectQuery = "SELECT * FROM Bought_Stock WHERE username = " + "'" + username + "'"
                    + " AND symbol = " + "'" + symbol + "' AND buy_price = " + Double.toString(sell_at_buy_price);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    int shares_bought = resultSet.getInt("shares_bought");
                    if (shares_bought < numSharesToSell) {
                        System.out
                                .println("You do not have enough shares to sell: " + numSharesToSell + " at buy price: "
                                        + sell_at_buy_price + " You only have: " + shares_bought + " shares.");

                        System.out.println("Exiting Sell.");
                        return;
                    } else {
                        total += numSharesToSell * sell_at_buy_price;
                    }
                } else {
                    System.out.println("You do not own any shares at this buy price: " + sell_at_buy_price);
                    System.out.println("Exiting Sell.");
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        

        // alter Bought_Stock table

        for (int i = 0; i < sellNumSharesArray.size(); i++) {
            int numSharesToSell = sellNumSharesArray.get(i);
            double sell_at_buy_price = sellAtBuyPriceArray.get(i);
            String selectQuery = "SELECT * FROM Bought_Stock WHERE username = " + "'" + username + "'"
                    + " AND symbol = " + "'" + symbol + "' AND buy_price = " + Double.toString(sell_at_buy_price);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    int shares_bought = resultSet.getInt("shares_bought");
                    
    
                    String updateQuery = "UPDATE Bought_Stock SET shares_bought = shares_bought - "
                            + Integer.toString(numSharesToSell) + " WHERE username = " + "'" + username + "'"
                            + " AND symbol = " + "'" + symbol + "' AND buy_price = "
                            + Double.toString(sell_at_buy_price);
                    try (Statement statement2 = connection.createStatement()) {
                        int rowsAffected = statement2.executeUpdate(updateQuery);
                        if (rowsAffected > 0) {
                            System.out.println("Updated Bought_Stock successful!");
                        } else {
                            System.out.println("Update Bought_Stock failed.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    removeEmptyBoughtStock(connection);
            
                } else {
                    System.out.println("You do not own any shares at this buy price: " + sell_at_buy_price);
                    System.out.println("Exiting Sell.");
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        String addToSell = "INSERT INTO Sell (tid, symbol, sell_price, shares) VALUES (?, ?, ?, ?)";

        int transaction_id = createTransaction(connection, username, 0);

        try (PreparedStatement preparedStatement = connection.prepareStatement(addToSell)) {
            preparedStatement.setInt(1, transaction_id);
            preparedStatement.setString(2, symbol);
            preparedStatement.setDouble(3, stockPrice);
            preparedStatement.setInt(4, totalShares);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Sell Transaction created successfully!");
            } else {
                System.out.println("Sell Transaction creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Sell Transaction creation failed.");
            e.printStackTrace();
        }

        depositSQL(total, connection, acc_id, username);
        withdrawSQL(20, connection, acc_id, username);

        int sell_leg_group_id = generateRandomSellLegGroupBy(connection);

        for (int i = 0; i < sellNumSharesArray.size(); i++) {
            int numSharesToSell = sellNumSharesArray.get(i);
            double sell_at_buy_price = sellAtBuyPriceArray.get(i);
            int leg_id = 0;
            while (true) {
                Random random = new Random();
                leg_id = random.nextInt(Integer.MAX_VALUE);
                String selectQuery = "SELECT * FROM Sell_leg WHERE leg_id = " + Integer.toString(leg_id);
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(selectQuery);
                    if (resultSet.next()) {
                        continue;
                    } else {
                        break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            String insertQuery = "INSERT INTO Sell_leg (bought_price, shares, tid, leg_id, group_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setDouble(1, sell_at_buy_price);
                preparedStatement.setInt(2, numSharesToSell);
                preparedStatement.setInt(3, transaction_id);
                preparedStatement.setInt(4, leg_id);
                preparedStatement.setInt(5, sell_leg_group_id);

                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Sell_leg created successfully!");
                } else {
                    System.out.println("Sell_leg creation failed.");
                }
            } catch (SQLException e) {
                System.out.println("ERROR: Sell_leg creation failed.");
                e.printStackTrace();
            }
            // String updateQuery = "UPDATE Stock_Account SET num_share = num_share - " +
            // Integer.toString(numShares) + ", balance_share = balance_share - " +
            // Double.toString(total) + " WHERE username = " + "'" + username + "'" + " AND
            // symbol = " + "'" + symbol + "'";
            String updateQuery = "UPDATE Stock_Account SET num_share = num_share - " + Integer.toString(numSharesToSell)
                    + ", balance_share = balance_share - " + Double.toString(numSharesToSell * sell_at_buy_price)
                    + " WHERE username = " + "'" + username + "'" + " AND symbol = " + "'" + symbol + "'";
            try (Statement statement2 = connection.createStatement()) {
                int rowsAffected = statement2.executeUpdate(updateQuery);
                if (rowsAffected > 0) {
                    System.out.println("Update Stock Account successful!");
                } else {
                    System.out.println("Update Stock Account failed.");
                }
            } catch (SQLException e) {
                System.out.println("ERROR: Update Stock Account failed.");
                e.printStackTrace();
            }

        }

    }

    

    public static int generateRandomSellLegGroupBy(OracleConnection connection) {
        int group_id = 0;
        while (true) {
            Random random = new Random();
            group_id = random.nextInt(Integer.MAX_VALUE);
            String selectQuery = "SELECT * FROM Sell_leg WHERE group_id = " + Integer.toString(group_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    continue;
                } else {
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return group_id;
    }

    private static void cancel(OracleConnection connection, Scanner scanner, int acc_id, String username) {
        // Implement cancel logic
        System.out.println("Cancel option selected.");
        if (have20(connection, username, acc_id) == false) {
            return;
        }
        int last_tid = -1;
        String selectQuery = "SELECT * FROM Customer WHERE username = " + "'" + username + "'";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                last_tid = resultSet.getInt("last_tid");
            } else {
                System.out.println("Sorry, You have no transaction to cancel.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String selectFromTransactions = "SELECT * FROM Transactions WHERE tid = " + Integer.toString(last_tid);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectFromTransactions);
            if (resultSet.next()) {
                Date date = resultSet.getDate("date_executed");
                if (!date.equals(Demo.getDateSQLFriendly(connection))) {
                    System.out.println(date);
                    System.out.println(Demo.getDateSQLFriendly(connection));
                    System.out.println("You can only cancel transactions that happened in the same day.");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String selectFromBuys = "SELECT * FROM Buy WHERE tid = " + Integer.toString(last_tid);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectFromBuys);
            if (resultSet.next()) {
                int shares = resultSet.getInt("shares");
                String symbol = resultSet.getString("symbol");
                double buy_price = resultSet.getDouble("buy_price");
                System.out.println("Your last transaction was a buy of " + shares + " shares of " + symbol
                        + " at a price of " + buy_price);
                System.out.println(
                        "Would you like to cancel this transaction? Type Y to cancel, type anything else to exit.");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("Y")) {
                    String updateStockAccount = "UPDATE Stock_Account SET num_share = num_share - "
                            + Integer.toString(shares) + ", balance_share = balance_share - "
                            + Double.toString(shares * buy_price) + " WHERE username = " + "'" + username + "'"
                            + " AND symbol = " + "'" + symbol + "'";
                    try (Statement statement2 = connection.createStatement()) {
                        int rowsAffected = statement2.executeUpdate(updateStockAccount);
                        if (rowsAffected > 0) {
                            System.out.println("Update Stock Account successful!");
                        } else {
                            System.out.println("Update Stock Account failed.");
                        }
                    } catch (SQLException e) {
                        System.out.println("ERROR: Update Stock Account failed.");
                        e.printStackTrace();
                    }

                    String updateBought_Stock = "UPDATE Bought_Stock SET shares_bought = shares_bought - "
                            + Integer.toString(shares) + " WHERE username = " + "'" + username + "'" + " AND symbol = "
                            + "'" + symbol + "' AND buy_price = " + Double.toString(buy_price);
                    try (Statement statement2 = connection.createStatement()) {
                        int rowsAffected = statement2.executeUpdate(updateBought_Stock);
                        if (rowsAffected > 0) {
                            System.out.println("Update Bought_Stock successful!");
                        } else {
                            System.out.println("Update Bought_Stock failed.");
                        }
                    } catch (SQLException e) {
                        System.out.println("ERROR: Update Bought_Stock failed.");
                        e.printStackTrace();
                    }
                    removeEmptyBoughtStock(connection);
                } else {
                    return;
                }
                String insertIntoCancel = "INSERT INTO Cancels (tid, ptid) VALUES (?, ?)";
                int transaction_id = createTransaction(connection, username, 0);
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertIntoCancel)) {
                    preparedStatement.setInt(1, transaction_id);
                    preparedStatement.setInt(2, last_tid);

                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Cancel Transaction created successfully!");
                    } else {
                        System.out.println("Cancel Transaction creation failed.");
                    }
                } catch (SQLException e) {
                    System.out.println("ERROR: Cancel Transaction creation failed.");
                    e.printStackTrace();
                }
                depositSQL(shares * buy_price, connection, acc_id, username);
                withdrawSQL(20, connection, acc_id, username);
                return;
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Not a Buy Transaction");
        }

        String selectFromSells = "SELECT * FROM Sell WHERE tid = " + Integer.toString(last_tid);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectFromSells);
            if (resultSet.next()) {
                int shares = resultSet.getInt("shares");
                String symbol = resultSet.getString("symbol");
                double sell_price = resultSet.getDouble("sell_price");
                System.out.println("Your last transaction was a Sell Transaction");
                System.out.println(
                        "Would you like to cancel this transaction? Type Y to cancel, type anything else to exit.");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("Y")) {
                    

                    
                    

                } else {
                    return;
                }

                String get_group_id_query = "SELECT * FROM Sell_leg WHERE tid = " + Integer.toString(last_tid);
                int group_id = 0;
                try (Statement statement2 = connection.createStatement()) {
                    ResultSet resultSet2 = statement2.executeQuery(get_group_id_query);
                    if (resultSet2.next()) {
                        group_id = resultSet2.getInt("group_id");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                String selectFromSellLeg = "SELECT * FROM Sell_leg WHERE group_id = " + Integer.toString(group_id);
                try (Statement statement2 = connection.createStatement()) {
                    ResultSet resultSet2 = statement2.executeQuery(selectFromSellLeg);
                    while (resultSet2.next()) {
                        int shares_bought = resultSet2.getInt("shares");
                        double bought_price = resultSet2.getDouble("bought_price");
                        createBought_Stock(connection, shares_bought, symbol, bought_price, username);
                        // String updateStockAccount = "UPDATE Stock_Account SET num_share = num_share +
                        // " + Integer.toString(shares) + ", balance_share = balance_share + " +
                        // Double.toString(shares * sell_price) + " WHERE username = " + "'" + username
                        // + "'" + " AND symbol = " + "'" + symbol + "'";
                        String updateStockAccount = "UPDATE Stock_Account SET num_share = num_share + "
                                + Integer.toString(shares_bought) + ", balance_share = balance_share + "
                                + Double.toString(shares_bought * bought_price) + " WHERE username = " + "'" + username
                                + "'" + " AND symbol = " + "'" + symbol + "'";
                        try (Statement statement3 = connection.createStatement()) {
                            int rowsAffected = statement3.executeUpdate(updateStockAccount);
                            if (rowsAffected > 0) {
                                System.out.println("Update Stock Account successful!");
                            } else {
                                System.out.println("Update Stock Account failed.");
                            }
                        } catch (SQLException e) {
                            System.out.println("ERROR: Update Stock Account failed.");
                            e.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                String deleteFromSellLeg = "DELETE FROM Sell_leg WHERE group_id = " + Integer.toString(group_id);
                try (Statement statement2 = connection.createStatement()) {
                    int rowsAffected = statement2.executeUpdate(deleteFromSellLeg);
                    if (rowsAffected > 0) {
                        System.out.println("Deleted Sell_leg successful!");
                    } else {
                        System.out.println("Delete Sell_leg failed.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                String insertIntoCancel = "INSERT INTO Cancels (tid, ptid) VALUES (?, ?)";
                int transaction_id = createTransaction(connection, username, 0);
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertIntoCancel)) {
                    preparedStatement.setInt(1, transaction_id);
                    preparedStatement.setInt(2, last_tid);

                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Cancel Transaction created successfully!");
                    } else {
                        System.out.println("Cancel Transaction creation failed.");
                    }
                } catch (SQLException e) {
                    System.out.println("Error: Cancel Transaction failed.");
                }
                withdrawSQL(shares * sell_price, connection, acc_id, username);
                withdrawSQL(20, connection, acc_id, username);
                return;
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Not a Sell Transaction");
        }

        System.out.println(
                "You cannot cancel your previous transaction. You cannot cancel an accure interest or cancel transaction or cancel a transaction at the beginning of the month.");

    }

    private static void showBalance(OracleConnection connection, int acc_id) {
        // Implement show balance logic
        System.out.println("Show Balance option selected.");
        double balance = getBalance(connection, acc_id);
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedNumber = df.format(balance);
        System.out.println("Your current balance is: " + formattedNumber);
    }

    private static double getBalance(OracleConnection connection, int acc_id) {
        double balance = 0.0;
        String selectQuery = "SELECT balance FROM Market_Account WHERE acc_id = " + Integer.toString(acc_id);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                balance = resultSet.getFloat("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    private static void createBuyTransaction(OracleConnection connection, int numShares, String symbol, double price,
            String username) {
        String insertQuery = "INSERT INTO Buy (tid, shares, buy_price, symbol) VALUES (?, ?, ?, ?)";

        int transaction_id = createTransaction(connection, username, 0);

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setDouble(1, transaction_id);
            preparedStatement.setInt(2, numShares);
            preparedStatement.setDouble(3, price);
            preparedStatement.setString(4, symbol);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Buy Transaction created successfully!");
            } else {
                System.out.println("Buy Transaction creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Buy Transaction creation failed.");
            e.printStackTrace();
        }
    }

    private static void createDepositTransaction(OracleConnection connection, double amount, String username) {
        int transaction_id = createTransaction(connection, username, 0);
        String insertQuery = "INSERT INTO Deposit (tid, amount) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, transaction_id);
            preparedStatement.setDouble(2, amount);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Deposit Transaction created successfully!");
            } else {
                System.out.println("Deposit Transaction creation failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Deposit Transaction creation failed.");
            e.printStackTrace();
        }
    }

    public static int createTransaction(OracleConnection connection, String username, int flag) {
        int transaction_id = 0;
        while (true) {
            Random random = new Random();
            transaction_id = random.nextInt(Integer.MAX_VALUE);
            String selectQuery = "SELECT * FROM Transactions WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    continue;
                } else {
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (flag == 0)
            System.out.println("Generated Transaction Key: " + transaction_id);
        String insertQuery = "INSERT INTO Transactions (tid, date_executed, time_order) VALUES (?, ?, ?)";

        Date date = Demo.getDateSQLFriendly(connection);
        // Date date = new Date(System.currentTimeMillis());
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, transaction_id);
            preparedStatement.setDate(2, date);
            Date myDate = new Date(System.currentTimeMillis());
            preparedStatement.setDate(3, myDate);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                if (flag == 0)
                    System.out.println("Transaction created successfully!");
            } else {
                if (flag == 0)
                    System.out.println("Transaction creation failed.");
            }
        } catch (SQLException e) {
            if (flag == 0)
                System.out.println("Transaction Account creation failed.");
            e.printStackTrace();
        }
        insertIntoCommits(connection, transaction_id, username, flag);

        String modifyCustomerTid = "UPDATE Customer SET last_tid = ? WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(modifyCustomerTid)) {
            preparedStatement.setInt(1, transaction_id);
            preparedStatement.setString(2, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                if(flag == 0)System.out.println("Modify Customer successful!");
            } else {
                if(flag == 0)System.out.println("Modify Customer failed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Modify Customer failed.");
            e.printStackTrace();
        }

        return (transaction_id);
    }

    public static void insertIntoCommits(OracleConnection connection, int transaction_id, String username, int flag) {
        String insertQuery = "INSERT INTO Commits (acc_id, tid, username) VALUES (?, ?, ?)";
        int acc_id = getAccountId(username, connection);

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, acc_id);
            preparedStatement.setInt(2, transaction_id);
            preparedStatement.setString(3, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                if (flag == 0)
                    System.out.println("Commit created successfully!");
            } else {
                if (flag == 0)
                    System.out.println("Commit creation failed.");
            }
        } catch (SQLException e) {
            if (flag == 0)
                System.out.println("Commit creation failed.");
            e.printStackTrace();
        }
    }

    private static void showTransactionHistory(OracleConnection connection, String username) {
        // Implement show transaction history logic
        System.out.println("Show Transaction History option selected.");
        ArrayList<Integer> transactions = new ArrayList<Integer>();
        transactions = getUserTransactions(connection, username);
        for (int i = 0; i < transactions.size(); i++) {
            int transaction_id = transactions.get(i);
            String selectQuery = "SELECT * FROM Transactions WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction ID: " + transaction_id);
                    System.out.println("Date Executed: " + resultSet.getString("date_executed").substring(0, 10));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Deposit WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Deposit");
                    System.out.println("Deposit Amount: " + resultSet.getString("amount"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Withdraw WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Withdraw");
                    System.out.println("Withdraw Amount: " + resultSet.getString("amount"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Buy WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Buy");
                    System.out.println("Number of Shares: " + resultSet.getString("shares"));
                    System.out.println("Buy Price: " + resultSet.getString("buy_price"));
                    System.out.println("Symbol: " + resultSet.getString("symbol"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Sell WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Sell");
                    System.out.println("Number of Shares: " + resultSet.getString("shares"));
                    System.out.println("Sell Price: " + resultSet.getString("sell_price"));
                    System.out.println("Symbol: " + resultSet.getString("symbol"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Cancels WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Cancel");
                    System.out.println("Previous Transaction Cancelled");
                    System.out.println("Previous Transaction ID: " + resultSet.getString("ptid"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            selectQuery = "SELECT * FROM Interests WHERE tid = " + Integer.toString(transaction_id);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Transaction Type: Accrue Interest");
                    System.out.println("Interest Rate: " + resultSet.getString("monthly_interest"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println(
                    "=======================================================================================================================");
        }
    }

    private static void listStockPrice(OracleConnection connection, Scanner scanner) {
        // Implement list stock price logic
        System.out.println("List Current Stock Price option selected.");
        System.out.println("Please enter the symbol of the stock you would like to view: ");
        String symbol = scanner.nextLine();
        double stockPrice = getStockPrice(connection, symbol);
        if (stockPrice != -1.0) {
            System.out.println("Stock exists.");
            System.out.println("Current price of " + symbol + " is: " + stockPrice);
            String selectQuery = "SELECT * FROM Stock_Actor WHERE symbol = " + "'" + symbol + "'";
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    System.out.println("Actor Name: " + resultSet.getString("actor_name"));
                    System.out.println("Date of Birth: " + resultSet.getString("date_of_birth").substring(0, 10));
                    // Check if closing_price is null
                    if (resultSet.getObject("closing_price") == null) {
                        System.out.println("Closing Price: N/A");
                    } else {
                        System.out.println("Closing Price: " + resultSet.getDouble("closing_price"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Stock does not exist.");
        }

    }

    

    private static void movieInformation(OracleConnection connection, Scanner scanner) {
        // Implement movie information logic
        System.out.println("Movie Information option selected.");
        System.out.println("Enter the name of the movie you would like to view: ");
        String movieName = scanner.nextLine();
        System.out.println("Enter the year of the movie you would like to view: ");
        int year = 0;
        while (true) {
            System.out.print("Type the year of the movie you wish to view: ");
            try {
                year = Integer.parseInt(scanner.nextLine());
                break; // Exit the loop if the input is a valid integer
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer. Try again.");
            }
        }
        String selectQuery = "SELECT * FROM Movie WHERE title = " + "'" + movieName + "'" + " AND movie_year = "
                + Integer.toString(year);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if (resultSet.next()) {
                System.out.println("Movie Title: " + resultSet.getString("title"));
                System.out.println("Year: " + resultSet.getString("movie_year"));
            } else {
                System.out.println("Movie does not exist.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String getAverageReviewsQuery = "SELECT * FROM Review WHERE title = " + "'" + movieName + "'"
                + " AND movie_year = " + Integer.toString(year);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(getAverageReviewsQuery);
            int count = 0;
            int sum = 0;
            while (resultSet.next()) {
                count++;
                sum += resultSet.getInt("rating");
            }
            if (count == 0) {
                System.out.println("No reviews for this movie, cannot determine average rating.");
            } else {
                System.out.println("Average Rating: " + (double) sum / count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static boolean doesAccountExist(OracleConnection connection, String username) {
        String selectQuery = "SELECT * FROM Account_Has WHERE username = " + "'" + username + "'";
        try (Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery(
                    selectQuery);
            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            // e.printStackTrace();
            // Handle any exceptions that may occur during database access
        }

        return (false);
    }

    

    public static ArrayList<Integer> getUserTransactions(OracleConnection connection, String username) {
        ArrayList<Integer> transactions = new ArrayList<>();
        String selectQuery = "SELECT c.tid FROM Commits c "
                + "JOIN Transactions t ON c.tid = t.tid "
                + "WHERE c.username = '" + username + "' "
                + "ORDER BY t.date_executed";

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()) {
                transactions.add(resultSet.getInt("tid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions that may occur during database access
        }

        return transactions;
    }

    private static void listAllReviews(OracleConnection connection, Scanner scanner) {
        System.out.println("List all Reviews for Movie option selected.");
        System.out.println("Enter the name of the movie you would like to view: ");
        String movieName = scanner.nextLine();
        System.out.println("Enter the year of the movie you would like to view: ");
        int year = 0;
        while (true) {
            System.out.print("Type the year of the movie you wish to view: ");
            try {
                year = Integer.parseInt(scanner.nextLine());
                break; // Exit the loop if the input is a valid integer
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer. Try again.");
            }
        }
        String selectQuery = "SELECT * FROM Review WHERE title = " + "'" + movieName + "'" + " AND movie_year = "
                + Integer.toString(year);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            int count = 1;
            boolean found = false;
            while (resultSet.next()) {
                found = true;
                System.out.println("Review " + count + ": " + resultSet.getString("written_review"));
                System.out.println("Rating: " + resultSet.getInt("rating"));
                count++;
            }
            if (found == false) {
                System.out.println("Movie does not exist.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // declare helper functions to print transactions

    public static boolean isOpen(OracleConnection connection) {
        if (Demo.isOpen(connection) == false) {
            System.out.println("Market is closed. Wait until market is open");
            return false;
        }
        return true;
    }

    public static boolean have20(OracleConnection connection, String username, int acc_id) {
        if (getBalance(connection, acc_id) < 20) {
            System.out.println("You need at least $20 to execute this transaction.");
            return false;
        }
        return true;
    }

    public static void topMovies(OracleConnection connection, Scanner scanner) {
        System.out.println("Top Movies option selected.");
        System.out.println("Enter the start year to search");
        int start_year = 0;
        while (true) {
            System.out.print("Type the start year: ");
            try {
                start_year = Integer.parseInt(scanner.nextLine());
                break; // Exit the loop if the input is a valid integer
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer. Try again.");
            }
        }
        System.out.println("Enter the end year to search");
        int end_year = 0;
        while (true) {
            System.out.print("Type the end year: ");
            try {
                end_year = Integer.parseInt(scanner.nextLine());
                break; // Exit the loop if the input is a valid integer
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer. Try again.");
            }
        }
        if (start_year > end_year) {
            System.out.println("Start year must be less than or equal to end year.");
            return;
        }
        if (start_year < 0 || end_year < 0) {
            System.out.println("Start year and end year must be greater than or equal to zero.");
            return;
        }
        String selectQuery = "SELECT * FROM Review WHERE movie_year >= " + Integer.toString(start_year)
                + " AND movie_year <= " + Integer.toString(end_year) + "AND rating = 10";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            HashMap<String, Integer> uniqueMovies = new HashMap<>();
            int count = 1;
            while (resultSet.next()) {
                String movieTitle = resultSet.getString("title");
                int movieYear = resultSet.getInt("movie_year");
                String movieKey = movieTitle + " " + movieYear;

                // Check if the movie is not already in the HashMap
                if (!uniqueMovies.containsKey(movieKey)) {
                    System.out.println("Movie: " + movieTitle + " " + movieYear);
                    uniqueMovies.put(movieKey, 1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addToTempMoney(OracleConnection connection, String username) {
        int bid = 0;
        while (true) {
            Random random = new Random();
            bid = random.nextInt(Integer.MAX_VALUE);
            String selectQuery = "SELECT * FROM Temp_Money WHERE bid = " + Integer.toString(bid);
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                if (resultSet.next()) {
                    continue;
                } else {
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        String insertQuery = "INSERT INTO Temp_Money (bid, temp_balance, balance_date, acc_id, time_order) VALUES (?, ?, ?, ?, ?)";
        int acc_id = getAccountId(username, connection);
        double temp_balance = getBalance(connection, acc_id);
        Date balance_date = Demo.getDateSQLFriendly(connection);
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, bid);
            preparedStatement.setDouble(2, temp_balance);
            preparedStatement.setDate(3, balance_date);
            preparedStatement.setInt(4, acc_id);
            Date myDate = new Date(System.currentTimeMillis());
            preparedStatement.setDate(5, myDate);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Temp_Money created successfully!");
            } else {
                System.out.println("Temp_Money creation failed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void listAllStockAccounts(OracleConnection connection, String username) {
        clearStockAccountsWithZero(connection);
        String selectQuery = "SELECT * FROM Stock_Account WHERE username = " + "'" + username + "'";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()) {
                System.out.println("Symbol: " + resultSet.getString("symbol"));
                System.out.println("Number of Shares: " + resultSet.getInt("num_share"));
                System.out.println("Amount spent (not value): " + resultSet.getDouble("balance_share"));
                System.out.println("Value of shares: "
                        + getStockPrice(connection, resultSet.getString("symbol")) * resultSet.getInt("num_share"));
                System.out.println(
                        "=======================================================================================================================");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeEmptyBoughtStock(OracleConnection connection) {
        String selectQuery = "SELECT * FROM Bought_Stock WHERE shares_bought = 0";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()) {
                String deleteQuery = "DELETE FROM Bought_Stock WHERE username = " + "'"
                        + resultSet.getString("username") + "'" + " AND symbol = " + "'" + resultSet.getString("symbol")
                        + "' AND buy_price = " + Double.toString(resultSet.getDouble("buy_price"));
                try (Statement statement2 = connection.createStatement()) {
                    int rowsAffected = statement2.executeUpdate(deleteQuery);
                    if (rowsAffected > 0) {
                        System.out.println("Deleted Bought_Stock successful!");
                    } else {
                        System.out.println("Delete Bought_Stock failed.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void clearStockAccountsWithZero(OracleConnection connection) {
        String selectQuery = "SELECT * FROM Stock_Account WHERE num_share = 0";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()) {
                String deleteQuery = "DELETE FROM Stock_Account WHERE username = " + "'"
                        + resultSet.getString("username") + "'" + " AND symbol = " + "'" + resultSet.getString("symbol")
                        + "'";
                try (Statement statement2 = connection.createStatement()) {
                    int rowsAffected = statement2.executeUpdate(deleteQuery);
                    if (rowsAffected > 0) {
                        System.out.println("Deleted Stock_Account successful!");
                    } else {
                        System.out.println("Delete Stock_Account failed.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
