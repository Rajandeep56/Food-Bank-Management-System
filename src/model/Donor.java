package model;

public class Donor extends User {
     public Donor(int userId, String name, String email, String password, String phone) {
        super(userId, name, email, password, phone, "Donor");
     }
    
}
