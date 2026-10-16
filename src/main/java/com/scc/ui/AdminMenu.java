package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userdao;

    public AdminMenu(Scanner sc, UserDao userdao) {
        this.sc = sc;
        this.userdao = userdao;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  FLEET OPERATIONS, DISPATCH & TRANSIT");
            System.out.println("  SCHEDULING SYSTEM");
            System.out.println("  ADMIN MENU  | Logged in: " + admin.name());
            System.out.println("==========================================");
            System.out.println("  [1] Manage User Accounts");
            System.out.println("  [2] Manage Vehicle Records");
            System.out.println("  [3] Manage Driver Records");
            System.out.println("  [4] Manage Trip Schedules");
            System.out.println("  [5] Fleet Operations Summary Report");
            System.out.println("  [6] System Activity Logs");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    System.out.println("Manage User Accounts is not available yet.");
                    break;
                case "2":
                    System.out.println("Manage Vehicle Records is not available yet.");
                    break;
                case "3":
                    System.out.println("Manage Driver Records is not available yet.");
                    break;
                case "4":
                    System.out.println("Manage Trip Schedules is not available yet.");
                    break;
                case "5":
                    System.out.println("Fleet Operations Summary Report is not available yet.");
                    break;
                case "6":
                    System.out.println("System Activity Logs is not available yet.");
                    break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + admin.name() + "!");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }
}