package com.scc;


import com.scc.dao.ActivityLogDao;        // NEW
import com.scc.dao.Databaseinitializer;   // NEW
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    // NEW: mo-create sa activity_logs table (ug users table) inig start
    static {
        Databaseinitializer.initialize();
    }

    private static final UserDao userdao = new UserDao();
    private static final ActivityLogDao logDao = new ActivityLogDao();   // NEW

    public static void main(String[] args){
        while(true){
            System.out.println("\n==============================");
            System.out.println("Fleet Managements System");
            System.out.println("=================================");
            System.out.println("1. login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("CHOOSE AN OPTION: ");
            String choice = sc.nextLine();
            switch(choice){
                case "1":
                    handleLogin();
                    break;
                case "2":

                    System.out.print("Enter your Fullname:  ");
                    String name = sc.nextLine();
                    System.out.print("Enter username: ");
                    String user = sc.nextLine();
                    System.out.print("Enter password: ");
                    String pass = sc.nextLine();


                    System.out.print("Select Role [1] ADMINISTRATOR | [2] DISPATCHER | [3] DRIVER: ");
                    String roleChoice = sc.nextLine().trim().toUpperCase();   // FIX: trim + uppercase
                    String role;
                    switch (roleChoice) {
                        case "1":
                        case "ADMINISTRATOR":   // FIX
                        case "ADMIN":           // FIX
                            role = "ADMINISTRATOR";
                            break;
                        case "2":
                        case "DISPATCHER":      // FIX
                            role = "DISPATCHER";
                            break;
                        case "3":
                        case "DRIVER":          // FIX
                            role = "DRIVER";
                            break;
                        default:
                            role = null;
                    }

                    if(role != null){
                        userdao.registerUser(name,user, pass,role);
                    }else {
                        System.out.println("Invalid role selection!");
                    }
                    break;

            }
        }
    }

    public static void handleLogin(){
        System.out.println("\n ================== LOG IN ====================");

        String username, password;
        while (true){
            System.out.print("Enter Username: ");
            username = sc.nextLine();
            if(username.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else {
                break;
            }
        }

        while (true){
            System.out.print("Enter Password: ");
            password = sc.nextLine();
            if(password.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else{
                break;
            }
        }

        User user = userdao.login(username, password);

        if(user != null){
            // NEW: i-record sa DB ang matag successful login (bisan unsa nga role)
            logDao.logAction(user.user_id(), "USER_LOGIN",
                    "User [" + user.user_id() + "] " + user.username() + " logged in as " + user.roles());

            switch (user.roles()){
                case "ADMIN" -> new AdminMenu(sc, userdao).display(user);
                default -> System.out.println("No menu available yet for role: " + user.roles());   // NEW
            }
        }
    }

}