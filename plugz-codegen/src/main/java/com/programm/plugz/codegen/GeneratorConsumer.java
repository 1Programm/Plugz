package com.programm.plugz.codegen;

import com.programm.plugz.codegen.codegenerator.JavaCodeGenerationException;
import com.programm.plugz.codegen.codegenerator.JavaCodeGenerator;

/**
 * Functional interface used to provide a java code generator and some generated class name so the user can build his code.
 */
public interface GeneratorConsumer {

    /**
     * Provides a java code generator and a generated class name.
     * The generated class name should be used to define the main class.
     * @param g the generator.
     * @param generatedClassName the generated class name.
     * @throws JavaCodeGenerationException if any code generating exception is thrown.
     */
    void accept(JavaCodeGenerator g, String generatedClassName) throws JavaCodeGenerationException;

}
