package org.example;

public class CalculerFileSize implements Visitor{
    public int visit(SimpleFile f){
        return f.getSize();
    };
    public int visit(Folder f){
        int somme = 0 ;
        for (int i = 0; i < f.getFiles().size(); i++) {
            somme += f.getFiles().get(i).accept(this);
        }
        return somme ;
    };
}
