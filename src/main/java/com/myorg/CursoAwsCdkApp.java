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
        VpcStack vpcStack = new VpcStack(app, "Vpc");

//Dentro da stack do meu cluster estou passando a vpc a qual ele pertence
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
//        Para criar um cluster eu preciso primeiro ter criado uma vpc, então informo que o cluster
//        depende da criação do vpc
        clusterStack.addDependency(vpcStack);

        Service01Stack service01Stack = new Service01Stack(app, "Service01", clusterStack.getCluster());
//        Para criar o Service01Stack eu preciso primeiro ter criado um cluster, então eu dependo dele.
        service01Stack.addDependency(clusterStack);

        app.synth();
    }
}
