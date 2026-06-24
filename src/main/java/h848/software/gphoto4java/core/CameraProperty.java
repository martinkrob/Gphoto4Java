/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package h848.software.gphoto4java.core;

import java.util.List;

/**
 *
 * @author krob
 */
public class CameraProperty {
    
    private String name;          // např. "shutterspeed"

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String label;         // např. "Shutter Speed"
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private String type;          // např. "RADIO", "TOGGLE"
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private boolean readOnly;     // true, pokud má foťák hodnotu zamčenou
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private String currentValue;  // aktuální hodnota z foťáku
    
    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    private java.util.List<String> choices = new java.util.ArrayList<>();
    
    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }
    
}
