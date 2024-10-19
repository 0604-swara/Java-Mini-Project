import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Interface for managing appointments
interface ManageAppointments {
    void bookAppointment(String patientName, String doctorName) throws Exception;
}

// Interface for generating reports
interface GenerateReport {
    void generateReport();
}

// Custom Exceptions
class PatientNotFoundException extends Exception {
    public PatientNotFoundException(String message) {
        super(message);
    }
}

class DoctorNotFoundException extends Exception {
    public DoctorNotFoundException(String message) {
        super(message);
    }
}

// Patient class
class Patient implements Serializable {
    private String name;
    private int age;
    private String ailment;

    public Patient(String name, int age, String ailment) {
        this.name = name;
        this.age = age;
        this.ailment = ailment;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getAilment() {
        return ailment;
    }

    public void displayPatientDetails() {
        System.out.println("Patient Name: " + name);
        System.out.println("Age: " + age);
        System.out.println("Ailment: " + ailment);
    }
}

// Doctor class
class Doctor implements Serializable {
    private String name;
    private String specialization;
    private Map<String, String> schedule; // Maps patient name to appointment time

    public Doctor(String name, String specialization) {
        this.name = name;
        this.specialization = specialization;
        this.schedule = new LinkedHashMap<>(); // Preserves order of insertion
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public Map<String, String> getSchedule() {
        return schedule;
    }

    // Add schedule with patient name and time
    public void addSchedule(String patientName, String time) {
        schedule.put(patientName, time);
    }

    // Display doctor schedule with patient names and appointment times
    public void displaySchedule() {
        System.out.println("Doctor Name: " + name);
        System.out.println("Specialization: " + specialization);
        if (schedule.isEmpty()) {
            System.out.println("Schedule: No appointments.");
        } else {
            System.out.println("Schedule:");
            schedule.forEach((patient, time) ->
                    System.out.println("Patient: " + patient + ", Time: " + time));
        }
    }
}

// HospitalAdmin class (implements both interfaces)
class HospitalAdmin implements ManageAppointments, GenerateReport, Serializable {
    private Map<String, Patient> patients;
    private Map<String, Doctor> doctors;

    public HospitalAdmin() {
        patients = new HashMap<>();
        doctors = new HashMap<>();
    }

    public Map<String, Patient> getPatients() {
        return patients;
    }

    public Map<String, Doctor> getDoctors() {
        return doctors;
    }

    // Register a new patient
    public synchronized void registerPatient(Patient patient) {
        if (patients.containsKey(patient.getName())) {
            System.out.println("Patient with name " + patient.getName() + " already exists.");
            return;
        }
        patients.put(patient.getName(), patient);
        System.out.println("Patient " + patient.getName() + " registered successfully.");
    }

    // Add a new doctor
    public synchronized void addDoctor(Doctor doctor) {
        if (doctors.containsKey(doctor.getName())) {
            System.out.println("Doctor with name " + doctor.getName() + " already exists.");
            return;
        }
        doctors.put(doctor.getName(), doctor);
        System.out.println("Doctor " + doctor.getName() + " added to the system.");
    }

    // Book an appointment
    @Override
    public synchronized void bookAppointment(String patientName, String doctorName) throws Exception {
        if (!patients.containsKey(patientName)) {
            throw new PatientNotFoundException("Patient with name " + patientName + " not found.");
        }
        if (!doctors.containsKey(doctorName)) {
            throw new DoctorNotFoundException("Doctor with name " + doctorName + " not found.");
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter appointment time (e.g., 10:30 AM): ");
        String time = scanner.nextLine().trim();

        doctors.get(doctorName).addSchedule(patientName, time);
        System.out.println("Appointment booked successfully for " + patientName + " with Dr. " + doctorName + " at " + time);
    }

    // Generate a report of all patients and doctors
    @Override
    public void generateReport() {
        System.out.println("\n=== Hospital Report ===");
        System.out.println("\nList of Patients:");
        if (patients.isEmpty()) {
            System.out.println("No patients registered.");
        } else {
            for (Patient patient : patients.values()) {
                patient.displayPatientDetails();
                System.out.println("-----------------------");
            }
        }

        System.out.println("\nList of Doctors and their schedules:");
        if (doctors.isEmpty()) {
            System.out.println("No doctors added.");
        } else {
            for (Doctor doctor : doctors.values()) {
                doctor.displaySchedule();
                System.out.println("-----------------------");
            }
        }
    }

    // File handling class for CSV operations
    static class FileHandler {
        // Save data to CSV file
        public static synchronized void saveData(HospitalAdmin admin, String filename) throws IOException {
            try (FileWriter writer = new FileWriter(filename, false)) { // Overwrite file, no append mode
                // Write patient data
                writer.write("PATIENTS\n");
                for (Patient patient : admin.getPatients().values()) {
                    writer.write(patient.getName() + "," + patient.getAge() + "," + patient.getAilment() + "\n");
                }

                // Write doctor data
                writer.write("DOCTORS\n");
                for (Doctor doctor : admin.getDoctors().values()) {
                    writer.write(doctor.getName() + "," + doctor.getSpecialization() + ",");

                    // Write patient schedules for this doctor
                    List<String> scheduleEntries = new ArrayList<>();
                    for (Map.Entry<String, String> entry : doctor.getSchedule().entrySet()) {
                        scheduleEntries.add(entry.getKey() + ":" + entry.getValue());
                    }
                    writer.write(String.join(";", scheduleEntries));
                    writer.write("\n");
                }
            }
            System.out.println("Data saved successfully to " + filename);
        }

        // Load data from CSV file
        public static synchronized HospitalAdmin loadData(String filename) throws IOException {
            HospitalAdmin admin = new HospitalAdmin();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                String section = "";

                while ((line = reader.readLine()) != null) {
                    if (line.equals("PATIENTS")) {
                        section = "PATIENTS";
                    } else if (line.equals("DOCTORS")) {
                        section = "DOCTORS";
                    } else if (!line.isEmpty()) {
                        String[] data = line.split(",");
                        if (section.equals("PATIENTS")) {
                            // Load patient data
                            Patient patient = new Patient(data[0], Integer.parseInt(data[1]), data[2]);
                            admin.registerPatient(patient);
                        } else if (section.equals("DOCTORS")) {
                            // Load doctor data
                            Doctor doctor = new Doctor(data[0], data[1]);
                            if (data.length > 2) {
                                String[] scheduledPatients = data[2].split(";");
                                for (String schedule : scheduledPatients) {
                                    String[] patientData = schedule.split(":");
                                    doctor.addSchedule(patientData[0], patientData[1]);
                                }
                            }
                            admin.addDoctor(doctor);
                        }
                    }
                }
            }
            System.out.println("Data loaded successfully from " + filename);
            return admin;
        }
    }
}

// Multithreading class to handle multiple patient registrations
class PatientRegistrationThread extends Thread {
    private HospitalAdmin admin;
    private Patient patient;

    public PatientRegistrationThread(HospitalAdmin admin, Patient patient) {
        this.admin = admin;
        this.patient = patient;
    }

    @Override
    public void run() {
        admin.registerPatient(patient);
    }
}

// Main class with user interaction
public class HospitalManagementSystem {
    private static Scanner scanner = new Scanner(System.in);
    private static HospitalAdmin admin = new HospitalAdmin();
    private static final String DATA_FILE = "hospitalData.csv";

    public static void main(String[] args) {
        // Check if data file exists and load data if it does
        File dataFile = new File(DATA_FILE);
        if (dataFile.exists()) {
            loadData();
        } else {
            System.out.println("No existing data found. Starting with an empty database.");
        }

        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = getIntegerInput("Enter your choice: ");
            switch (choice) {
                case 1:
                    addDoctor();
                    break;
                case 2:
                    registerPatient();
                    break;
                case 3:
                    bookAppointment();
                    break;
                case 4:
                    generateReport();
                    break;
                case 5:
                    saveData();
                    break;
                case 6:
                    exit = true;
                    saveData(); // Automatically save data before exiting
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Hospital Management System ===");
        System.out.println("1. Add Doctor");
        System.out.println("2. Register Patient");
        System.out.println("3. Book Appointment");
        System.out.println("4. Generate Report");
        System.out.println("5. Save Data");
        System.out.println("6. Exit");
    }

    // Add a new doctor
    private static void addDoctor() {
        System.out.println("\n=== Add Doctor ===");
        System.out.print("Enter doctor name: ");
        String name = scanner.nextLine();
        System.out.print("Enter specialization: ");
        String specialization = scanner.nextLine();
        Doctor doctor = new Doctor(name, specialization);
        admin.addDoctor(doctor);
    }

    // Register a new patient (uses multithreading)
    private static void registerPatient() {
        System.out.println("\n=== Register Patient ===");
        System.out.print("Enter patient name: ");
        String name = scanner.nextLine();
        System.out.print("Enter age: ");
        int age = getIntegerInput("Enter age: ");
        System.out.print("Enter ailment: ");
        String ailment = scanner.nextLine();

        Patient patient = new Patient(name, age, ailment);
        PatientRegistrationThread registrationThread = new PatientRegistrationThread(admin, patient);
        registrationThread.start();
        try {
            registrationThread.join(); // Ensure the thread completes before moving on
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Book an appointment with exception handling
    private static void bookAppointment() {
        System.out.println("\n=== Book Appointment ===");
        System.out.print("Enter patient name: ");
        String patientName = scanner.nextLine();
        System.out.print("Enter doctor name: ");
        String doctorName = scanner.nextLine();

        try {
            admin.bookAppointment(patientName, doctorName);
        } catch (PatientNotFoundException | DoctorNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generate a report of all patients and doctors
    private static void generateReport() {
        admin.generateReport();
    }

    // Save data to CSV file
    private static void saveData() {
        try {
            HospitalAdmin.FileHandler.saveData(admin, DATA_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load data from CSV file
    private static void loadData() {
        try {
            admin = HospitalAdmin.FileHandler.loadData(DATA_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get integer input with error handling
    private static int getIntegerInput(String prompt) {
        int input = -1;
        boolean valid = false;
        while (!valid) {
            try {
                System.out.print(prompt);
                input = Integer.parseInt(scanner.nextLine());
                valid = true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
        return input;
    }
}
