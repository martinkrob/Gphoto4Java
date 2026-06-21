/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package h848.software.gphoto4java;

/**
 *
 * @author martin
 */
public class CameraFile {

    private final String folder;
    private final String filename;
    private final int index;

    public CameraFile(String folder, String filename, int index) {
        this.folder = folder;
        this.filename = filename;
        this.index = index;
    }

    public String getFolder() {
        return folder;
    }

    public String getFilename() {
        return filename;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return folder + "/" + filename + " (Index: " + index + ")";
    }
}
