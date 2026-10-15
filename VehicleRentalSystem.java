import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Vehicle Rental System - console-based Java college project.
 * Compile: javac VehicleRentalSystem.java
 * Run:     java VehicleRentalSystem
 * Demo admin login: admin / admin123
 * Payments are simulated; no real money is transferred.
 */
public class VehicleRentalSystem {
    private static final Scanner in = new Scanner(System.in);
    private static final Path DIR = Paths.get("vehicle_rental_data");
    private static final Path USERS_FILE = DIR.resolve("users.txt");
    private static final Path VEHICLES_FILE = DIR.resolve("vehicles.txt");
    private static final Path BOOKINGS_FILE = DIR.resolve("bookings.txt");
    private static final String ADMIN = "admin", ADMIN_PASSWORD = "admin123";
    private static final Map<String, User> users = new LinkedHashMap<>();
    private static final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
    private static final Map<String, Booking> bookings = new LinkedHashMap<>();
    private static User loggedIn;

    public static void main(String[] args) {
        try {
            Files.createDirectories(DIR);
            loadData();
            seedVehicles();
            System.out.println("====================================");
            System.out.println("       VEHICLE RENTAL SYSTEM");
            System.out.println("====================================");
            mainMenu();
        } catch (IOException e) {
            System.out.println("Could not initialize data: " + e.getMessage());
        } finally { in.close(); }
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("\n1. Customer sign up\n2. Customer login\n3. Administrator login\n0. Exit");
            switch (integer("Choose: ")) {
                case 1 -> signUp();
                case 2 -> login();
                case 3 -> adminLogin();
                case 0 -> { saveAll(); System.out.println("Thank you for using the system."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void customerMenu() {
        while (loggedIn != null) {
            System.out.println("\nCUSTOMER MENU - " + loggedIn.name);
            System.out.println("1. Browse vehicles by city\n2. Book a vehicle\n3. My booking history\n4. Return/close a booking\n5. My profile\n0. Log out");
            switch (integer("Choose: ")) {
                case 1 -> browse(); case 2 -> book(); case 3 -> myBookings();
                case 4 -> returnBooking(); case 5 -> profile();
                case 0 -> { loggedIn = null; System.out.println("Logged out."); }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void adminMenu() {
        while (true) {
            System.out.println("\nADMIN MENU\n1. View vehicles\n2. Add vehicle\n3. Remove/unlist vehicle\n4. Update vehicle\n5. Booking report\n0. Log out");
            switch (integer("Choose: ")) {
                case 1 -> printVehicles(new ArrayList<>(vehicles.values()));
                case 2 -> addVehicle(); case 3 -> removeVehicle(); case 4 -> updateVehicle();
                case 5 -> report(); case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void signUp() {
        System.out.println("\nCUSTOMER SIGN UP");
        String username = required("Username: ").toLowerCase(Locale.ROOT);
        if (username.contains("|") || users.containsKey(username) || username.equals(ADMIN)) {
            System.out.println("Username already exists or is invalid."); return;
        }
        User u = new User(username, required("Full name: "), required("Phone: "),
                required("Driving licence number: "), hash(required("Password: ")));
        users.put(username, u); saveUsers();
        System.out.println("Account created. You can now log in.");
    }

    private static void login() {
        String username = required("Username: ").toLowerCase(Locale.ROOT);
        String password = required("Password: ");
        User u = users.get(username);
        if (u != null && u.passwordHash.equals(hash(password))) {
            loggedIn = u; System.out.println("Welcome, " + u.name + "!"); customerMenu();
        } else System.out.println("Incorrect username or password.");
    }

    private static void profile() {
        System.out.println("Name: " + loggedIn.name + "\nUsername: " + loggedIn.username +
                "\nPhone: " + loggedIn.phone + "\nLicence: " + loggedIn.license);
    }

    private static void browse() {
        String city = required("Enter city (Hyderabad, Bengaluru, Vijayawada, etc.): ");
        List<Vehicle> list = new ArrayList<>();
        for (Vehicle v : vehicles.values()) if (v.active && v.city.equalsIgnoreCase(city)) list.add(v);
        if (list.isEmpty()) System.out.println("No listed vehicles found in that city."); else printVehicles(list);
    }

    private static void printVehicles(List<Vehicle> list) {
        if (list.isEmpty()) { System.out.println("No vehicles found."); return; }
        System.out.printf("%-13s %-7s %-27s %-15s %-12s %-10s%n", "ID", "TYPE", "MODEL", "CITY", "PRICE/DAY", "STATUS");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (Vehicle v : list) System.out.printf(Locale.US, "%-13s %-7s %-27s %-15s ₹%-11.2f %-10s%n",
                v.id, v.type, v.model, v.city, v.price, v.active ? "Listed" : "Unlisted");
    }

    private static void book() {
        printVehicles(new ArrayList<>(vehicles.values()));
        String id = required("Vehicle ID to book: ").toUpperCase(Locale.ROOT);
        Vehicle v = vehicles.get(id);
        if (v == null || !v.active) { System.out.println("Vehicle not found or not listed."); return; }
        LocalDate start = date("Start date (YYYY-MM-DD): ");
        if (start.isBefore(LocalDate.now())) { System.out.println("Date cannot be in the past."); return; }
        int days = integer("Number of rental days: ");
        if (days < 1 || days > 365) { System.out.println("Enter 1 to 365 days."); return; }
        LocalDate end = start.plusDays(days); // exclusive end date
        if (!available(id, start, end)) { System.out.println("This vehicle is already booked for overlapping dates."); return; }
        double total = v.price * days;
        System.out.println("\nBOOKING SUMMARY\nVehicle: " + v.model + "\nCity: " + v.city +
                "\nDates: " + start + " to " + end.minusDays(1) + "\nDays: " + days);
        System.out.printf(Locale.US, "Total: ₹%.2f%n", total);
        if (!payment(total)) { System.out.println("Booking cancelled."); return; }
        String bookingId = "B" + (System.currentTimeMillis() % 100000000L);
        bookings.put(bookingId, new Booking(bookingId, loggedIn.username, id, start, end, days, total, "ACTIVE", LocalDate.now()));
        saveBookings(); System.out.println("Booking confirmed! Booking ID: " + bookingId);
    }

    private static boolean payment(double amount) {
        System.out.println("\nSIMULATED PAYMENT\n1. Google Pay (demo)\n2. PhonePe (demo)\n3. Pay at rental desk (demo)\n0. Cancel");
        int c = integer("Payment option: ");
        if (c < 1 || c > 3) return false;
        String method = c == 1 ? "Google Pay" : c == 2 ? "PhonePe" : "Rental desk";
        System.out.printf(Locale.US, "%s marked successful for ₹%.2f (SIMULATION ONLY).%n", method, amount);
        System.out.println("No real money was transferred."); return true;
    }

    private static boolean available(String vehicleId, LocalDate start, LocalDate end) {
        for (Booking b : bookings.values()) if (b.vehicleId.equalsIgnoreCase(vehicleId) && b.status.equals("ACTIVE"))
            if (start.isBefore(b.end) && b.start.isBefore(end)) return false;
        return true;
    }

    private static void myBookings() {
        List<Booking> list = new ArrayList<>();
        for (Booking b : bookings.values()) if (b.username.equalsIgnoreCase(loggedIn.username)) list.add(b);
        printBookings(list);
    }

    private static void returnBooking() {
        myBookings(); String id = required("Booking ID to return/close: ").toUpperCase(Locale.ROOT);
        Booking b = bookings.get(id);
        if (b == null || !b.username.equalsIgnoreCase(loggedIn.username)) { System.out.println("Booking not found for your account."); return; }
        if (!b.status.equals("ACTIVE")) { System.out.println("Booking is already closed."); return; }
        b.status = "RETURNED"; saveBookings(); System.out.println("Booking marked as returned.");
    }

    private static void printBookings(List<Booking> list) {
        if (list.isEmpty()) { System.out.println("No bookings found."); return; }
        for (Booking b : list) System.out.printf(Locale.US,
                "ID:%s | User:%s | Vehicle:%s | %s to %s | Days:%d | Total:₹%.2f | Status:%s%n",
                b.id, b.username, b.vehicleId, b.start, b.end.minusDays(1), b.days, b.total, b.status);
    }

    private static void adminLogin() {
        if (required("Admin username: ").equals(ADMIN) && required("Admin password: ").equals(ADMIN_PASSWORD)) adminMenu();
        else System.out.println("Incorrect administrator credentials.");
    }

    private static void addVehicle() {
        String id = required("Vehicle ID: ").toUpperCase(Locale.ROOT);
        if (id.contains("|") || vehicles.containsKey(id)) { System.out.println("Invalid or duplicate ID."); return; }
        String type = required("Type (Car/Bike): "), model = required("Model: "), city = required("City: ");
        double price = decimal("Price per day (₹): ");
        if (price <= 0) { System.out.println("Price must be positive."); return; }
        vehicles.put(id, new Vehicle(id, type, model, city, price, true)); saveVehicles(); System.out.println("Vehicle added.");
    }

    private static void removeVehicle() {
        printVehicles(new ArrayList<>(vehicles.values())); Vehicle v = vehicles.get(required("Vehicle ID to unlist: ").toUpperCase(Locale.ROOT));
        if (v == null) System.out.println("Vehicle not found."); else { v.active = false; saveVehicles(); System.out.println("Vehicle unlisted; old bookings are preserved."); }
    }

    private static void updateVehicle() {
        printVehicles(new ArrayList<>(vehicles.values())); Vehicle v = vehicles.get(required("Vehicle ID to update: ").toUpperCase(Locale.ROOT));
        if (v == null) { System.out.println("Vehicle not found."); return; }
        System.out.println("Press Enter to keep the existing value.");
        String s = ask("Model [" + v.model + "]: "); if (!s.isBlank()) v.model = s;
        s = ask("City [" + v.city + "]: "); if (!s.isBlank()) v.city = s;
        s = ask("Daily price [" + v.price + "]: ");
        if (!s.isBlank()) try { double p = Double.parseDouble(s); if (p > 0) v.price = p; else System.out.println("Price ignored."); }
        catch (NumberFormatException e) { System.out.println("Invalid price ignored."); }
        saveVehicles(); System.out.println("Vehicle updated.");
    }

    private static void report() {
        printBookings(new ArrayList<>(bookings.values())); double total = 0;
        for (Booking b : bookings.values()) total += b.total;
        System.out.printf(Locale.US, "Total value of all recorded bookings: ₹%.2f%n", total);
    }

    private static void loadData() throws IOException {
        if (Files.exists(USERS_FILE)) for (String line : Files.readAllLines(USERS_FILE)) {
            String[] p = line.split("\\|", -1); if (p.length == 5) users.put(p[0], new User(p[0], p[1], p[2], p[3], p[4]));
        }
        if (Files.exists(VEHICLES_FILE)) for (String line : Files.readAllLines(VEHICLES_FILE)) {
            String[] p = line.split("\\|", -1); if (p.length == 6) try {
                vehicles.put(p[0], new Vehicle(p[0], p[1], p[2], p[3], Double.parseDouble(p[4]), Boolean.parseBoolean(p[5])));
            } catch (NumberFormatException ignored) { }
        }
        if (Files.exists(BOOKINGS_FILE)) for (String line : Files.readAllLines(BOOKINGS_FILE)) {
            String[] p = line.split("\\|", -1); if (p.length == 9) try {
                Booking b = new Booking(p[0], p[1], p[2], LocalDate.parse(p[3]), LocalDate.parse(p[4]),
                        Integer.parseInt(p[5]), Double.parseDouble(p[6]), p[7], LocalDate.parse(p[8])); bookings.put(b.id, b);
            } catch (RuntimeException ignored) { }
        }
    }

    private static void seedVehicles() {
        if (!vehicles.isEmpty()) return;
        seed("HYD-CAR-01", "Car", "Maruti Suzuki Swift", "Hyderabad", 1800);
        seed("HYD-CAR-02", "Car", "Hyundai i20", "Hyderabad", 2200);
        seed("HYD-BIKE-01", "Bike", "Honda Activa 6G", "Hyderabad", 500);
        seed("HYD-BIKE-02", "Bike", "Royal Enfield Classic 350", "Hyderabad", 1000);
        seed("BLR-CAR-01", "Car", "Tata Nexon", "Bengaluru", 2500);
        seed("BLR-BIKE-01", "Bike", "TVS Apache RTR 160", "Bengaluru", 650);
        seed("VJA-CAR-01", "Car", "Maruti Suzuki Baleno", "Vijayawada", 1700);
        seed("VJA-BIKE-01", "Bike", "Bajaj Pulsar 150", "Vijayawada", 550);
        saveVehicles();
    }
    private static void seed(String id, String type, String model, String city, double price) { vehicles.put(id, new Vehicle(id, type, model, city, price, true)); }

    private static void saveAll() { saveUsers(); saveVehicles(); saveBookings(); }
    private static void saveUsers() { List<String> lines = new ArrayList<>(); for (User u : users.values()) lines.add(join(u.username,u.name,u.phone,u.license,u.passwordHash)); write(USERS_FILE, lines); }
    private static void saveVehicles() { List<String> lines = new ArrayList<>(); for (Vehicle v : vehicles.values()) lines.add(join(v.id,v.type,v.model,v.city,Double.toString(v.price),Boolean.toString(v.active))); write(VEHICLES_FILE, lines); }
    private static void saveBookings() { List<String> lines = new ArrayList<>(); for (Booking b : bookings.values()) lines.add(join(b.id,b.username,b.vehicleId,b.start.toString(),b.end.toString(),Integer.toString(b.days),Double.toString(b.total),b.status,b.created.toString())); write(BOOKINGS_FILE, lines); }
    private static void write(Path path, List<String> lines) { try { Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); } catch (IOException e) { System.out.println("Could not save " + path + ": " + e.getMessage()); } }
    private static String join(String... values) { StringJoiner j = new StringJoiner("|"); for (String s : values) j.add(s.replace("|", "/").replace("\n", " ")); return j.toString(); }

    private static String hash(String value) {
        try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); }
        catch (Exception e) { throw new IllegalStateException("Could not hash password", e); }
    }
    private static String ask(String prompt) { System.out.print(prompt); return in.nextLine().trim(); }
    private static String required(String prompt) { while (true) { String s = ask(prompt); if (!s.isEmpty()) return s; System.out.println("Please enter a value."); } }
    private static int integer(String prompt) { while (true) try { return Integer.parseInt(ask(prompt)); } catch (NumberFormatException e) { System.out.println("Enter a whole number."); } }
    private static double decimal(String prompt) { while (true) try { return Double.parseDouble(ask(prompt)); } catch (NumberFormatException e) { System.out.println("Enter a valid number."); } }
    private static LocalDate date(String prompt) { while (true) try { return LocalDate.parse(required(prompt)); } catch (DateTimeParseException e) { System.out.println("Use YYYY-MM-DD format."); } }

    private static class User {
        String username, name, phone, license, passwordHash;
        User(String username, String name, String phone, String license, String passwordHash) { this.username=username; this.name=name; this.phone=phone; this.license=license; this.passwordHash=passwordHash; }
    }
    private static class Vehicle {
        String id, type, model, city; double price; boolean active;
        Vehicle(String id, String type, String model, String city, double price, boolean active) { this.id=id; this.type=type; this.model=model; this.city=city; this.price=price; this.active=active; }
    }
    private static class Booking {
        String id, username, vehicleId, status; LocalDate start, end, created; int days; double total;
        Booking(String id, String username, String vehicleId, LocalDate start, LocalDate end, int days, double total, String status, LocalDate created) {
            this.id=id; this.username=username; this.vehicleId=vehicleId; this.start=start; this.end=end; this.days=days; this.total=total; this.status=status; this.created=created;
        }
    }
}
