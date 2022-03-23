package edu.ufl.cise.plc;

import java.util.HashMap;

import edu.ufl.cise.plc.ast.Declaration;

public class SymbolTable {

//TODO:  Implement a symbol table class that is appropriate for this language.
    HashMap<String, Declaration> entries = new HashMap<>();

    //returns true if name successfully inserted in symbol table, false if already present
    public boolean insert(String name, Declaration dec) {
        return (entries.putIfAbsent(name, dec) == null);
    }

    public Declaration lookup(String name) {
        return entries.get(name);
    }
}
