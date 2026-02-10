package com.example.mydormitory;
import java.util.List;

// Guide.java
public class user {
    private int id;
    private String phoneNumber;
    private String name;
    private String lastName;
    private String surname;
    private List<String> roles;
    private List<String> documentPath;

    public user(int id, String phoneNumber, String name, String lastName, String surname, List<String> roles, List<String> documentPath) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.lastName = lastName;
        this.surname = surname;
        this.roles = roles;
        this.documentPath = documentPath;
    }

    // Getters
    public int getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getName() { return name; }
    public String getLastName() { return lastName; }
    public String getSurname() { return surname; }
    public List<String> getRoles() { return roles; }
    public List<String> getDocumentPath() { return documentPath; }
}
