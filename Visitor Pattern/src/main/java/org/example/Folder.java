package org.example;

import java.util.ArrayList;

public class Folder extends File{
    private ArrayList<File> files = new ArrayList<>();

    public int accept(Visitor v){
        return v.visit(this);
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }
}
