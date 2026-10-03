import java.util.Scanner;

class Vehicle {
int vehicleId;
String model;
String type;
double price;


Vehicle(int vehicleId, String model, String type, double price) {
    this.vehicleId = vehicleId;
    this.model = model;
    this.type = type;
    this.price = price;
}

void displayVehicle() {
    System.out.println("Vehicle ID: " + vehicleId);
    System.out.println("Model: " + model);
    System.out.println("Type: " + type);
    System.out.println("Price per day: Rs." + price);
    System.out.println("----------------------");
}


}

class Customer {
String name;
String phone;


Customer(String name, String phone) {
    this.name = name;
    this.phone = phone;
}

void displayCustomer() {
    System.out.println("Customer Name: " + name);
    System.out.println("Phone: " + phone);
}
