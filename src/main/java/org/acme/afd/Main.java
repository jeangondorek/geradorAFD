package org.acme.afd;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Inject
    InitializeProgram initializeProgram;

    public static void main(String[] args) {
        Quarkus.run(Main.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        try {
            initializeProgram.initialize();
            Quarkus.waitForExit();
            return 0;
        } catch (Exception e) {
            System.err.println("❌ Erro na inicialização: " + e.getMessage());
            return 1;
        }
    }
}