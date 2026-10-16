package com.scc;


import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userdao = new UserDao();

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
                    String roleChoice = sc.nextLine();
                    String role;
                    switch (roleChoice) {
                        case "1":
                            role = "ADMINISTRATOR";
                            break;
                        case "2":
                            role = "DISPATCHER";
                            break;
                        case "3":
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
        System.out.println("\n    ======= LOG IN =======");

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
            switch (user.roles()){
                case "ADMIN" -> new AdminMenu(sc, userdao).display(user);
            }
        }
    }

}
