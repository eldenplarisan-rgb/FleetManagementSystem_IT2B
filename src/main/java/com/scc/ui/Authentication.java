package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final Scanner sc;
    private final UserDao userdao;
    private final ActivityLogDao logDao = new ActivityLogDao();

    public Authentication(Scanner sc, UserDao userdao) {
        this.sc = sc;
        this.userdao = userdao;
    }

    // Main entry screen: Login / Register / Exit
    public void display() {
        boolean running = true;
        while (running) {
            System.out.println("\n==============================");
            System.out.println("Fleet Managements System");
            System.out.println("=================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("CHOOSE AN OPTION: ");

            switch (sc.nextLine().trim()) {
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

    // ===================== LOGIN =====================
    private void handleLogin() {
        System.out.println("\n    ======= LOG IN =======");

        String username = askRequired("Enter Username");
        String password = askRequired("Enter Password");

        User user = userdao.login(username, password);
        if (user == null) {
            return;   // login() already printed the reason
        }

        logDao.logAction(user.user_id(), "USER_LOGIN",
                "User [" + user.user_id() + "] logged in as " + user.roles());

        switch (user.roles()) {
            case "ADMIN" -> new AdminMenu(sc, userdao).display(user);
            default -> System.out.println("No menu available yet for role: " + user.roles());
        }
    }

    // ===================== REGISTER =====================
    private void handleRegister() {
        System.out.println("\n    ======= REGISTER =======");

        String name = askRequired("Enter your Fullname");
        String username = askRequired("Enter username");
        String password = askRequired("Enter password");

        System.out.print("Select Role [1] ADMIN | [2] DISPATCHER | [3] DRIVER: ");
        String role;
        switch (sc.nextLine().trim()) {
            case "1": role = "ADMIN"; break;
            case "2": role = "DISPATCHER"; break;
            case "3": role = "DRIVER"; break;
            default:  role = null;
        }

        if (role == null) {
            System.out.println("Invalid role selection!");
            return;
        }
        userdao.registerUser(name, username, password, role);
    }

    // Keeps asking until the field is not empty
    private String askRequired(String label) {
        String value;
        do {
            System.out.print(label + ": ");
            value = sc.nextLine().trim();
            if (value.isEmpty()) System.out.println("Invalid! This field cannot be empty.");
        } while (value.isEmpty());
        return value;
    }
}