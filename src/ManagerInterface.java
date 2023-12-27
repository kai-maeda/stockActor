import java.util.Scanner;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import javax.swing.SwingUtilities;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.sql.ResultSetMetaData;
import java.text.DecimalFormat;
import java.sql.Date;


public class ManagerInterface {
    public static void parentFunction(OracleConnection connection, String username) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("=======================================================================================================================");
            System.out.println("Manager Interface Options:");
            System.out.println("1. Add Interest");
            System.out.println("2. Generate Montly Statement");
            System.out.println("3. List Active Customers");
            System.out.println("4. Generate Government Drug & Tax Evasion Report (DTER)");
            System.out.println("5. Customer Report");
            System.out.println("6. Delete Transactions");
            System.out.println("7. Change Monthly Interest Rate");
            System.out.println("0. Exit");
            System.out.println("=======================================================================================================================");

            System.out.print("Enter your choice (0-7): ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            System.out.println("=======================================================================================================================");
            switch (choice) {
                case 1:
                    addInterest(connection,username);
                    break;
                case 2:
                    generateMonthlyStatement(connection, scanner);
                    break;
                case 3:
                    listActiveCustomers(connection);
                    break;
                case 4:
                    generateDTER(connection);
                    break;
                case 5:
                    customerReport(connection, scanner);
                    break;
                case 6:
                    deleteTransactions(connection);
                    break;
                case 7:
                    changeMonthlyInterest(connection, scanner);
                    break;
                case 0:
                    System.out.println("Exiting Manager Interface...");
                    StartupOptions.main2(connection);
                default:
                    System.out.println("Invalid option. Please choose a valid option (0-7).");
                    break;
            }
        }
    }
    public static void addInterest(OracleConnection connection, String username) {
        //need list of temp_money and initial balance and length of month

        Date clean_date = Demo.getDateSQLFriendly(connection);
        String curr_date = Demo.getDate(connection,1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate curr_date2 = LocalDate.parse(curr_date, formatter); 
        int last_day = curr_date2.lengthOfMonth();
        Month curr_month = curr_date2.getMonth();
        if(curr_date2.getDayOfMonth() == last_day) {
            String selectQuery1 = ("SELECT monthlyInterest " + 
                                    "FROM Manager " +  
                                    "WHERE username = '" + username + "'");
            String selectQuery2 = "SELECT * " +
                                    "FROM Market_Account";
            String insertQuery1 = "INSERT INTO Interests (tid, monthly_interest) " +
                                    "VALUES (?, ?)";
            try(Statement statement = connection.createStatement()) {
                ResultSet resultSet1 = statement.executeQuery(selectQuery1);
                float monthlyInterest;
                if(resultSet1.next()) {
                    monthlyInterest = resultSet1.getFloat("monthlyInterest");
                    resultSet1.close();
                } else {
                    monthlyInterest = 0.01f;
                }
                try(Statement statement4 = connection.createStatement()) {
                    ResultSet resultSet3 = statement4.executeQuery(selectQuery2);
                    while(resultSet3.next()) {
                        float avg_balance = 0;
                        int acc_id = resultSet3.getInt("acc_id");
                        String acc_username = resultSet3.getString("username").trim();
                        int tid = TraderInterface.createTransaction(connection, acc_username,1);
                        String selectQuery4 = "SELECT T.* FROM Market_Account M, Temp_money T WHERE M.acc_id = T.acc_id AND M.acc_id = " + acc_id + 
                                                " AND EXTRACT(MONTH FROM T.balance_date) = EXTRACT(MONTH FROM TO_DATE('" + curr_date2 + "', 'yyyy-mm-dd')) ORDER BY T.balance_date DESC";
                        float[] balance_results = getInitialAndFinalBalance(connection,acc_username,1);
                        try(Statement statement2 = connection.createStatement()) {
                            ResultSet resultSet4 = statement2.executeQuery(selectQuery4);
                            int prev_day = last_day;
                            while(resultSet4.next()) {
                                java.sql.Date balance_date = resultSet4.getDate("balance_date");
                                LocalDate fixed_balance_date = balance_date.toLocalDate();
                                float temp_balance2 = resultSet4.getFloat("temp_balance");
                                Month temp_month  = fixed_balance_date.getMonth();
                                int temp_day = fixed_balance_date.getDayOfMonth();
                                avg_balance += (prev_day-temp_day+1)* temp_balance2;
                            }
                            resultSet4.close();
                            avg_balance += (prev_day) * balance_results[0];
                        } 
                        avg_balance /= last_day;

                        String updateQuery1 = ("UPDATE Market_Account " + 
                                               "SET balance = balance + " + avg_balance + " * " + monthlyInterest +
                                              " WHERE acc_id = " + acc_id);
                        try(Statement statement3 = connection.createStatement()) {
                            statement3.executeUpdate(updateQuery1);
                        }
                        try(PreparedStatement preparedStatement = connection.prepareStatement(insertQuery1)){
                            preparedStatement.setInt(1, tid);
                            preparedStatement.setFloat(2,avg_balance * monthlyInterest);
                            preparedStatement.executeUpdate();
                        }
                        int bid = 0;
                        while(true){
                            Random random = new Random();
                            bid = random.nextInt(Integer.MAX_VALUE);
                            String selectQuery6 = "SELECT * FROM Temp_Money WHERE bid = " + Integer.toString(bid);
                            try (Statement statement7 = connection.createStatement()) {
                                ResultSet resultSet = statement7.executeQuery(selectQuery6);
                                if (resultSet.next()) continue;
                                break;
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        String insertQuery = "INSERT INTO Temp_Money (temp_balance, balance_date,acc_id, bid, time_order) VALUES (?,?,?,?,?)";
                        try(PreparedStatement preparedStatement4 = connection.prepareStatement(insertQuery)){
                            long time_order = System.currentTimeMillis();
                            Date current_date = new Date(time_order);
                            preparedStatement4.setFloat(1, balance_results[1] + avg_balance * monthlyInterest);
                            preparedStatement4.setDate(2,clean_date);
                            preparedStatement4.setInt(3,acc_id);
                            preparedStatement4.setInt(4,bid);
                            preparedStatement4.setDate(5,current_date);
                            preparedStatement4.executeUpdate();
                        }
                    }
                    resultSet3.close();
                }
                System.out.println("Added monthly interest rate of " + monthlyInterest + " to all customer market accounts!");
            }  catch (SQLException e) {e.printStackTrace();}
        } else {
            System.out.println("Sorry you are only able to add appropriate monthly interest on the last business day of the month. Please try again later!");
        }
    }    
    public static void generateMonthlyStatement(OracleConnection connection, Scanner scanner) {
        System.out.print("Please enter the username associated with the customer that you would like to receive a general monthly statement for: ");
        while(true) {
            if(scanner.hasNext()) {
                String correct_id = scanner.next();
                System.out.println("=======================================================================================================================");
                if(checkIfMarketAccountExists(connection, correct_id)) {
                    System.out.println("Sorry this person has not created a market account yet so there is no monthly statement to print.");
                    break;
                }
                generateListOfTransaction(connection, 0, correct_id);
                break;
            } else {
                String wrong_id = scanner.next();
                if("q".equalsIgnoreCase(wrong_id)) break;
                System.out.println("Sorry, we could not find a customer associated with that username.");
                System.out.print("Please enter a new username or type 'q' to return to Manager Interface: ");
            }
        }

    }    
    public static void listActiveCustomers(OracleConnection connection) {
        String curr_date = Demo.getDate(connection,1);
        String selectQuery = ("SELECT C.* " + 
                                "FROM Customer C, (SELECT T.username, SUM(T.sumShares) AS totalShares " + 
                                    "FROM (SELECT C.username, SUM(B.shares) AS sumShares " +
                                        "FROM Buy B, Transactions T, Commits C " +
                                        "WHERE B.tid = T.tid AND C.tid = T.tid AND EXTRACT(MONTH FROM T.date_executed) = " + "EXTRACT(MONTH FROM TO_DATE('" + curr_date + "', 'yyyy-mm-dd'))" +
                                        "GROUP BY C.username " +
                                        "UNION " +
                                        "SELECT C.username, SUM(S.shares) AS sumShares " +
                                        "FROM Sell S, Transactions T, Commits C " +
                                        "WHERE S.tid = T.tid AND C.tid = T.tid AND EXTRACT(MONTH FROM T.date_executed) = " +  "EXTRACT(MONTH FROM TO_DATE('" + curr_date + "', 'yyyy-mm-dd'))" +
                                        "GROUP BY C.username) T " + 
                                    "GROUP BY T.username) T2 " + 
                                "WHERE C.username = T2.username AND T2.totalShares >= 1000");
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            System.out.println("Here is a list of all customers who have bought or sold at least 1000 shares this month...");
            while(resultSet.next()) {
                String state_id = resultSet.getString("state_id").trim();
                int tax_id = resultSet.getInt("tax_id");
                String cname = resultSet.getString("cname").trim();
                String phone = resultSet.getString("phone").trim();
                String email = resultSet.getString("email").trim();
                String username = resultSet.getString("username").trim();
                String password = resultSet.getString("password").trim();
                System.out.println("State ID: " + state_id + ", Tax ID: " + tax_id + ", Customer Name: " + cname + ", Phone Number: " + phone + 
                ", Email: " + email + ", Username: " + username + ", Password: " + password);
            }
        } catch (SQLException e) {e.printStackTrace();}
    }    
    public static void generateDTER(OracleConnection connection) {
        String selectQuery = "SELECT * FROM Customer";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            System.out.println("Generating list of customers who have made more than $10,000 in the past month...");
            while(resultSet.next()) {
                String username = resultSet.getString("username").trim();
                float total_earnings = generateListOfTransaction(connection, 1, username);
                if(total_earnings >= 10000) {
                    String state_id = resultSet.getString("state_id").trim();
                    String cname = resultSet.getString("cname").trim();
                    String phone = resultSet.getString("phone").trim();
                    String email = resultSet.getString("email").trim();
                    int tax_id = resultSet.getInt("tax_id");
                    String password = resultSet.getString("password").trim();
                    DecimalFormat df = new DecimalFormat("#.##");
                    String total_format = df.format(total_earnings);
                    System.out.println("State ID = " + state_id + ", Tax ID = " + tax_id + ", Name = " + cname + ", Phone number = " + phone + ", Email = " + email + ", Username = "+ username + ", Password = "+ password +", Total Earnings = $" + total_format);
                }
            }
        } catch (SQLException e) {e.printStackTrace();}
        //
    }    

    public static void customerReport(OracleConnection connection, Scanner scanner) {
        System.out.print("Please enter the username associated with the customer that you would like to receive a customer report for: ");
        while(true) {
            if(scanner.hasNext()) {
                String correct_id = scanner.next();
                if(doesCustomerExist(connection, correct_id) == false) {
                    System.out.println("Sorry, we could not find a customer associated with that username.");
                    System.out.print("Please enter a new username or type 'q' to return to Manager Interface: ");
                    continue;
                }
                System.out.println("=======================================================================================================================");
                String selectQuery1 = "SELECT * FROM Customer WHERE username = '" + correct_id + "'";
                String selectQuery2 = "SELECT * FROM Market_Account WHERE username = '" + correct_id + "'";
                String selectQuery3 = "SELECT * FROM Stock_Account WHERE username = '" + correct_id + "'";
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet1 = statement.executeQuery(selectQuery1);
                    if(resultSet1.next()) {
                        String state_id = resultSet1.getString("state_id").trim();
                        int tax_id = resultSet1.getInt("tax_id");
                        String cname = resultSet1.getString("cname").trim();
                        String phone = resultSet1.getString("phone").trim();
                        String email = resultSet1.getString("email").trim();
                        String username = resultSet1.getString("username").trim();
                        String password = resultSet1.getString("password").trim();
                        System.out.println("Here is the customer you were searching for!");
                        System.out.println("State ID: " + state_id );
                        System.out.println("Tax ID: " + tax_id);
                        System.out.println("Customer Name: " + cname);
                        System.out.println("Phone Number: " + phone);
                        System.out.println("Email: " + email);
                        System.out.println("Username: " + username);
                        System.out.println("Password: " + password);
                    }
                    System.out.println("=======================================================================================================================");
                    try(Statement statement2 = connection.createStatement()) {
                        ResultSet resultSet2 = statement2.executeQuery(selectQuery2);
                        if(resultSet2.next()) {
                            float balance = resultSet2.getFloat("balance");
                            DecimalFormat df = new DecimalFormat("#.##");
                            String balance_format = df.format(balance);
                            int acc_id = resultSet2.getInt("acc_id");
                            System.out.println("Market Account ID: " + acc_id);
                            System.out.println("Balance: $" + balance_format);
                        } else {
                            System.out.println("This person has not yet created a Market Account.");
                        }
                    }
                    System.out.println("=======================================================================================================================");
                    System.out.println("Here are the stocks that they own!");
                    try(Statement statement3 = connection.createStatement()) {
                        ResultSet resultSet3 = statement3.executeQuery(selectQuery3);
                        while(resultSet3.next()) {
                            String symbol = resultSet3.getString("symbol").trim();
                            int num_share = resultSet3.getInt("num_share");
                            System.out.println("Symbol: " + symbol + ", Number of Shares: " + num_share);
                        }
                    }
                } catch (SQLException e) {e.printStackTrace();}
                break;
            } else {
                String wrong_id = scanner.next();
                if("q".equalsIgnoreCase(wrong_id)) break;
                System.out.println("Sorry, we could not find a customer associated with that username.");
                System.out.print("Please enter a new username or type 'q' to return to Manager Interface: ");
            }
        }
        
    }    
    public static void deleteTransactions(OracleConnection connection) {
        String deleteQuery = "DELETE FROM Transactions";
        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate(deleteQuery);
            System.out.println("Successfully deleted the list of transactions for all customers!");
        } catch (SQLException e) {e.printStackTrace();}
    }
    public static void changeMonthlyInterest(OracleConnection connection, Scanner scanner) {
        System.out.print("Enter the desired monthly interest rate to be set for all customers. (Ex: 0.01): ");
        while(true) {
            if(scanner.hasNextFloat()) {
                float temp = scanner.nextFloat();
                if(temp <= 0) {
                    System.out.print("Please enter a valid decimal > 0 or type 'q' to return to Manager Interface:");
                } else {
                    String updateQuery = "UPDATE Manager SET monthlyInterest = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setFloat(1, temp);
                        preparedStatement.executeUpdate();
                    } catch(SQLException e) {e.printStackTrace();}
                    System.out.println("Successfully changed the monthly interest rate of all customers to be " + temp + "!");
                    break;
                }
            }else {
                String input = scanner.next();
                if("q".equalsIgnoreCase(input)) {
                    break;
                } else {
                    System.out.print("Please enter a valid decimal > 0 or type 'q' to return to Manager Interface:");
                }
            }
        }
    }    
    public static float generateListOfTransaction(OracleConnection connection, int flag, String username) {
        String curr_date = Demo.getDate(connection,1);
        String[] types_trans = {"Buy", "Sell", "Withdraw", "Deposit", "Cancels", "Interests"};
        float total_earnings = 0;
        float commissions = 0;
        DecimalFormat df = new DecimalFormat("#.##");
        //flag = 0 means for monthlyStatement, flag = 1 for DTER
        for(int i = 0; i < types_trans.length; i++) {
            String curr_type = types_trans[i];
            String selectQuery = "SELECT A.acc_id, T.date_executed, " + curr_type.charAt(0) +  ".* " + 
                                    "FROM Account_has A, Commits O, Transactions T, " + curr_type + " " + curr_type.charAt(0) + 
                                    " WHERE A.username = O.username AND A.acc_id = O.acc_id AND O.tid = T.tid AND " + curr_type.charAt(0) + ".tid = T.tid" +
                                    " AND A.username = '" + username + "' AND EXTRACT(MONTH FROM T.date_executed) = EXTRACT(MONTH FROM TO_DATE('" + curr_date + "', 'yyyy-mm-dd'))";
            try(Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(selectQuery);
                while(resultSet.next()) {
                    int acc_id = resultSet.getInt("acc_id");
                    String date_executed = resultSet.getString("date_executed").trim();
                    int tid = resultSet.getInt("tid");
                    if(flag == 0) System.out.print("Account ID: " + acc_id + ", Date executed: " + date_executed + ", Transaction ID: " + tid);
                    if(i == 0) {
                        commissions += 20;
                        String symbol = resultSet.getString("symbol").trim();
                        int shares = resultSet.getInt("shares");
                        float buy_price = resultSet.getFloat("buy_price");
                        if(flag == 0) System.out.print(", Stock symbol: " + symbol + ", Shares: " + shares + ", Buy price: " + buy_price + "\n");
                    } else if(i == 1) {
                        commissions += 20;
                        String symbol = resultSet.getString("symbol").trim();
                        float sell_price = resultSet.getFloat("sell_price");
                        int shares = resultSet.getInt("shares");
                        if(flag == 0) System.out.print(", Stock symbol: " + symbol + ", Shares: " + shares + ", Sell price: " + sell_price + "\n");
                        total_earnings += getSellLegs(connection,tid,sell_price, flag);
                    } else if(i == 2) {
                        float amount = resultSet.getFloat("amount");
                        if(flag == 0) System.out.print(", Withdrawed amount: " + amount + "\n");
                    } else if(i == 3) {
                        float amount = resultSet.getFloat("amount");
                        if(flag == 0) System.out.print(", Deposited amount: " + amount + "\n");
                    } else if(i == 4) {
                        commissions += 20;
                        int ptid = resultSet.getInt("ptid");
                        if(flag == 0) System.out.print(", Cancelled transaction ID: " + ptid + "\n");
                    }
                    else {
                        float monthly_interest = resultSet.getFloat("monthly_interest");
                        total_earnings += monthly_interest;
                        if(flag == 0) System.out.print(", Monthly interest earned: $" + monthly_interest + "\n");
                    }
                }
            } catch (SQLException e) {e.printStackTrace();}
        }    
        if(flag == 0) System.out.println("=======================================================================================================================");
        getInitialAndFinalBalance(connection, username, flag);
        String total_format = df.format(total_earnings);
        String commisions_format = df.format(commissions);
        if(total_earnings >= 0) {
            if(flag == 0) System.out.println("Total earnings this month = $" + total_format);
        } else {
            if(flag == 0) System.out.println("Total loss this month = $" + total_format);
        }
        if(flag == 0) System.out.println("Total commission paid = $" + commisions_format);
        return total_earnings;
    }
    public static int getSellLegs(OracleConnection connection, int tid, float sell_price, int flag) {
        int money_earned = 0;
        String selectQuery = "SELECT  L.* FROM Sell_leg L, Sell S WHERE L.tid = S.tid AND S.tid = " + tid; 
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while(resultSet.next()) {
                float bought_price = resultSet.getFloat("bought_price");
                DecimalFormat df = new DecimalFormat("#.##");
                String bought_format = df.format(bought_price);
                int shares = resultSet.getInt("shares");
                int leg_id = resultSet.getInt("leg_id");
                if(flag == 0) System.out.println("Leg ID = " + leg_id + ", Bought price = $" + bought_format + ", Shares = " + shares);
                money_earned += (sell_price - bought_price)*shares;
            }
        } catch (SQLException e) {e.printStackTrace();}
        return money_earned;
    }
    public static float[] getInitialAndFinalBalance(OracleConnection connection, String username, int flag) {
        String curr_date = Demo.getDate(connection,1);
        DecimalFormat df = new DecimalFormat("#.##");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate curr_date2 = LocalDate.parse(curr_date, formatter2);
        Month month1 = curr_date2.getMonth();
        String selectQuery = "SELECT T.* FROM Market_Account M, Temp_money T WHERE username = '" + username + 
                            "' AND M.acc_id = T.acc_id ORDER BY T.balance_date DESC, T.time_order DESC";
        String final_balance = "0";
        String initial_balance = "0";
        float temp_balance = 0;
        try(Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if(resultSet.next()) {
                temp_balance = resultSet.getFloat("temp_balance");
                final_balance = df.format(temp_balance);
                String balance_date = resultSet.getString("balance_date").trim();
                LocalDate balance_date2 = LocalDate.parse(balance_date, formatter);
                Month month2 = balance_date2.getMonth();
                if(month1 != month2) {
                    initial_balance = df.format(temp_balance);
                    if(flag == 0) System.out.println("Inital account balance = $" + initial_balance + ", Final account balance = $" + final_balance);
                    return new float[]{temp_balance, Float.parseFloat(final_balance)};
                }
            }
            while(resultSet.next()) {
                String balance_date = resultSet.getString("balance_date").trim();
                LocalDate balance_date2 = LocalDate.parse(balance_date, formatter);
                Month month2 = balance_date2.getMonth();
                if(month1 != month2 ) {
                    temp_balance = resultSet.getFloat("temp_balance");
                    initial_balance = df.format(temp_balance);
                    if(flag == 0) System.out.println("Inital account balance = $" + initial_balance + ", Final account balance = $" + final_balance);
                    return new float[]{temp_balance, Float.parseFloat(final_balance)};
                }
            }
            if(resultSet.previous()) {
                temp_balance = resultSet.getFloat("temp_balance");
                initial_balance = df.format(temp_balance);
                if(flag == 0) System.out.println("Inital account balance = $" + initial_balance + " Final account balance = $" + final_balance);
            }
        } catch (SQLException e) {e.printStackTrace();}
        return new float[]{temp_balance, Float.parseFloat(final_balance)};
    }
    public static boolean checkIfMarketAccountExists(OracleConnection connection, String username) {
        String selectQuery = "SELECT * FROM Market_Account WHERE username = '" + username + "'";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if(resultSet.next()) {
                return false;
            }
        } catch (SQLException e) {e.printStackTrace();}
        return true;
    }
    public static boolean doesCustomerExist(OracleConnection connection, String username) {
        String selectQuery = "SELECT * FROM Customer WHERE username = '" + username + "'";
        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectQuery);
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {e.printStackTrace();}
        return false;
    }

}
