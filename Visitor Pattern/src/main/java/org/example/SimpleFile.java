package org.example;

public class SimpleFile extends File{
    public int accept(Visitor v){
        return v.visit(this);
    }
}
