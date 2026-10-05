package db.crud;

import db.crud.dao.UserDaoImpl;
import db.crud.dao.UserDaoInterface;
import db.crud.model.User;
import db.crud.service.UserService;
import db.crud.util.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        UserDaoInterface userDao = new UserDaoImpl();
        UserService userService = new UserService(userDao);

        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        while (running) {

            printMenu();

            int choice = readInt(scanner);

            try {

                switch (choice) {

                    case 1 -> createUser(scanner, userService);

                    case 2 -> readUser(scanner, userService);

                    case 3 -> readAllUsers(userService);

                    case 4 -> updateUser(scanner, userService);

                    case 5 -> deleteUser(scanner, userService);

                    case 0 -> {
                        running = false;
                        System.out.println("Выход из программы.");
                    }

                    default ->
                            System.out.println("Неверный пункт меню.");

                }

            } catch (Exception e) {

                System.out.println("Произошла ошибка:");
                e.printStackTrace();
            }
        }

        HibernateUtil.shutdown();
        scanner.close();
    }

    private static void printMenu() {

        System.out.println();
        System.out.println("========== USER SERVICE ==========");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Найти пользователя по ID");
        System.out.println("3. Показать всех пользователей");
        System.out.println("4. Обновить пользователя");
        System.out.println("5. Удалить пользователя");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private static void createUser(
            Scanner scanner,
            UserService userService) {

        System.out.print("Введите имя: ");
        String name = scanner.nextLine();

        System.out.print("Введите email: ");
        String email = scanner.nextLine();

        System.out.print("Введите возраст: ");
        int age = readInt(scanner);

        userService.createUser(name, email, age);
    }

    private static void readUser(
            Scanner scanner,
            UserService userService) {

        System.out.print("Введите ID пользователя: ");
        long id = Long.parseLong(scanner.nextLine());

        User user = userService.getUser(id);

        if (user != null) {
            System.out.println(user);
        }
    }

    private static void readAllUsers(
            UserService userService) {

        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Пользователей нет.");
            return;
        }

        System.out.println("\nСписок пользователей:");

        for (User user : users) {
            System.out.println(user);
        }
    }

    private static void updateUser(
            Scanner scanner,
            UserService userService) {

        System.out.print("Введите ID пользователя: ");
        long id = Long.parseLong(scanner.nextLine());

        User existingUser = userService.getUser(id);

        if (existingUser == null) {
            return;
        }

        System.out.println("Текущий пользователь:");
        System.out.println(existingUser);

        System.out.print("Новое имя: ");
        String name = scanner.nextLine();

        System.out.print("Новый email: ");
        String email = scanner.nextLine();

        System.out.print("Новый возраст: ");
        int age = readInt(scanner);

        userService.updateUser(id, name, email, age);
    }

    private static void deleteUser(
            Scanner scanner,
            UserService userService) {

        System.out.print("Введите ID пользователя: ");
        long id = Long.parseLong(scanner.nextLine());

        userService.deleteUser(id);
    }

    private static int readInt(Scanner scanner) {

        while (true) {

            try {

                return Integer.parseInt(scanner.nextLine());

            } catch (NumberFormatException e) {

                System.out.print("Введите целое число: ");
            }
        }
    }
}