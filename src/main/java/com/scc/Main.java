package com.scc;


import com.scc.dao.UserDao;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userdao = new UserDao();

    public static void main(String[] args){

        System.out.println("\n==============================");
        System.out.println("Fleet Managements System");
        System.out.println("=================================");
        System.out.println("1. login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.println("CHOOSE AN OPTION: ");
        String choice = sc.nextLine();
        switch(choice){
            case "1":
                break;
            case "2":
                System.out.print("Enter new username: ");
                String user = sc.nextLine();
                System.out.print("Enter password: ");
                String pass = sc.nextLine();


                System.out.print("Select Role [1] ADMINISTRATOR | [2] DISPATCHER | [3] DRIVER: ");
                int roleChoice = sc.nextInt();
                String role;
                switch (roleChoice) {
                    case 1:
                        role = "ADMINISTRATOR";
                        break;
                    case 2:
                        role = "DISPATCHER";
                        break;
                    case 3:
                        role = "DRIVER";
                        break;
                    default:
                        role = null;
                }

                if(role != null){
                    userdao.registerUser(user, pass,role);

                }else {
                    System.out.println("Invalid role selection!");
                }

                break;



    }


    }

}
