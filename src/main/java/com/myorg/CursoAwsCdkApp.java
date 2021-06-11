package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class CursoAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

//Todas as minhas stacks são definidas aqui

//Primeiro parâmetro é o escopo e nele passamos o aplicativo que estamos criando e onde vamos criar
//nosso recurso. O segundo parâmetro é um id
        new VpcStack(app, "Vpc");

        app.synth();
    }
}
