package cli;

import utility.LoggerService;

import java.util.Scanner;

public class MainMenuHandler {
    //Logger instance
    private static final LoggerService logger = LoggerService.getInstance();
    private Scanner scanner;

    public MainMenuHandler(Scanner scanner) {
        this.scanner = scanner;
    }


    //---------Display Main Menu------
    public void displayMainMenu() {
        System.out.println("\n====== TEAMMATE COMMUNITY SYSTEM ======");
        System.out.println("Login as:");
        System.out.println("1. Participant");
        System.out.println("2. Organizer");
        System.out.println("3. Exit");
        System.out.print("Select option: ");
    }
    //get user input as an intiger
    public int getLoginChoice() {
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();

            return choice;
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Invalid input. Please enter a number.");
            return -1;
        }
    }
}