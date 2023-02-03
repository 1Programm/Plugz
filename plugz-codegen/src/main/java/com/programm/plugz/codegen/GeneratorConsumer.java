package com.programm.plugz.codegen;

import com.programm.plugz.codegen.codegenerator.JavaCodeGenerationException;
import com.programm.plugz.codegen.codegenerator.JavaCodeGenerator;

public interface GeneratorConsumer {

    void accept(JavaCodeGenerator g, String generatedClassName) throws JavaCodeGenerationException;

}
