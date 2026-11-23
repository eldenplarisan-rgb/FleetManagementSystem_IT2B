package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.Databaseinitializer;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;
import com.scc.ui.DispatcherMenu;
import com.scc.ui.DriverMenu;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    // mo-create sa activity_logs table (ug users table) inig start
    static {
        Databaseinitializer.initialize();
    }

    private static final UserDao userdao = new UserDao();
    private static final ActivityLogDao logDao = new ActivityLogDao();

    public static void main(String[] args){
        boolean running = true;
        while(running){
            System.out.println("\n=================================");
            System.out.println(" Welcome to Fleet Managements System");
            System.out.println("=================================");
            System.out.println("1. login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("CHOOSE AN OPTION: ");
            String choice = sc.nextLine().trim();
            switch(choice){
                case "1":
                    handleLogin();
                    break;
                case "2":
                    handleRegister();
                    break;
                case "3":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1, 2 or 3.");
            }
        }
    }

    private static void handleRegister(){
        System.out.print("Enter your Fullname:  ");
        String name = sc.nextLine().trim();
        System.out.print("Enter username: ");
        String user = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String pass = sc.nextLine().trim();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            System.out.println("Invalid! Fullname, username and password cannot be empty.");
            return;
        }

        System.out.print("Select Role [1] ADMIN | [2] DISPATCHER | [3] DRIVER: ");
        String roleChoice = sc.nextLine().trim().toUpperCase();
        String role;
        switch (roleChoice) {
            case "1":
            case "ADMINISTRATOR":
            case "ADMIN":
                role = "ADMIN";            // FIX: must be "ADMIN" (not "ADMINISTRATOR") to match the menu
                break;
            case "2":
            case "DISPATCHER":
                role = "DISPATCHER";
                break;
            case "3":
            case "DRIVER":
                role = "DRIVER";
                break;
            default:
                role = null;
        }

        if(role != null){
            userdao.registerUser(name, user, pass, role);
        }else {
            System.out.println("Invalid role selection!");
        }
    }

    public static void handleLogin(){
        System.out.println("\n ================== LOG IN ====================");

        String username, password;
        while (true){
            System.out.print("Enter Username: ");
            username = sc.nextLine().trim();
            if(username.isEmpty()){
                System.out.println("Invalid! Username cannot be empty.");
            }else {
                break;
            }
        }

        while (true){
            System.out.print("Enter Password: ");
            password = sc.nextLine().trim();
            if(password.isEmpty()){
                System.out.println("Invalid! Password cannot be empty.");
            }else{
                break;
            }
        }

        User user = userdao.login(username, password);

        if(user != null){
            // i-record sa DB ang matag successful login (bisan unsa nga role)
            logDao.logAction(user.user_id(), "USER_LOGIN",
                    "User [" + user.user_id() + "] " + user.username() + " logged in as " + user.roles());

            switch (user.roles()){
                case "ADMIN" -> new AdminMenu(sc, userdao).display(user);
                case "DISPATCHER" -> new DispatcherMenu(sc, userdao).display(user);
                case "DRIVER" -> new DriverMenu(sc, userdao).display(user);
                default -> System.out.println("No menu available yet for role: " + user.roles());
            }
        }
    }

}