import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorkshopManager {

    private final List<Vehicle> vehicleList;

    public WorkshopManager() {this.vehicleList = new ArrayList<>();}

    public void addVehicle(Vehicle v) throws ServiceException {
        if (v == null) {
            throw new ServiceException("Cannot add empty vehicle to workshop");
        }

        if (findVehicle(v.getRegistrationNumber()) != null) {
            throw new ServiceException("Vehicle with this registration number already exists in workshop.");
        }

        if (v.getServiceCost() > Vehicle.MAX_SERVICE_COST) {
            throw new ServiceException("Vehicle not maintainable in this workshop");
        }

        vehicleList.add(v);
        System.out.println("Vehicle added successfully. Registration number : " + v.getRegistrationNumber() + "\n");
    }

    public void displayAllVehicleDetails() throws ServiceException{
        System.out.println("\n --------- WORKSHOP INVENTORY ---------");

        if (vehicleList.isEmpty()) {
            throw new ServiceException("No vehicles in the inventory yet.");
        }

        for (Vehicle v : vehicleList) {
            v.showVehicleDetails();
            System.out.println();
        }
    }


    public void serviceAllVehicles() {
        if (vehicleList.isEmpty()) {
            System.out.println("No vehicles to service.");
            return;
        }

        System.out.println("\n --------- SERVICING ALL VEHICLES ---------\n");

        for (Vehicle vehicle : vehicleList) {
            // Since Vehicle implements Serviceable, we can directly call interface methods
            vehicle.serviceVehicle();
            System.out.println("Service Cost: BDT " + vehicle.getServiceCost());
            System.out.println("---");
        }
    }

    // Find Your Vehicle
    public Vehicle findVehicle(String regNum) throws ServiceException {
        if (regNum == null || regNum.trim().isEmpty()) {
            throw new ServiceException("Registration number cannot be null or empty for search.");
        }
        for (Vehicle v : vehicleList) {
            if (Objects.equals(v.getRegistrationNumber(), regNum)) return v;
        }
        return null;
    }

    public void withdrawVehicle(String regNum) throws ServiceException {
        if (vehicleList.isEmpty()) throw new ServiceException("Workshop is empty. No vehicles to withdraw.");

        Vehicle vehicleToRemove = findVehicle(regNum);

        if (vehicleToRemove == null) throw new ServiceException("Cannot withdraw. Vehicle with reg " + regNum + " not found.");

        // Removing the vehicle as repairing done
        vehicleList.remove(vehicleToRemove);
        System.out.println("Vehicle " + regNum + " withdrawn successfully.");
    }

    public double totalServiceRevenue() {
        double totalServiceCost = 0;

        for (Vehicle v : vehicleList) {
            totalServiceCost += (v.getServiceCost());
        }

        return totalServiceCost;
    }

    public int getVehicleCount() {return vehicleList.size();}
}
