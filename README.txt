VEHICLE RENTAL SYSTEM - JAVA CONSOLE PROJECT

Requirements: JDK 17 or later recommended.

HOW TO RUN
1. Extract this ZIP file.
2. Open Command Prompt or a terminal in the folder containing VehicleRentalSystem.java.
3. Compile: javac VehicleRentalSystem.java
4. Run:     java VehicleRentalSystem

ADMIN DEMO LOGIN
Username: admin
Password: admin123

FEATURES
- Customer signup and login
- Passwords stored as SHA-256 hashes in a local text file
- Browse cars and bikes by city
- Sample India-specific vehicle models and daily prices
- Booking start date and rental duration validation
- Prevents overlapping active bookings for the same vehicle
- Calculates rental price by day
- Simulated Google Pay, PhonePe, and rental-desk payment options
- Customer booking history and return/close booking
- Admin can list, add, unlist, and update vehicles
- Admin booking report
- Saves records in vehicle_rental_data/users.txt, vehicles.txt, and bookings.txt

IMPORTANT LIMITATIONS
Payments are simulated only; no real money is transferred and the program does not connect to Google Pay or PhonePe. This is a college demonstration, not production rental software. Admin credentials are hard-coded for demonstration. Real deployment would require stronger security controls, a database, proper password hashing, logging, and an actual payment provider.
