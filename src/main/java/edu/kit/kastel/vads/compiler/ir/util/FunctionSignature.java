package edu.kit.kastel.vads.compiler.ir.util;

import java.util.ArrayList;
import java.util.List;

public class FunctionSignature {
        private final String returnType;
        private final List<String> parameterTypes;
        private final List<String> parameterNames;

        public FunctionSignature(String returnType, List<String> parameterTypes, List<String> parameterNames) {
            this.returnType = returnType;
            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.parameterNames = new ArrayList<>(parameterNames);
        }

        public String returnType() { return returnType; }
        public List<String> parameterTypes() { return parameterTypes; }
        public List<String> parameterNames() { return parameterNames; }
        public int getParameterCount() { return parameterTypes.size(); }
}
