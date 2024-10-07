package org.example;

public interface Visitor {
    public int visit(SimpleFile f);
    public int visit(Folder f);
}
