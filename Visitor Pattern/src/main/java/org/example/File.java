package org.example;

public abstract class File {
    private int size ;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public abstract int accept(Visitor v);

}
