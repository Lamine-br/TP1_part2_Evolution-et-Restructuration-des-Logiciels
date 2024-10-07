package org.example;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {

        Folder f = new Folder();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            files.add(new Folder());
        }
        for (int i = 0; i < 5; i++) {
            SimpleFile file = new SimpleFile();
            file.setSize(i);
            files.add(file);
        }

        Folder f2 = new Folder();
        ArrayList<File> files2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SimpleFile file = new SimpleFile();
            file.setSize(i);
            files2.add(file);
        }
        f2.setFiles(files2);
        files.add(f2);

        f.setFiles(files);

        Visitor v1 = new CalculerFileSize();
        System.out.println("Size : "+f.accept(v1));

        Visitor v2 = new ClaculerFileNumber();
        System.out.println("Number of files : "+f.accept(v2));
    }
}