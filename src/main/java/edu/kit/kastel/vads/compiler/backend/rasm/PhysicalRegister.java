package edu.kit.kastel.vads.compiler.backend.rasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public class PhysicalRegister implements Register {

    private String register;
    public PhysicalRegister(String physicalRegister) {
        this.register = physicalRegister;
    }

    @Override
    public String toString(){
        return this.register;
    }
}
